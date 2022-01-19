package com.qtgl.iga.dao;


import com.qtgl.iga.bo.DynamicValue;

import java.util.List;

public interface DynamicValueDao {

     List<DynamicValue> findAllByAttrId(List<String> attrIds, String tenantId);
}
