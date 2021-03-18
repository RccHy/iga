package com.qtgl.iga.dao;


import com.qtgl.iga.bean.NodeDto;
import com.qtgl.iga.bo.NodeRules;

import java.util.List;

public interface NodeRulesDao {


    List<NodeRules> getByNodeAndType(String nodeId, Integer type, Boolean active);


    NodeDto saveNodeRules(NodeDto nodeRules);
}
