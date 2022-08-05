package com.qtgl.iga.service.impl;

import com.qtgl.iga.bean.NodeDto;
import com.qtgl.iga.bean.TreeBean;
import com.qtgl.iga.bo.*;
import com.qtgl.iga.dao.NodeDao;
import com.qtgl.iga.dao.NodeRulesDao;
import com.qtgl.iga.dao.NodeRulesRangeDao;
import com.qtgl.iga.dao.TaskLogDao;
import com.qtgl.iga.service.NodeService;
import com.qtgl.iga.utils.enumerate.ResultCode;
import com.qtgl.iga.utils.exception.CustomException;
import com.qtgl.iga.vo.NodeRulesVo;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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
    @Autowired
    TaskLogDao taskLogDao;


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
            throw new CustomException(ResultCode.FAILED, "添加节点失败");
        }
        if (null != node.getNodeRules() && node.getNodeRules().size() > 0) {

            //添加节点规则明细
            NodeDto nodeRule = nodeRulesDao.saveNodeRules(save);
            if (null == nodeRule) {
                throw new CustomException(ResultCode.FAILED, "添加节点规则明细失败");
            }
            //添加节点规则明细作用域
            NodeDto range = nodeRulesRangeDao.saveNodeRuleRange(nodeRule);
            if (null == range) {
                throw new CustomException(ResultCode.FAILED, "添加节点规则明细作用域失败");
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
    public NodeDto deleteNode(Map<String, Object> arguments, String id) {
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
                    throw new CustomException(ResultCode.FAILED, "删除节点规则作用域失败");
                }
            }
        }
        if (flag >= 0 && null != rules) {
            //删除rule
            i = nodeRulesDao.deleteNodeRules((String) arguments.get("id"));
            nodeDto.setNodeRules(rules);
        }
        if (i < 0 && null != rules) {
            throw new CustomException(ResultCode.FAILED, "删除节点规则失败");
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


    /**
     * @param arguments
     * @param domain
     * @Description: 根据传入版本回滚或应用对应版本(不传入回滚以当前生产版本作为回滚版本 ( 无生产版本, 则提示无法回滚))
     * 回滚:先获取传入的版本(不传入的话,则取当前生产版本),然后将所有版本置为历史版本,最后根据版本号将对应版本应用为生产版本
     * 应用:获取传入的版本(不传入的话,则取编辑中版本),然后将所有版本置为历史版本,最后根据版本号将对应版本应用为生产版本
     * @return: com.qtgl.iga.bo.Node
     */
    @Override
    public Node applyNode(Map<String, Object> arguments, String domain) throws Exception {
        //传入的版本数据类型转换为数据库对应类型
        Timestamp version = null;
        if (null != arguments.get("version")) {
            version = new Timestamp(((Date) arguments.get("version")).getTime());
        }
        String type = (String) arguments.get("type");
        Boolean mark = (Boolean) arguments.get("mark");
//        if (null == version) {
//            throw new Exception("版本非法,请确认");
//        }
        //mark为true 则为回滚  false为应用
        if (mark) {
            //查询有无生产版本
            List<Node> proNodes = nodeDao.findNodesByStatusAndType(0, type, domain, null);
            if (null == proNodes) {
                return null;
            }

        }
        //查询是否在同步中
        List<TaskLog> logList = taskLogDao.findByStatus(domain);
        if (null != logList && logList.size() > 0) {
            if ("doing".equals(logList.get(0).getStatus())) {
                throw new CustomException(ResultCode.FAILED, "数据正在同步,应用失败,请稍后再试");

            }

        }
        if (null == version) {
            if (mark) {
                //回滚查询生产版本
                List<Node> nodes = nodeDao.findNodesByStatusAndType(0, type, domain, null);
                if (null != nodes && nodes.size() > 0) {
                    version = new Timestamp(nodes.get(0).getCreateTime());
                }
            } else {
                //应用查询编辑中版本

                List<Node> nodes = nodeDao.findNodesByStatusAndType(1, type, domain, null);
                if (null != nodes && nodes.size() > 0) {
                    version = new Timestamp(nodes.get(0).getCreateTime());
                }

            }
        }
        if (mark) {
            //查询编辑中的node
            // 加 type查询
            List<Node> nodes = nodeDao.findNodesByStatusAndType(1, type, domain, null);
            if (null != nodes && nodes.size() > 0) {
                for (Node node : nodes) {
                    HashMap<String, Object> map = new HashMap<>();
                    map.put("id", node.getId());
                    map.put("status", 1);
                    map.put("type", type);
                    deleteNode(map, domain);
                }
            }
        }
        //将所有版本改为历史版本
        Integer nodeHistory = getInteger(null, type, domain, null);
        //将传入版本改为正式版本
        if (null != version) {
            Integer rangeNew = getInteger(null, type, domain, version);
            if ((nodeHistory >= 0) && (rangeNew >= 0)) {
                return new Node();
            }
        }
        return null;
    }

    private Integer getInteger(Integer status, String type, String domain, Timestamp version) {

        List<Node> nodes = nodeDao.findNodesByStatusAndType(status, type, domain, version);
        if (null != nodes && nodes.size() > 0) {
            for (Node node : nodes) {
                if(node.getStatus().equals(3)){
                    continue;
                }
                List<NodeRulesVo> rules = nodeRulesDao.findNodeRulesByNodeId(node.getId(), null);
                if (null != rules && rules.size() > 0) {
                    for (NodeRulesVo rule : rules) {
                        List<NodeRulesRange> ranges = nodeRulesRangeDao.getByRulesId(rule.getId(), null);
                        if (null != ranges && ranges.size() > 0) {
                            for (NodeRulesRange range : ranges) {
                                Integer rangeHistory = nodeRulesRangeDao.makeNodeRulesRangesToHistory(range.getId(), null == version ? 2 : 0);
                                if (rangeHistory < 0) {
                                    logger.error("应用失败 range {}", range);
                                    throw new CustomException(ResultCode.FAILED, "应用range失败");
                                }
                            }
                        }
                        Integer ruleHistory = nodeRulesDao.makeNodeRulesToHistory(rule.getId(), null == version ? 2 : 0);
                        if (ruleHistory < 0) {
                            logger.error("应用失败rule  {}", rule);
                            throw new CustomException(ResultCode.FAILED, "应用rule失败");
                        }
                    }
                }
                Integer nodeHistory = nodeDao.makeNodeToHistory(domain, null == version ? 2 : 0, node.getId());
                if (nodeHistory < 0) {
                    logger.error("应用失败  {}", nodes);
                    throw new CustomException(ResultCode.FAILED, "应用node失败");
                }
            }
        }

        return 1;
    }

    /**
     * 弃用方法
     *
     * @param arguments
     * @param domain
     * @return
     * @throws Exception
     */
    @Override
    public Node rollbackNode(Map<String, Object> arguments, String domain) throws Exception {
        Object version = arguments.get("version");
        if (null == version) {
            throw new CustomException(ResultCode.FAILED, "版本非法,请确认");
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
                throw new CustomException(ResultCode.FAILED, "回滚失败");
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
            throw new CustomException(ResultCode.FAILED, "状态不能为空");
        }
        if (1 == status) {
            //判断同步任务状态
//            ExecutorService executorService = TaskThreadPool.executorServiceMap.get(domain.getDomainName());
//            executorService.shutdown();
//            boolean terminated = executorService.isTerminated();
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
                ConcurrentHashMap<String, String> inheritMap = new ConcurrentHashMap<>();
                for (NodeDto node : nodes) {
                    node.setId(null);
                    node.setStatus(1);
                    node.setCreateTime(System.currentTimeMillis());
                    if (null != node.getNodeRules()) {
                        for (NodeRulesVo nodeRule : node.getNodeRules()) {
                            String nodeRulesNewId = UUID.randomUUID().toString().replace("-", "");
                            inheritMap.put(nodeRule.getId(),nodeRulesNewId);
                            nodeRule.setId(nodeRulesNewId);
                            nodeRule.setStatus(1);
                            if (null != nodeRule.getNodeRulesRanges()) {
                                for (NodeRulesRange nodeRulesRange : nodeRule.getNodeRulesRanges()) {
                                    nodeRulesRange.setId(null);
                                    nodeRulesRange.setStatus(1);
                                }
                            }
                        }
                        //继承规则inherit_id赋值
                        for (NodeRulesVo nodeRule : node.getNodeRules()) {
                            if(null!=nodeRule.getInheritId()){
                                String newInheritId = inheritMap.get(nodeRule.getInheritId());
                                nodeRule.setInheritId(newInheritId);
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

    @Override
    public List<Node> nodeStatus(Map<String, Object> arguments, String domain) {
        Integer status = (Integer) arguments.get("status");
        String type = (String) arguments.get("type");
        if (null == status) {
            throw new CustomException(ResultCode.FAILED, "状态不能为空");
        }
        if (null == type) {
            throw new CustomException(ResultCode.FAILED, "类型不能为空");
        }
        return nodeDao.findByStatus(status, domain, type);
    }

    @Override
    public void updateNodeAndRules(List<Node> nodes, List<TreeBean> beans) {
        //运算完成的结果集中不包含node所指定的挂载节点,则该规则需置为无效
        ArrayList<Node> invalidNodes = new ArrayList<>();
        ArrayList<NodeRulesVo> invalidNodeRules = new ArrayList<>();
        ArrayList<NodeRulesRange> invalidNodeRulesRanges = new ArrayList<>();
        if(!CollectionUtils.isEmpty(beans)&&!CollectionUtils.isEmpty(nodes)){
            Map<String, TreeBean> collect = beans.stream().collect(Collectors.toMap(TreeBean::getCode, treeBean -> treeBean));
            for (Node node : nodes) {
                if(!StringUtils.isBlank(node.getNodeCode())){
                    if(!collect.containsKey(node.getNodeCode())){
                        invalidNodes.add(node);
                        List<NodeRulesVo> nodeRulesVos = nodeRulesDao.findPullNodeRulesByNodeId(node.getId(), null);
                        if(!CollectionUtils.isEmpty(nodeRulesVos)){
                            invalidNodeRules.addAll(nodeRulesVos);
                            for (NodeRulesVo nodeRulesVo : nodeRulesVos) {
                                List<NodeRulesRange> nodeRulesRanges = nodeRulesRangeDao.getByRulesId(nodeRulesVo.getId(), null);
                                if(!CollectionUtils.isEmpty(nodeRulesRanges)){
                                    invalidNodeRulesRanges.addAll(nodeRulesRanges);
                                }
                            }
                        }
                    }
                }
            }
            nodeDao.updateNodeAndRules(invalidNodes,invalidNodeRules,invalidNodeRulesRanges);
        }
    }

}
