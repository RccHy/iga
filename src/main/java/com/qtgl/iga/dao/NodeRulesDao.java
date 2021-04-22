package com.qtgl.iga.dao;


import com.qtgl.iga.bean.NodeDto;
import com.qtgl.iga.bo.NodeRules;
import com.qtgl.iga.vo.NodeRulesVo;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

public interface NodeRulesDao {


    List<NodeRules> getByNodeAndType(String nodeId, Integer type, Boolean active, Integer status);


    NodeDto saveNodeRules(NodeDto nodeRules);

    List<NodeRules> findNodeRules(Map<String, Object> arguments);

    NodeRules updateRules(NodeRules nodeRules);

    Integer deleteNodeRules(String id);

    List<NodeRulesVo> findNodeRulesByNodeId(String id, Integer status);

    NodeRulesVo saveNodeRules(NodeRulesVo nodeRules);

    NodeRules findNodeRulesById(String id, Integer status);

    Integer deleteNodeRulesById(String id);

    List<NodeRules> findNodeRulesByUpStreamTypeId(String id, Integer status) throws InvocationTargetException, IllegalAccessException;

    Integer makeNodeRulesToHistory(String id, Integer status);
}
