package com.qtgl.iga.dao.impl;

import com.qtgl.iga.bo.Tenant;
import com.qtgl.iga.dao.TenantDao;
import org.springframework.cglib.beans.BeanMap;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

/**
 * <FileName> TenantDaoImpl
 * <Desc>
 **/
@Repository
public class TenantDaoImpl implements TenantDao {


    @Resource(name = "jdbcSSO")
    JdbcTemplate jdbcSSO;

    @Override
    public Tenant findByDomainName(String domainName) {
        String sql = "select id, domain, tenant_match as tenantMatch, tenant_name as tenantName,del_mark as delMark," +
                "create_time as createTime ,update_time as updateTime from tenant where domain =? and del_mark=0";

        List<Map<String, Object>> mapList = jdbcSSO.queryForList(sql, domainName);

        Tenant tenant = new Tenant();
        if (mapList.size() == 1) {
            for (Map<String, Object> map : mapList) {

                BeanMap beanMap = BeanMap.create(tenant);
                beanMap.putAll(map);
            }
            return tenant;
        }


        return null;
    }
}
