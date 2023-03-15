package com.qtgl.iga.service;

import com.qtgl.iga.bo.DynamicValue;

import java.util.List;

public interface DynamicValueService {
    List<DynamicValue> findAllByAttrId(List<String> attrIds, String tenantId);

    List<DynamicValue> findAllAttrByType(String id, String type);

    List<DynamicValue> findAllByAttrIdIGA(List<String> attrIds, String id);
}
