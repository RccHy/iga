package com.qtgl.iga.service;

import com.qtgl.iga.bo.Tenant;

public interface TenantService {
    Tenant findByDomainName(String domainName);
}
