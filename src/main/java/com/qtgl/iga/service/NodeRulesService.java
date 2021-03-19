package com.qtgl.iga.service;


import com.qtgl.iga.bo.NodeRules;

import java.util.List;
import java.util.Map;

public interface NodeRulesService {


    List<NodeRules> findNodeRules(Map<String, Object> arguments, String id);

    NodeRules deleteRules(Map<String, Object> arguments, String id);

    NodeRules saveRules(NodeRules nodeRules);

    NodeRules updateRules(NodeRules nodeRules);
}
