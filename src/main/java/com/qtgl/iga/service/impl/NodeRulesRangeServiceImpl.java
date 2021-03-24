package com.qtgl.iga.service.impl;


import com.qtgl.iga.bo.NodeRulesRange;
import com.qtgl.iga.dao.NodeRulesDao;
import com.qtgl.iga.service.NodeRulesRangeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@Transactional

public class NodeRulesRangeServiceImpl implements NodeRulesRangeService {


    @Autowired
    NodeRulesDao nodeRulesDao;


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
}
