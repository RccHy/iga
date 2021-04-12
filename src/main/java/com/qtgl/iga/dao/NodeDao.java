package com.qtgl.iga.dao;


import com.qtgl.iga.bean.NodeDto;
import com.qtgl.iga.bo.Node;

import java.util.List;
import java.util.Map;

public interface NodeDao {

    NodeDto save(NodeDto node);

    List<Node> getByCode(String domain, String deptTreeType, String nodeCode,Integer status);

    Integer deleteNode(Map<String, Object> arguments, String id);

    List<Node> findNodes(Map<String, Object> arguments, String domain);

    List<Node> findByTreeTypeId(String id,Integer status);

    List<Node> findNodesPlus(Map<String, Object> arguments, String id);

    List<Node> findNodesByCode(String code, String domain);

    Integer makeNodeToHistory(String domain,Integer status,String id);

    Integer publishNode(String id);

}
