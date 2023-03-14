package com.qtgl.iga.service;


import com.qtgl.iga.bean.NodeDto;
import com.qtgl.iga.bo.NodeRulesRange;
import com.qtgl.iga.bo.UpstreamTypeField;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface NodeRulesRangeService {


    List<NodeRulesRange> findNodeRulesRange(Map<String, Object> arguments, String id);

    NodeRulesRange deleteNodeRulesRange(Map<String, Object> arguments, String id);

    NodeRulesRange saveNodeRulesRange(NodeRulesRange nodeRulesRange, String id);

    NodeRulesRange updateNodeRulesRange(NodeRulesRange nodeRulesRange);

    NodeDto saveNodeRuleRange(NodeDto nodeRule);

    NodeRulesRange saveNodeRuleRange(NodeRulesRange nodeRulesRange);

    List<NodeRulesRange> getByRulesId(String id, Integer status);

    Integer deleteNodeRulesRangeByRuleId(String ruleId);

    Integer makeNodeRulesRangesToHistory(String rulesRangeId, Integer status);

    ArrayList<UpstreamTypeField> getByUpstreamTypeId(String upstreamTypeId);
}
