package com.qtgl.iga.dao;


import com.qtgl.iga.bean.NodeDto;
import com.qtgl.iga.bo.NodeRules;
import com.qtgl.iga.vo.NodeRulesVo;

import java.util.List;
import java.util.Map;

public interface NodeRulesDao {


    List<NodeRules> getByNodeAndType(String nodeId, Integer type, Boolean active, Integer status);

    NodeDto saveNodeRules(NodeDto nodeRules);

    List<NodeRules> findNodeRules(Map<String, Object> arguments, String domain);

    NodeRules updateRules(NodeRules nodeRules);

    Integer deleteNodeRules(String id);

    List<NodeRulesVo> findNodeRulesByNodeId(String nodeId, Integer status);

    List<NodeRules> findNodeRulesByService(String serviceName, String domain, String synType);

    NodeRulesVo saveNodeRules(NodeRulesVo nodeRules);

    NodeRules findNodeRulesById(String id, Integer status);

    Integer deleteNodeRulesById(String id);

    List<NodeRules> findNodeRulesByUpStreamTypeId(String id, Integer status);

    Integer makeNodeRulesToHistory(String id, Integer status);

    List<NodeRules> findNodeRulesByServiceKey(String serviceKey, Integer status, Integer type);

    List<NodeRulesVo> findPullNodeRulesByNodeId(String id, Integer status);

    List<NodeRules> findNodeRulesByUpStreamTypeIdsAndType(List<String> ids, String type, String domain, Integer status);
}
