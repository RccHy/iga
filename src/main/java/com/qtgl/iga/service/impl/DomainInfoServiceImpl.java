package com.qtgl.iga.service.impl;

import com.qtgl.iga.bo.DeptTreeType;
import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.dao.*;
import com.qtgl.iga.service.DomainInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
    @Autowired
    MonitorRulesDao monitorRulesDao;
    @Autowired
    DeptRelationTypeDao deptRelationTypeDao;
    @Autowired
    CardTypeDao cardTypeDao;
    @Value("${app.client}")
    private String client;
    @Value("${app.secret}")
    private String secret;


    @Override
    public List<DomainInfo> findAll() {
        return dao.findAll();
    }

    @Override
    @Transactional
    public void install(DomainInfo domainInfo) {

        /* 初始化数据 */

        // 单位类型
        /*DeptType deptType = new DeptType();
        deptType.setId(UUID.randomUUID().toString());
        deptType.setName("二级部门");
        deptType.setCode("02");
        deptType.setCreateTime(new Timestamp(new java.util.Date().getTime()));
        deptType.setUpdateTime(new Timestamp(new java.util.Date().getTime()));
        deptType.setCreateUser("iga");
        deptType.setDomain(domainInfo.getId());
        try {
            deptTypeDao.saveDeptTypes(deptType, domainInfo.getId());
        } catch (CustomException e) {
            e.printStackTrace();
            throw new CustomException(ResultCode.FAILED, "请勿重复初始化");
        }*/

        // 插入租户信息
        Integer save = dao.save(domainInfo);
        if (save > 0) {
            // 组织机构类型
            deptTreeTypeDao.initialization(domainInfo.getId());
            // 组织机构关系类型
            deptRelationTypeDao.initialization(domainInfo.getId());
            // 岗位类型
            postTypeDao.initialization(domainInfo.getId());
            //  监控规则
            monitorRulesDao.initialization(domainInfo.getId());
            // 人员证件类型
            cardTypeDao.initialization(domainInfo.getId());
        }
    }

    @Override
    public DomainInfo getLocalhost() {
        // 判断是否有超级租户信息
        DomainInfo superDomain = dao.getByDomainName("localhost");

        if (superDomain == null) {
            superDomain = new DomainInfo("localhost", null, new Timestamp(System.currentTimeMillis()), 0, client, secret);
            dao.save(superDomain);
        }
        return superDomain;
    }


    @Override
    public DomainInfo getByDomainName(String name) {
        return dao.getByDomainName(name);
    }
}
