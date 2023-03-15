package com.qtgl.iga.service.impl;

import com.qtgl.iga.bo.DynamicAttr;
import com.qtgl.iga.dao.DynamicAttrDao;
import com.qtgl.iga.service.DynamicAttrService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class DynamicAttrServiceImpl implements DynamicAttrService {

    @Resource
    DynamicAttrDao dynamicAttrDao;

    @Override
    public List<DynamicAttr> findAllByType(String type, String tenantId) {
        return dynamicAttrDao.findAllByType(type, tenantId);
    }

    @Override
    public List<DynamicAttr> findAllByTypeIGA(String type, String tenantId) {
        return dynamicAttrDao.findAllByTypeIGA(type, tenantId);
    }
}
