package com.qtgl.iga.dao;


import com.qtgl.iga.bean.NodeDto;
import com.qtgl.iga.bo.NodeRules;
import com.qtgl.iga.vo.NodeRulesVo;

import java.util.List;
import java.util.Map;

public interface NodeRulesDao {


    List<NodeRules> getByNodeAndType(String nodeId, Integer type, Boolean active);


    NodeDto saveNodeRules(NodeDto nodeRules);

    List<NodeRules> findNodeRules(Map<String, Object> arguments);

    NodeRules updateRules(NodeRules nodeRules);

    Integer deleteNodeRules(String id);

    List<NodeRulesVo> findNodeRulesByNodeId(String id);

    NodeRulesVo saveNodeRules(NodeRulesVo nodeRules);

    NodeRules findNodeRulesById(String id);

    Integer deleteNodeRulesById(String id);
}
