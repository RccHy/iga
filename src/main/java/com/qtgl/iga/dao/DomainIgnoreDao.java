package com.qtgl.iga.dao;

import com.qtgl.iga.bo.DomainIgnore;

import java.util.List;

public interface DomainIgnoreDao {
    List<DomainIgnore> findByDomain(String domainId);

    DomainIgnore save(DomainIgnore domainIgnore);
    Integer deleteByUpstreamId(String upstreamId);

    DomainIgnore deleteByUpstreamIdAndDomain(String upstreamId, String domainId);

    DomainIgnore deleteByNodeRuleIdAndDomain(String nodeRuleId, String domainId);

    List<DomainIgnore> findByParam(DomainIgnore domainIgnore);
}
