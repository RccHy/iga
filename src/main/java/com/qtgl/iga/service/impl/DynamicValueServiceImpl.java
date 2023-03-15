package com.qtgl.iga.service.impl;

import com.qtgl.iga.bo.DynamicValue;
import com.qtgl.iga.dao.DynamicValueDao;
import com.qtgl.iga.service.DynamicValueService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class DynamicValueServiceImpl implements DynamicValueService {
    @Resource
    DynamicValueDao dynamicValueDao;

    @Override
    public List<DynamicValue> findAllByAttrId(List<String> attrIds, String tenantId) {
        return dynamicValueDao.findAllByAttrId(attrIds, tenantId);
    }

    @Override
    public List<DynamicValue> findAllAttrByType(String tenantId, String type) {
        return dynamicValueDao.findAllAttrByType(tenantId, type);
    }

    @Override
    public List<DynamicValue> findAllByAttrIdIGA(List<String> attrIds, String id) {
        return dynamicValueDao.findAllByAttrIdIGA(attrIds, id);
    }
}
