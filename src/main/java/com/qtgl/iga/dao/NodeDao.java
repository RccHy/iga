package com.qtgl.iga.dao;


import com.qtgl.iga.bo.Node;

import java.util.List;

public interface NodeDao {

    Node save(Node node);

    List<Node> getByCode(String domain, String nodeCode);

}
