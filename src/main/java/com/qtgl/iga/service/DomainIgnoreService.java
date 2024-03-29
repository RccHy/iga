package com.qtgl.iga.service;

import com.qtgl.iga.bo.DomainIgnore;

import java.util.List;

public interface DomainIgnoreService {
    List<DomainIgnore> findByDomain(String domainId);

    DomainIgnore save(DomainIgnore domainIgnore);

    DomainIgnore recoverUpstreamOrRule(DomainIgnore domainIgnore, String domain);

    Integer deleteByUpstreamId(String upstreamId);
}
