package com.qtgl.iga.service;


import com.qtgl.iga.bo.NodeRules;
import com.qtgl.iga.vo.NodeRulesVo;

import java.util.List;
import java.util.Map;

public interface NodeRulesService {


    List<NodeRules> findNodeRules(Map<String, Object> arguments, String id);

    NodeRules deleteRules(Map<String, Object> arguments, String id) throws Exception;

    NodeRulesVo saveRules(NodeRulesVo nodeRules) throws Exception;

    NodeRulesVo updateRules(NodeRulesVo nodeRules) throws Exception;
}
