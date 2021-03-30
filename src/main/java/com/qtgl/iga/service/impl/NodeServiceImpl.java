package com.qtgl.iga.service.impl;

import com.qtgl.iga.bean.NodeDto;
import com.qtgl.iga.bo.Node;
import com.qtgl.iga.bo.NodeRulesRange;
import com.qtgl.iga.dao.NodeDao;
import com.qtgl.iga.dao.NodeRulesDao;
import com.qtgl.iga.dao.NodeRulesRangeDao;
import com.qtgl.iga.service.NodeService;
import com.qtgl.iga.vo.NodeRulesVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.*;

@Service
@Transactional
public class NodeServiceImpl implements NodeService {


    @Autowired
    NodeDao nodeDao;
    @Autowired
    NodeRulesDao nodeRulesDao;
    @Autowired
    NodeRulesRangeDao nodeRulesRangeDao;


    @Override
    public NodeDto saveNode(NodeDto node, String domain) throws Exception {
        //删除原有数据
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("id", node.getId());
        NodeDto nodeDto = deleteNode(hashMap, domain);
        if (null != nodeDto) {
            node.setCreateTime(nodeDto.getCreateTime());
            node.setUpdateTime(System.currentTimeMillis());
        } else {
            node.setId(null);
        }

        //添加节点规则
        node.setDomain(domain);
        NodeDto save = nodeDao.save(node);
        if (null == save) {
            throw new Exception("添加节点失败");
        }
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

    @Override
    public Node getRoot(String domain, String deptTreeType) {
        return nodeDao.getByCode(domain, deptTreeType, "").get(0);
    }

    @Override
    public List<Node> getByCode(String domain, String deptTreeType, String nodeCode) {
        return nodeDao.getByCode(domain, deptTreeType, nodeCode);
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

        List<NodeRulesVo> rules = nodeRulesDao.findNodeRulesByNodeId((String) arguments.get("id"));

        if (null != rules) {


            //删除range
            for (NodeRulesVo rule : rules) {
                List<NodeRulesRange> byRulesId = nodeRulesRangeDao.getByRulesId(rule.getId());
                flag = nodeRulesRangeDao.deleteNodeRulesRangeByRuleId(rule.getId());
                if (flag > 0) {
                    rule.setNodeRulesRanges(byRulesId);
                } else {
                    throw new Exception("删除节点规则作用域失败");
                }
            }
        }
        if (flag > 0) {
            //删除rule
            i = nodeRulesDao.deleteNodeRules((String) arguments.get("id"));
            nodeDto.setNodeRules(rules);
        } else {
            throw new Exception("删除节点规则失败");
        }
        //如果节点规则明细为空,直接删除node并返回
        if (i > 0 || null == rules) {
            //删除node
            Integer integer = nodeDao.deleteNode(arguments, id);
            if (integer > 0) {

                return nodeDto;
            }
        }


        return null;
    }

    @Override
    public List<NodeDto> findNodes(Map<String, Object> arguments, String id) {
        ArrayList<NodeDto> nodeDos = new ArrayList<>();
        //获取node
        List<Node> nodeList = nodeDao.findNodes(arguments, id);
        //根据node查询对应规则
        for (Node node : nodeList) {
            NodeDto nodeDto = new NodeDto(node);
            List<NodeRulesVo> nodeRulesByNodeId = nodeRulesDao.findNodeRulesByNodeId(node.getId());

            if (null != nodeRulesByNodeId) {
                //根据rules查询对应的range
                for (NodeRulesVo nodeRulesVo : nodeRulesByNodeId) {
                    List<NodeRulesRange> byRulesId = nodeRulesRangeDao.getByRulesId(nodeRulesVo.getId());
                    nodeRulesVo.setNodeRulesRanges(byRulesId);
                }
                nodeDto.setNodeRules(nodeRulesByNodeId);
                nodeDos.add(nodeDto);
            }
        }
        return nodeDos;
    }

    @Override
    public NodeDto updateNode(NodeDto nodeDto) {
        return null;
    }


}
