package com.qtgl.iga.service.impl;

import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.dao.DomainInfoDao;
import com.qtgl.iga.service.DomainInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@Transactional
public class DomainInfoServiceImpl implements DomainInfoService {


    @Autowired
    DomainInfoDao dao;


    @Override
    public List<DomainInfo> findAll() {
        return dao.findAll();
    }

    @Override
    public void install(DomainInfo domainInfo) {
        // 插入租户信息
        dao.save(domainInfo);
        /** 初始化数据**/



    }

    @Override
    public DomainInfo getByDomainName(String name) {
        return dao.getByDomainName(name);
    }
}
