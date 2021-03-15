package com.qtgl.iga.service;


import com.qtgl.iga.bo.Node;

import java.util.List;

public interface NodeService {

    Node save(Node node);
    Node getRoot(String domain);
    List<Node> getByCode(String domain, String nodeCode);

}
