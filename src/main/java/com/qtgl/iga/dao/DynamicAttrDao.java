package com.qtgl.iga.dao;


import com.qtgl.iga.bo.DynamicAttr;

import java.util.List;

public interface DynamicAttrDao {

     List<DynamicAttr> findAllByType(String type, String domain);

    List<DynamicAttr> findAllByTypeIGA(String type, String id);
}
