package com.qtgl.iga.dao;

import com.qtgl.iga.bo.DomainInfo;

import java.util.List;

public interface DomainInfoDao {

    void save(DomainInfo domainInfo);
    List<DomainInfo> findAll();
}
