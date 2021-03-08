package com.qtgl.iga.service.impl;

import com.qtgl.iga.bo.DeptType;
import com.qtgl.iga.dao.DeptTypeDao;
import com.qtgl.iga.service.DeptTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class DeptTypeServiceImpl implements DeptTypeService {

    @Autowired
    DeptTypeDao deptTypeDao;

    @Override
    public List<DeptType> getAllDeptTypes() {
        return deptTypeDao.getAllDeptTypes();
    }
}
