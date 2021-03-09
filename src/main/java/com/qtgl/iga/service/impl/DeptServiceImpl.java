package com.qtgl.iga.service.impl;

import com.qtgl.iga.bo.Dept;
import com.qtgl.iga.dao.DeptDao;
import com.qtgl.iga.service.DeptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class DeptServiceImpl implements DeptService {

    @Autowired
    DeptDao deptDao;

    @Override
    public List<Dept> getAllDepts() {
        return deptDao.getAllDepts();
    }
}
