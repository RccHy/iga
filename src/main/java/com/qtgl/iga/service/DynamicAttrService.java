package com.qtgl.iga.service;

import com.qtgl.iga.bo.DynamicAttr;

import java.util.List;

public interface DynamicAttrService {
    List<DynamicAttr> findAllByType(String type, String tenantId);

    List<DynamicAttr> findAllByTypeIGA(String type, String tenantId);
}
