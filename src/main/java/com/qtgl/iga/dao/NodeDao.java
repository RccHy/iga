package com.qtgl.iga.dao;


import com.qtgl.iga.bean.NodeDto;
import com.qtgl.iga.bo.Node;
import com.qtgl.iga.bo.NodeRulesRange;
import com.qtgl.iga.vo.NodeRulesVo;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface NodeDao {

    NodeDto save(NodeDto node);

    List<Node> getByCode(String domain, String deptTreeType, String nodeCode, Integer status, String type);

    Integer deleteNode(Map<String, Object> arguments, String domainId);

    List<Node> findNodes(Map<String, Object> arguments, String domain);

    List<Node> findByTreeTypeCode(String code, Integer status, String domain);

    List<Node> findNodesPlus(Map<String, Object> arguments, String id);

    List<Node> findNodesByCode(String code, String domain, String type);

    Integer makeNodeToHistory(String domain, Integer status, String id);

    List<Node> findNodesByStatusAndType(Integer status, String type, String domain, Timestamp version);

    List<Node> findById(String id);

    List<Node> findByStatus(Integer status, String domain, String type);

    //    Integer publishNode(String id);
    Integer deleteNodeById(String id, String domain);


    Integer updateNodeAndRules(ArrayList<Node> invalidNodes, ArrayList<NodeRulesVo> invalidNodeRules, ArrayList<NodeRulesRange> invalidNodeRulesRanges);

    List<Node> getByTreeType(String domainId, String deptTreeTypeCode, Integer status, String type);

    List<Node> findNodes(String domainId, Integer status, String type);

}
