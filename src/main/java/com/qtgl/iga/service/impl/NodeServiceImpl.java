package com.qtgl.iga.service.impl;

import com.qtgl.iga.bean.NodeDto;
import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.bo.Node;
import com.qtgl.iga.bo.NodeRulesRange;
import com.qtgl.iga.dao.NodeDao;
import com.qtgl.iga.dao.NodeRulesDao;
import com.qtgl.iga.dao.NodeRulesRangeDao;
import com.qtgl.iga.service.NodeService;
import com.qtgl.iga.vo.NodeRulesVo;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.*;

@Service
@Transactional
public class NodeServiceImpl implements NodeService {

    public static Logger logger = LoggerFactory.getLogger(NodeServiceImpl.class);

    @Autowired
    NodeDao nodeDao;
    @Autowired
    NodeRulesDao nodeRulesDao;
    @Autowired
    NodeRulesRangeDao nodeRulesRangeDao;


    @Override
    public NodeDto saveNode(NodeDto node, String domain) throws Exception {
        //删除原有数据
        if (null != node.getId()) {
            HashMap<String, Object> hashMap = new HashMap<>();
            hashMap.put("id", node.getId());
            hashMap.put("type", node.getType());
            deleteNode(hashMap, domain);
        }
        //保留版本号

        if (null == node.getCreateTime()) {
            List<Node> nodesByStatusAndType = nodeDao.findNodesByStatusAndType(node.getStatus(), node.getType(), domain, null);
            if (null != nodesByStatusAndType) {
                node.setCreateTime(nodesByStatusAndType.get(0).getCreateTime());
            }
        }

        //添加节点规则
        node.setDomain(domain);
        NodeDto save = nodeDao.save(node);
        if (null == save) {
            throw new Exception("添加节点失败");
        }
        if (null != node.getNodeRules() && node.getNodeRules().size() > 0) {

            //添加节点规则明细
            NodeDto nodeRule = nodeRulesDao.saveNodeRules(save);
            if (null == nodeRule) {
                throw new Exception("添加节点规则明细失败");
            }
            //添加节点规则明细作用域
            NodeDto range = nodeRulesRangeDao.saveNodeRuleRange(nodeRule);
            if (null == range) {
                throw new Exception("添加节点规则明细作用域失败");
            }
            return range;
        }

        return save;

    }

    @Override
    public Node getRoot(String domain, String deptTreeType) {
        return nodeDao.getByCode(domain, deptTreeType, "", 0, "dept").get(0);
    }

    @Override
    public List<Node> getByCode(String domain, String deptTreeType, String nodeCode) {
        return nodeDao.getByCode(domain, deptTreeType, nodeCode, 0, "dept");
    }

    @Override
    public NodeDto deleteNode(Map<String, Object> arguments, String id) throws Exception {
        //根据id查询规则是否为禁用状态
        Integer i = 0;
        Integer flag = 0;

        List<Node> nodes = nodeDao.findNodes(arguments, id);
        if (null == nodes || nodes.size() <= 0) {
            return null;
        }

        NodeDto nodeDto = new NodeDto(nodes.get(0));

        List<NodeRulesVo> rules = nodeRulesDao.findNodeRulesByNodeId((String) arguments.get("id"), (Integer) arguments.get("status"));

        if (null != rules) {


            //删除range
            for (NodeRulesVo rule : rules) {
                List<NodeRulesRange> byRulesId = nodeRulesRangeDao.getByRulesId(rule.getId(), (Integer) arguments.get("status"));
                flag = nodeRulesRangeDao.deleteNodeRulesRangeByRuleId(rule.getId());
                if (flag >= 0) {
                    rule.setNodeRulesRanges(byRulesId);
                } else {
                    throw new Exception("删除节点规则作用域失败");
                }
            }
        }
        if (flag >= 0 && null != rules) {
            //删除rule
            i = nodeRulesDao.deleteNodeRules((String) arguments.get("id"));
            nodeDto.setNodeRules(rules);
        }
        if (i < 0 && null != rules) {
            throw new Exception("删除节点规则失败");
        }
        //如果节点规则明细为空,直接删除node并返回
        if (i >= 0 || null == rules) {
            //删除node
            Integer integer = nodeDao.deleteNode(arguments, id);
            if (integer >= 0) {

                return nodeDto;
            }
        }


        return null;
    }

    @Override
    public List<NodeDto> findNodes(Map<String, Object> arguments, String id) {
        //标识是否含有继承
        Boolean flag = false;
        ArrayList<NodeDto> nodeDos = new ArrayList<>();
        //获取node
        List<Node> nodeList = nodeDao.findNodes(arguments, id);
        //根据node查询对应规则
        for (Node node : nodeList) {
            NodeDto nodeDto = new NodeDto(node);
            List<NodeRulesVo> nodeRulesByNodeId = nodeRulesDao.findNodeRulesByNodeId(node.getId(), null);

            if (null != nodeRulesByNodeId) {
                //根据rules查询对应的range
                for (NodeRulesVo nodeRulesVo : nodeRulesByNodeId) {
                    if (null == nodeRulesVo.getInheritId()) {
                        List<NodeRulesRange> byRulesId = nodeRulesRangeDao.getByRulesId(nodeRulesVo.getId(), null);
                        nodeRulesVo.setNodeRulesRanges(byRulesId);
                    } else {
                        flag = true;
                    }

                }
                nodeDto.setInherit(flag);
                nodeDto.setNodeRules(nodeRulesByNodeId);

            }
            nodeDos.add(nodeDto);
        }
        if (null != nodeDos && nodeDos.size() > 0) {
            return nodeDos;
        }
        return null;

    }

    @Override
    public NodeDto updateNode(NodeDto nodeDto) {
        return null;
    }

    @Override
    public List<NodeDto> findNodesPlus(Map<String, Object> arguments, String id) {
        //查询node
        ArrayList<NodeDto> nodeDos = new ArrayList<>();
        //获取node
        List<Node> nodeList = nodeDao.findNodesPlus(arguments, id);
        //根据node查询rules
        if (null != nodeList && nodeList.size() > 0) {
            for (Node node : nodeList) {

                NodeDto nodeDto = new NodeDto(node);
                List<NodeRulesVo> nodeRulesByNodeId = nodeRulesDao.findNodeRulesByNodeId(node.getId(), null);

                if (null != nodeRulesByNodeId) {
                    //根据rules查询对应的range
                    ArrayList<NodeRulesVo> nodeRulesVos = new ArrayList<>();
                    for (NodeRulesVo nodeRulesVo : nodeRulesByNodeId) {
                        if (StringUtils.isBlank(nodeRulesVo.getInheritId())) {
                            List<NodeRulesRange> byRulesId = nodeRulesRangeDao.getByRulesId(nodeRulesVo.getId(), null);
                            nodeRulesVo.setNodeRulesRanges(byRulesId);
                            nodeRulesVos.add(nodeRulesVo);
                        } else {
                            nodeRulesVos.add(null);
                        }

                    }
                    nodeDto.setNodeRules(nodeRulesVos);

                }
                nodeDos.add(nodeDto);
            }
        }


        return nodeDos;
    }

    @Override
    public List<Node> findNodesByCode(String code, String domain, String type) {
        return nodeDao.findNodesByCode(code, domain, type);
    }

    @Override
    public Node applyNode(Map<String, Object> arguments, String domain) throws Exception {
        Object version = arguments.get("version");
        String type = (String) arguments.get("type");
        Boolean mark = (Boolean) arguments.get("mark");
        if (null == version) {
            throw new Exception("版本非法,请确认");
        }
        //mark为true 则为回滚  false为应用
        if (mark) {
            //查询编辑中的node

            // 加 type查询
            List<Node> nodes = nodeDao.findNodesByStatusAndType(1, type, domain, null);
            for (Node node : nodes) {
                HashMap<String, Object> map = new HashMap<>();
                map.put("id", node.getId());
                map.put("status", 1);
                map.put("type", type);
                deleteNode(map, domain);
            }
        }
        //将所有版本改为历史版本

        Integer nodeHistory = getInteger(null, type, domain, null);

        //将传入版本改为正式版本
        Integer rangeNew = getInteger(null, type, domain, version);
        if ((nodeHistory >= 0) && (rangeNew >= 0)) {
            return new Node();
        }
        return null;
    }

    private Integer getInteger(Integer status, String type, String domain, Object version) {

        List<Node> nodes = nodeDao.findNodesByStatusAndType(status, type, domain, version);
        if (null != nodes && nodes.size() > 0) {
            for (Node node : nodes) {
                List<NodeRulesVo> rules = nodeRulesDao.findNodeRulesByNodeId(node.getId(), null);
                if (null != rules && rules.size() > 0) {
                    for (NodeRulesVo rule : rules) {
                        List<NodeRulesRange> ranges = nodeRulesRangeDao.getByRulesId(rule.getId(), null);
                        if (null != ranges && ranges.size() > 0) {
                            for (NodeRulesRange range : ranges) {
                                Integer rangeHistory = nodeRulesRangeDao.makeNodeRulesRangesToHistory(range.getId(), null == version ? 2 : 0);
                                if (rangeHistory < 0) {
                                    logger.error("应用失败 range {}", range);
                                    throw new RuntimeException("应用失败");
                                }
                            }
                        }
                        Integer ruleHistory = nodeRulesDao.makeNodeRulesToHistory(rule.getId(), null == version ? 2 : 0);
                        if (ruleHistory < 0) {
                            logger.error("应用失败rule  {}", rule);
                            throw new RuntimeException("应用失败");
                        }
                    }
                }
                Integer nodeHistory = nodeDao.makeNodeToHistory(domain, null == version ? 2 : 0, node.getId());
                if (nodeHistory < 0) {
                    logger.error("应用失败  {}", nodes);
                    throw new RuntimeException("应用失败");
                }
            }
        }

        return 1;
    }


    @Override
    public Node rollbackNode(Map<String, Object> arguments, String domain) throws Exception {
        Object version = arguments.get("version");
        if (null == version) {
            throw new Exception("版本非法,请确认");
        }
        //  删除编辑中的node
        //查询编辑中的node
        List<Node> nodes = nodeDao.findNodes(arguments, domain);
        for (Node node : nodes) {
            HashMap<String, Object> map = new HashMap<>();
            map.put("id", node.getId());
            map.put("status", 1);
            NodeDto nodeDto = deleteNode(map, domain);
            if (null == nodeDto) {
                throw new RuntimeException("回滚失败");
            }
        }
        return new Node();
    }

    /**
     * @param arguments
     * @param domain
     * @param type
     * @Description: 判断是否有编辑中node
     * @return: java.lang.Integer
     */
    @Override
    public Integer judgeEdit(Map<String, Object> arguments, DomainInfo domain, String type) throws Exception {
        List<NodeDto> nodeList = null;
        Integer status = (Integer) arguments.get("status");
        if (null == status) {
            throw new Exception("状态不能为空");
        }
        if (1 == status) {
            //查看是否有治理中的规则
            HashMap<String, Object> map = new HashMap<>();
            map.put("status", 1);
            map.put("type", type);
            nodeList = findNodes(map, domain.getId());
        }
        //   如果状态为编辑,并且没有治理中的数据
        if (null == nodeList && status == 1) {
            //复制,再返回
            HashMap<String, Object> map = new HashMap<>();
            map.put("status", 0);
            map.put("type", type);
            List<NodeDto> nodes = findNodes(map, domain.getId());
            //复制数据
            if (null != nodes && nodes.size() > 0) {
                for (NodeDto node : nodes) {
                    node.setId(null);
                    node.setStatus(1);
                    node.setCreateTime(System.currentTimeMillis());
                    if (null != node.getNodeRules()) {
                        for (NodeRulesVo nodeRule : node.getNodeRules()) {
                            nodeRule.setId(null);
                            nodeRule.setStatus(1);
                            if (null != nodeRule.getNodeRulesRanges()) {
                                for (NodeRulesRange nodeRulesRange : nodeRule.getNodeRulesRanges()) {
                                    nodeRulesRange.setId(null);
                                    nodeRulesRange.setStatus(1);
                                }
                            }
                        }
                    }

                    saveNode(node, domain.getId());
                }
            }

            return null;

        }
        return status;
    }


}
