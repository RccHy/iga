package com.qtgl.iga.service.impl;

import com.qtgl.iga.bo.DeptRelationType;
import com.qtgl.iga.dao.DeptRelationTypeDao;
import com.qtgl.iga.service.DeptRelationTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DeptRelationTypeServiceImpl implements DeptRelationTypeService {


    @Autowired
    DeptRelationTypeDao deptRelationTypeDao;

    @Override
    public List<DeptRelationType> findAll(String domain) {
        return deptRelationTypeDao.findAll(domain);
    }

    @Override
    public void initialization(String domainInfo) {
        final List<DeptRelationType> all = deptRelationTypeDao.findAll(domainInfo);
        if (null == all || all.size() <= 0) {
            deptRelationTypeDao.initialization(domainInfo);
        }
    }
}
