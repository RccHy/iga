package com.qtgl.iga.service.impl;


import com.qtgl.iga.bo.NodeRules;
import com.qtgl.iga.dao.NodeRulesDao;
import com.qtgl.iga.service.NodeRulesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@Transactional

public class NodeRulesServiceImpl implements NodeRulesService {


    @Autowired
    NodeRulesDao nodeRulesDao;


    @Override
    public List<NodeRules> findNodeRules(Map<String, Object> arguments, String id) {
        return nodeRulesDao.findNodeRules(arguments);
    }

    @Override
    public NodeRules deleteRules(Map<String, Object> arguments, String id) {
        return null;
    }

    @Override
    public NodeRules saveRules(NodeRules nodeRules) {
        return null;
    }

    @Override
    public NodeRules updateRules(NodeRules nodeRules) {
        return nodeRulesDao.updateRules(nodeRules);
    }
}
