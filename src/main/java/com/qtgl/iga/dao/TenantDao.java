package com.qtgl.iga.dao;

import com.qtgl.iga.bo.Tenant;

/**
 * <FileName> TenantDao
 * <Desc>
 **/
public interface TenantDao {
    Tenant findByDomainName(String domainName);
}
