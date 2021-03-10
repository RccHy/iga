package com.qtgl.iga.service.impl;

import com.qtgl.iga.bo.DeptTreeType;
import com.qtgl.iga.dao.DeptTreeTypeDao;
import com.qtgl.iga.service.DeptTreeTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@Transactional
public class DeptTreeTypeServiceImpl implements DeptTreeTypeService {

    @Autowired
    DeptTreeTypeDao deptTreeTypeDao;

    @Override
    public List<DeptTreeType> findAll(Map<String, Object> arguments, String domain) {
        return deptTreeTypeDao.findAll(arguments, domain);
    }

    @Override
    public DeptTreeType deleteDeptTreeType(Map<String, Object> arguments, String domain) throws Exception {
        return deptTreeTypeDao.deleteDeptTreeType(arguments, domain);
    }

    @Override
    public DeptTreeType saveDeptTreeType(DeptTreeType deptTreeType, String domain) throws Exception {
        return deptTreeTypeDao.saveDeptTreeType(deptTreeType, domain);
    }

    @Override
    public DeptTreeType updateDeptTreeType(DeptTreeType deptTreeType) throws Exception {
        return deptTreeTypeDao.updateDeptTreeType(deptTreeType);
    }

}
