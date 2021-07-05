package com.qtgl.iga.service.impl;

import com.qtgl.iga.bo.DeptType;
import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.dao.DeptTreeTypeDao;
import com.qtgl.iga.dao.DeptTypeDao;
import com.qtgl.iga.dao.DomainInfoDao;
import com.qtgl.iga.dao.PostTypeDao;
import com.qtgl.iga.service.DomainInfoService;
import com.qtgl.iga.utils.enumerate.ResultCode;
import com.qtgl.iga.utils.exception.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;
import java.util.UUID;


@Service
@Transactional
public class DomainInfoServiceImpl implements DomainInfoService {


    @Autowired
    DomainInfoDao dao;
    @Autowired
    DeptTreeTypeDao deptTreeTypeDao;
    @Autowired
    DeptTypeDao deptTypeDao;
    @Autowired
    PostTypeDao postTypeDao;

    @Override
    public List<DomainInfo> findAll() {
        return dao.findAll();
    }

    @Override
    @Transactional
    public void install(DomainInfo domainInfo) throws Exception {

        /* 初始化数据 */

        // 单位类型
        DeptType deptType = new DeptType();
        deptType.setId(UUID.randomUUID().toString());
        deptType.setName("二级部门");
        deptType.setCode("01");
        deptType.setCreateTime(new Timestamp(new java.util.Date().getTime()));
        deptType.setUpdateTime(new Timestamp(new java.util.Date().getTime()));
        deptType.setCreateUser("iga");
        deptType.setDomain(domainInfo.getId());
        try {
            deptTypeDao.saveDeptTypes(deptType, domainInfo.getId());
        } catch (CustomException e) {
            e.printStackTrace();
            throw new CustomException(ResultCode.FAILED, "请勿重复初始化");
        }
        // 组织机构类型
        deptTreeTypeDao.initialization(domainInfo.getId());
        // 岗位类型
        postTypeDao.initialization(domainInfo.getId());
        // 插入租户信息
        dao.save(domainInfo);

    }

    @Override
    public DomainInfo getByDomainName(String name) {
        return dao.getByDomainName(name);
    }
}
