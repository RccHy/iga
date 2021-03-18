package com.qtgl.iga.dao;


import com.qtgl.iga.bean.NodeDto;
import com.qtgl.iga.bo.Node;

import java.util.List;

public interface NodeDao {

    NodeDto save(NodeDto node);

    List<Node> getByCode(String domain, String nodeCode);

}
