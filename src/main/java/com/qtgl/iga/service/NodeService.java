package com.qtgl.iga.service;


import com.qtgl.iga.bo.Node;

public interface NodeService {

    Node save(Node node);
    Node getRoot(String domain);
    Node getByCode(String domain,String nodeCode);

}
