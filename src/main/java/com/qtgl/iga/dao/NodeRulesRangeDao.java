package com.qtgl.iga.dao;


import com.qtgl.iga.bo.NodeRulesRange;

import java.util.List;

public interface NodeRulesRangeDao {


    List<NodeRulesRange> getByRulesId(String rulesId);


}
