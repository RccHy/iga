package com.qtgl.iga.dao;

import com.qtgl.iga.bo.DomainInfo;

import java.util.List;

public interface DomainInfoDao {

    void save(DomainInfo domainInfo);

    List<DomainInfo> findAll();

    DomainInfo getByDomainName(String name);

    DomainInfo findById(String id);

}
