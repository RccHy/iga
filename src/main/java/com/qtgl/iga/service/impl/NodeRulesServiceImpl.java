package com.qtgl.iga.service.impl;


import com.qtgl.iga.bo.Node;
import com.qtgl.iga.bo.NodeRules;
import com.qtgl.iga.bo.NodeRulesRange;
import com.qtgl.iga.dao.NodeRulesDao;
import com.qtgl.iga.dao.NodeRulesRangeDao;
import com.qtgl.iga.service.NodeRulesService;
import com.qtgl.iga.service.NodeService;
import com.qtgl.iga.vo.NodeRulesVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional

public class NodeRulesServiceImpl implements NodeRulesService {


    @Autowired
    NodeRulesDao nodeRulesDao;
    @Autowired
    NodeRulesRangeDao nodeRulesRangeDao;
    @Autowired
    NodeService nodeService;


    @Override
    public List<NodeRules> findNodeRules(Map<String, Object> arguments, String id) {
        return nodeRulesDao.findNodeRules(arguments);
    }

    @Override
    public NodeRulesVo deleteRules(Map<String, Object> arguments, String domain) throws Exception {
        // todo 查询删除规则拉取的数据子节点是否有规则
        List<String> codes = (List<String>) arguments.get("codes");
        if(null != codes &&codes.size()>0){
            //根据code查询是否有对应的规则
            for (String code : codes) {
               List<Node> nodes =  nodeService.findNodesByCode(code,domain);
               if(null!=nodes &&nodes.size()>0){
                   for (Node node : nodes) {
                       HashMap<String, Object> hashMap = new HashMap<>();
                       hashMap.put("id",node.getId());
                       nodeService.deleteNode(hashMap,domain);
                   }
               }
            }
        }
        //查询是否有range需要删除
        List<NodeRulesRange> ranges = nodeRulesRangeDao.getByRulesId((String) arguments.get("id"),null);
        //删除range
        if (null != ranges && ranges.size() > 0) {
            Integer i = nodeRulesRangeDao.deleteNodeRulesRangeByRuleId((String) arguments.get("id"));
            if (!(i > 0)) {
                throw new Exception("删除range失败");
            }
        }
        //todo 查询rule  删除编辑中
        NodeRules nodeRules = nodeRulesDao.findNodeRulesById((String) arguments.get("id"), 1);
        //删除rules
        Integer flag = nodeRulesDao.deleteNodeRulesById((String) arguments.get("id"));
        if (flag > 0) {
            NodeRulesVo nodeRulesVo = new NodeRulesVo(nodeRules);
            nodeRulesVo.setNodeRulesRanges(ranges);
            return nodeRulesVo;
        } else {
            throw new Exception("删除rule失败");
        }

    }

    @Override
    public NodeRulesVo saveRules(NodeRulesVo nodeRules) throws Exception {
//        ArrayList<NodeRulesRange> nodeRulesRanges = new ArrayList<>();
        //添加rules
        NodeRulesVo nodeRulesVo = nodeRulesDao.saveNodeRules(nodeRules);
//        //添加range
//        if (null == nodeRulesVo) {
//            throw new Exception("添加节点规则失败");
//        }
//        if (null != nodeRulesVo.getNodeRulesRanges()) {
//            for (NodeRulesRange nodeRulesRange : nodeRulesVo.getNodeRulesRanges()) {
//                NodeRulesRange range = nodeRulesRangeDao.saveNodeRuleRange(nodeRulesRange);
//                if (null == range) {
//                    throw new Exception("添加节点规则作用域失败");
//                }
//                nodeRulesRanges.add(range);
//
//            }
//        }
//        nodeRulesVo.setNodeRulesRanges(nodeRulesRanges);
        return nodeRulesVo;
    }

    @Override
    public NodeRulesVo updateRules(NodeRulesVo nodeRules) throws Exception {
        //清除所有range
        Integer integer = nodeRulesRangeDao.deleteNodeRulesRangeByRuleId(nodeRules.getId());
        if (!(integer > 0)) {
            throw new Exception("删除range失败");
        }
        List<NodeRulesRange> ranges = null;
        if (null != nodeRules.getNodeRulesRanges()) {
            //修改range
            for (NodeRulesRange rulesRange : nodeRules.getNodeRulesRanges()) {
                NodeRulesRange nodeRulesRange;
                nodeRulesRange = nodeRulesRangeDao.saveNodeRuleRange(rulesRange);
//            else {
//                nodeRulesRange = nodeRulesRangeDao.updateRulesRange(rulesRange);
//            }
                if (null == nodeRulesRange) {
                    throw new Exception("操作节点作用域失败");
                }
                ranges.add(nodeRulesRange);

            }
        }

        //修改rules
        NodeRules rules = nodeRulesDao.updateRules(nodeRules);
        NodeRulesVo nodeRulesVo = new NodeRulesVo(rules);
        nodeRulesVo.setNodeRulesRanges(ranges);
        return nodeRulesVo;
    }

    @Override
    public List<NodeRulesVo> deleteBatchRules(Map<String, Object> arguments, String id) throws Exception {

        List<String> ruleIds = (List<String>) arguments.get("ids");
        ArrayList<NodeRulesVo> nodeRulesVos = new ArrayList<>();
        for (String ruleId : ruleIds) {
            HashMap<String, Object> map = new HashMap<>();
            map.put("id", ruleId);
            NodeRulesVo nodeRulesVo = deleteRules(map, id);
            nodeRulesVos.add(nodeRulesVo);
        }

        return nodeRulesVos;
    }
}
