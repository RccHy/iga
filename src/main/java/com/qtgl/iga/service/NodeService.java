package com.qtgl.iga.service;


import com.qtgl.iga.bean.NodeDto;
import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.bo.Node;

import java.util.List;
import java.util.Map;

public interface NodeService {

    NodeDto saveNode(NodeDto node, String domain) throws Exception;

    Node getRoot(String domain, String deptTreeType);

    List<Node> getByCode(String domain, String deptTreeType, String nodeCode);

    NodeDto deleteNode(Map<String, Object> arguments, String id) throws Exception;

    List<NodeDto> findNodes(Map<String, Object> arguments, String id);

    NodeDto updateNode(NodeDto nodeDto);

    List<NodeDto> findNodesPlus(Map<String, Object> arguments, String id);

    List<Node> findNodesByCode(String code, String domain,String type);

    Node applyNode(Map<String, Object> arguments,String id) throws Exception;

    Node rollbackNode(Map<String, Object> arguments,String id) throws Exception;

    Integer judgeEdit(Map<String, Object> arguments, DomainInfo domain, String type) throws Exception;
}
