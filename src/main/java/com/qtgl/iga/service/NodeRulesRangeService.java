package com.qtgl.iga.service;


import com.qtgl.iga.bo.NodeRulesRange;

import java.util.List;
import java.util.Map;

public interface NodeRulesRangeService {


    List<NodeRulesRange> findNodeRulesRange(Map<String, Object> arguments, String id);

    NodeRulesRange deleteNodeRulesRange(Map<String, Object> arguments, String id);

    NodeRulesRange saveNodeRulesRange(NodeRulesRange nodeRulesRange, String id);

    NodeRulesRange updateNodeRulesRange(NodeRulesRange nodeRulesRange);
}
