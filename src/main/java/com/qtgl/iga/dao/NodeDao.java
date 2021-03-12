package com.qtgl.iga.dao;


import com.qtgl.iga.bo.Node;

public interface NodeDao {

    Node save(Node node);
    Node getByCode(String domain,String nodeCode);

}
