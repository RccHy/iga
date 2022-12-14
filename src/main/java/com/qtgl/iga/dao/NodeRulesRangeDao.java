package com.qtgl.iga.dao;


import com.qtgl.iga.bean.NodeDto;
import com.qtgl.iga.bo.NodeRulesRange;
import com.qtgl.iga.bo.UpstreamTypeField;

import java.util.ArrayList;
import java.util.List;

public interface NodeRulesRangeDao {


    List<NodeRulesRange> getByRulesId(String rulesId, Integer status);

    NodeDto saveNodeRuleRange(NodeDto nodeRulesRanges);

    Integer deleteNodeRulesRangeByRuleId(String id);

    NodeRulesRange updateRulesRange(NodeRulesRange rulesRange);

    NodeRulesRange saveNodeRuleRange(NodeRulesRange rulesRange);

    Integer makeNodeRulesRangesToHistory(String id, Integer status);

    ArrayList<UpstreamTypeField> getByUpstreamTypeId(String id);
}
