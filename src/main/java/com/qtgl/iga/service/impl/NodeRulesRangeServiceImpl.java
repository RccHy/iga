package com.qtgl.iga.service.impl;


import com.qtgl.iga.bean.NodeDto;
import com.qtgl.iga.bo.NodeRulesRange;
import com.qtgl.iga.bo.UpstreamTypeField;
import com.qtgl.iga.dao.NodeRulesRangeDao;
import com.qtgl.iga.service.NodeRulesRangeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class NodeRulesRangeServiceImpl implements NodeRulesRangeService {

    @Resource
    NodeRulesRangeDao nodeRulesRangeDao;


    @Override
    public List<NodeRulesRange> findNodeRulesRange(Map<String, Object> arguments, String id) {
        return null;
    }

    @Override
    public NodeRulesRange deleteNodeRulesRange(Map<String, Object> arguments, String id) {
        return null;
    }

    @Override
    public NodeRulesRange saveNodeRulesRange(NodeRulesRange nodeRules, String id) {
        return null;
    }

    @Override
    public NodeRulesRange updateNodeRulesRange(NodeRulesRange nodeRules) {
        return null;
    }

    @Override
    public NodeDto saveNodeRuleRange(NodeDto nodeRule) {
        return nodeRulesRangeDao.saveNodeRuleRange(nodeRule);
    }

    @Override
    public NodeRulesRange saveNodeRuleRange(NodeRulesRange nodeRulesRange) {
        return nodeRulesRangeDao.saveNodeRuleRange(nodeRulesRange);
    }

    @Override
    public List<NodeRulesRange> getByRulesId(String id, Integer status) {
        return nodeRulesRangeDao.getByRulesId(id,status);
    }

    @Override
    public Integer deleteNodeRulesRangeByRuleId(String ruleId) {
        return nodeRulesRangeDao.deleteNodeRulesRangeByRuleId(ruleId);
    }

    @Override
    public Integer makeNodeRulesRangesToHistory(String rulesRangeId, Integer status) {
        return nodeRulesRangeDao.makeNodeRulesRangesToHistory(rulesRangeId,status);
    }

    @Override
    public ArrayList<UpstreamTypeField> getByUpstreamTypeId(String upstreamTypeId) {
        return nodeRulesRangeDao.getByUpstreamTypeId(upstreamTypeId);
    }
}
