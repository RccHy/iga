package com.qtgl.iga.service.impl;

import com.qtgl.iga.bo.DomainIgnore;
import com.qtgl.iga.dao.DomainIgnoreDao;
import com.qtgl.iga.service.DomainIgnoreService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.relational.core.sql.In;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
@Slf4j
public class DomainIgnoreServiceImpl implements DomainIgnoreService {
    @Resource
    DomainIgnoreDao domainIgnoreDao;
    @Override
    public List<DomainIgnore> findByDomain(String domainId) {
        return domainIgnoreDao.findByDomain(domainId);
    }

    @Override
    public DomainIgnore save(DomainIgnore domainIgnore) {
        return domainIgnoreDao.save(domainIgnore);
    }

    @Override
    public Integer deleteByUpstreamId(String upstreamId) {
        return domainIgnoreDao.deleteByUpstreamId(upstreamId);
    }
}
