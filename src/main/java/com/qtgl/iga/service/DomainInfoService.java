package com.qtgl.iga.service;


import com.qtgl.iga.bo.DomainInfo;

import java.util.List;

public interface DomainInfoService {

    List<DomainInfo> findAll();

    void install(DomainInfo domainInfo) throws Exception;

    DomainInfo getByDomainName(String name);
    DomainInfo getLocalhost();

}
