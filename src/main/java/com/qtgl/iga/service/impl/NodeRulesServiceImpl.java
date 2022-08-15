package com.qtgl.iga.service.impl;


import com.qtgl.iga.bo.Node;
import com.qtgl.iga.bo.NodeRules;
import com.qtgl.iga.bo.NodeRulesRange;
import com.qtgl.iga.dao.NodeDao;
import com.qtgl.iga.dao.NodeRulesDao;
import com.qtgl.iga.dao.NodeRulesRangeDao;
import com.qtgl.iga.service.NodeRulesService;
import com.qtgl.iga.service.NodeService;
import com.qtgl.iga.utils.enumerate.ResultCode;
import com.qtgl.iga.utils.exception.CustomException;
import com.qtgl.iga.vo.NodeRulesVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class NodeRulesServiceImpl implements NodeRulesService {


    @Resource
    NodeRulesDao nodeRulesDao;
    @Resource
    NodeRulesRangeDao nodeRulesRangeDao;
    @Resource
    NodeService nodeService;
    @Resource
    NodeDao nodeDao;


    @Override
    public List<NodeRules> findNodeRules(Map<String, Object> arguments, String id) {
        return nodeRulesDao.findNodeRules(arguments);
    }

    @Override
    public NodeRulesVo deleteRules(Map<String, Object> arguments, String domain) throws Exception {
        //   查询删除规则拉取的数据子节点是否有规则
        List<String> codes = (List<String>) arguments.get("codes");
        String type = (String) arguments.get("type");
        if (null != codes && codes.size() > 0) {
            //根据code查询是否有对应的规则
            for (String code : codes) {
                List<Node> nodes = nodeService.findNodesByCode(code, domain, type);
                if (null != nodes && nodes.size() > 0) {
                    for (Node node : nodes) {
                        HashMap<String, Object> hashMap = new HashMap<>();
                        hashMap.put("id", node.getId());
                        nodeService.deleteNode(hashMap, domain);
                    }
                }
            }
        }
        //查询是否有range需要删除
        List<NodeRulesRange> ranges = nodeRulesRangeDao.getByRulesId((String) arguments.get("id"), null);
        //删除range
        if (null != ranges && ranges.size() > 0) {
            Integer i = nodeRulesRangeDao.deleteNodeRulesRangeByRuleId((String) arguments.get("id"));
            if (!(i > 0)) {
                throw new CustomException(ResultCode.FAILED, "删除range失败");
            }
        }
        //  查询rule  删除编辑中 或正式(person或occupy)
        NodeRules nodeRules = nodeRulesDao.findNodeRulesById((String) arguments.get("id"), null);
        //删除rules
        Integer flag = nodeRulesDao.deleteNodeRulesById((String) arguments.get("id"));
        //查询node
        if (null != nodeRules) {
            List<NodeRulesVo> nodeRulesByNodeId = nodeRulesDao.findNodeRulesByNodeId(nodeRules.getNodeId(),null);
            //node下没有对应的nodeRules则将node删除
            if (CollectionUtils.isEmpty(nodeRulesByNodeId)) {
                nodeDao.deleteNodeById(nodeRules.getNodeId(), domain);
            }
        }
        if (flag > 0) {
            return new NodeRulesVo();
        } else {
            throw new CustomException(ResultCode.FAILED, "删除rule失败");
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
            throw new CustomException(ResultCode.FAILED, "删除range失败");
        }
        List<NodeRulesRange> ranges = null;
        if (null != nodeRules.getNodeRulesRanges()) {
            //修改range
            for (NodeRulesRange rulesRange : nodeRules.getNodeRulesRanges()) {
                NodeRulesRange nodeRulesRange;
                nodeRulesRange = nodeRulesRangeDao.saveNodeRuleRange(rulesRange);

                if (null == nodeRulesRange) {
                    throw new CustomException(ResultCode.FAILED, "操作节点作用域失败");
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
            map.put("type", arguments.get("type"));
            NodeRulesVo nodeRulesVo = deleteRules(map, id);
            nodeRulesVos.add(nodeRulesVo);
        }

        return nodeRulesVos;
    }

    @Override
    public List<NodeRules> deleteBatchRules(List<NodeRules> nodeRules, String domain) throws Exception {
        for (NodeRules nodeRule : nodeRules) {
            deleteRules(nodeRule, domain);
        }
        return nodeRules;
    }

    public NodeRules deleteRules(NodeRules rules, String domain) {
        //查询是否有range需要删除
        List<NodeRulesRange> ranges = nodeRulesRangeDao.getByRulesId(rules.getId(), null);
        //删除range
        if (null != ranges && ranges.size() > 0) {
            Integer i = nodeRulesRangeDao.deleteNodeRulesRangeByRuleId(rules.getId());
            if (!(i > 0)) {
                throw new CustomException(ResultCode.FAILED, "删除range失败");
            }
        }
        //删除rules
        Integer flag = nodeRulesDao.deleteNodeRulesById(rules.getId());
        //查询node
        List<NodeRulesVo> nodeRulesByNodeId = nodeRulesDao.findNodeRulesByNodeId(rules.getNodeId(), null);
        //node下没有对应的nodeRules则将node删除
        if (CollectionUtils.isEmpty(nodeRulesByNodeId)) {
            nodeDao.deleteNodeById(rules.getNodeId(), domain);
        }
        if (flag > 0) {
            return new NodeRulesVo();
        } else {
            throw new CustomException(ResultCode.FAILED, "删除rule失败");
        }
    }
}
