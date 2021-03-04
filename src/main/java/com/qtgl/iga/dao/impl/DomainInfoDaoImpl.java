package com.qtgl.iga.dao.impl;

import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.dao.DomainInfoDao;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.List;

@Repository
public class DomainInfoDaoImpl implements DomainInfoDao {


     @Resource(name = "jdbcIGA")
     JdbcTemplate jdbcIGA;


    @Override
    public void save(DomainInfo domainInfo) {
        jdbcIGA.update("INSERT INTO `t_mgr_domain_info`(`id`, `domainId`, `domainName`, `clientId`, `clientSecret`, `status`, `createTime`, `updateTime`) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?);",
                domainInfo.getId(),domainInfo.getDomainId(),domainInfo.getDomainName(),domainInfo.getClientId(),domainInfo.getClientSecret(),domainInfo.getStatus(),domainInfo.getCreateTime(),domainInfo.getUpdateTime()
                );
    }

    @Override
    public List<DomainInfo> findAll() {
        List<DomainInfo> domainInfos = jdbcIGA.queryForList("select * from t_mgr_domain_info where status=0", DomainInfo.class);
        return domainInfos;
    }

    @Override
    public DomainInfo getByDomainName(String name) {
        DomainInfo domainInfo=jdbcIGA.queryForObject("select * from t_mgr_domain_info where status=0",DomainInfo.class);
        return domainInfo;
    }
}
