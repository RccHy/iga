package com.qtgl.iga.service;


import com.qtgl.iga.bean.NodeDto;
import com.qtgl.iga.bo.Node;

import java.util.List;
import java.util.Map;

public interface NodeService {

    NodeDto saveNode(NodeDto node,String domain) throws Exception;

    Node getRoot(String domain);

    List<Node> getByCode(String domain, String nodeCode);

    NodeDto deleteNode(Map<String, Object> arguments, String id) throws Exception;

    Node findNodes(Map<String, Object> arguments, String id);

    NodeDto updateNode(NodeDto nodeDto);
}
