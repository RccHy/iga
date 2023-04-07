package com.qtgl.iga.service;


import com.qtgl.iga.bean.NodeDto;
import com.qtgl.iga.bo.NodeRules;
import com.qtgl.iga.vo.NodeRulesVo;

import java.util.List;
import java.util.Map;

public interface NodeRulesService {


    List<NodeRules> findNodeRules(Map<String, Object> arguments, String domain);

    NodeRules deleteRules(Map<String, Object> arguments, String id) throws Exception;

    NodeRulesVo saveRules(NodeRulesVo nodeRules) throws Exception;

    NodeRulesVo updateRules(NodeRulesVo nodeRules) throws Exception;

    List<NodeRulesVo> deleteBatchRules(Map<String, Object> arguments, String id) throws Exception;

    List<NodeRules> deleteBatchRules(List<NodeRules> nodeRules, String domain) throws Exception;

    List<NodeRules> findNodeRulesByService(String serviceName, String domain, String synType);

    List<NodeRules> getByNodeAndType(String nodeId, int type, Boolean active, int status);

    List<NodeRulesVo> findNodeRulesByUpStreamIdAndType(List<String> ids, String type, String domain, Integer status);

    NodeDto saveNodeRules(NodeDto save);

    List<NodeRulesVo> findNodeRulesByNodeId(String nodeId, Integer status);

    Integer deleteNodeRules(String id);

    Integer makeNodeRulesToHistory(String ruleId, Integer status);

    List<NodeRules> findNodeRulesByServiceKey(String serviceKey, Integer status, Integer synWay);

    List<NodeRules> findNodeRulesByUpStreamTypeId(String upstreamTypeId, Integer status);

    List<NodeRules> findNodeRulesByDomain(String superDomainId, Integer status, String type);

    List<NodeRulesVo> findSuperNodeRulesByNodeId(String nodeId, Integer status, String domainId);

    List<NodeRulesVo> findNodeRulesByNodeIds(List<String> nodeIds);

    NodeRules findNodeRulesById(String ruleId, Integer status);
}
