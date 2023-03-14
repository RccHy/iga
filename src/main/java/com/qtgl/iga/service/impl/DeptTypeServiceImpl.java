package com.qtgl.iga.service.impl;

import com.qtgl.iga.bo.DeptType;
import com.qtgl.iga.dao.DeptTypeDao;
import com.qtgl.iga.service.DeptTypeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class DeptTypeServiceImpl implements DeptTypeService {

    @Resource
    DeptTypeDao deptTypeDao;

    /**
     * @param arguments
     * @param domain
     * @Description: 获取所有
     * @return: java.util.List<com.qtgl.iga.bo.DeptType>
     */
    @Override
    public List<DeptType> getAllDeptTypes(Map<String, Object> arguments, String domain) {
        return deptTypeDao.getAllDeptTypes(arguments, domain);
    }

    @Override
    public DeptType deleteDeptTypes(Map<String, Object> arguments, String domain) throws Exception {
        return deptTypeDao.deleteDeptTypes(arguments, domain);
    }

    @Override
    public DeptType saveDeptTypes(DeptType deptType, String domain) throws Exception {
        return deptTypeDao.saveDeptTypes(deptType, domain);
    }

    @Override
    public DeptType updateDeptTypes(DeptType deptType) throws Exception {
        return deptTypeDao.updateDeptTypes(deptType);
    }

    @Override
    public DeptType findById(String deptTypeId) {
        return deptTypeDao.findById(deptTypeId);
    }
}
