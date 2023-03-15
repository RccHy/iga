package com.qtgl.iga.service.impl;

import com.qtgl.iga.bo.Tenant;
import com.qtgl.iga.dao.TenantDao;
import com.qtgl.iga.service.TenantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;


@Service
@Transactional
@Slf4j
public class TenantServiceImpl implements TenantService {

    @Resource
    TenantDao tenantDao;

    @Override
    public Tenant findByDomainName(String domainName) {
        return tenantDao.findByDomainName(domainName);
    }
}
