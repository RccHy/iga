package com.qtgl.iga.service.impl;

import com.qtgl.iga.bo.DeptType;
import com.qtgl.iga.dao.DeptTypeDao;
import com.qtgl.iga.service.DeptTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@Transactional
public class DeptTypeServiceImpl implements DeptTypeService {

    @Autowired
    DeptTypeDao deptTypeDao;
    /**
     * @Description: 获取所有
     * @param arguments
     * @param domain
     * @return: java.util.List<com.qtgl.iga.bo.DeptType>
     */
    @Override
    public List<DeptType> getAllDeptTypes(Map<String, Object> arguments, String domain) {
        return deptTypeDao.getAllDeptTypes(arguments, domain);
    }

    @Override
    public DeptType deleteSchemaField(Map<String, Object> arguments, String domain) throws Exception {
        return deptTypeDao.deleteSchemaField(arguments, domain);
    }

    @Override
    public DeptType saveSchemaField(DeptType deptType, String domain) throws Exception {
        return deptTypeDao.saveSchemaField(deptType, domain);
    }

    @Override
    public DeptType updateSchemaField(DeptType deptType) throws Exception {
        return deptTypeDao.updateSchemaField(deptType);
    }
}
