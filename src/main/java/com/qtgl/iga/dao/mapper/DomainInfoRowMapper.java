package com.qtgl.iga.dao.mapper;

import com.qtgl.iga.bo.DomainInfo;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * <FileName> DeptTypeRowMapper
 * <Desc>
 **/
public class DomainInfoRowMapper implements RowMapper<DomainInfo> {
    @Override
    public DomainInfo mapRow(ResultSet rs, int i) throws SQLException {
        DomainInfo domainInfo = new DomainInfo();
        domainInfo.setId(rs.getString("id"));
        domainInfo.setDomainId(rs.getString("domain_id"));
        domainInfo.setDomainName(rs.getString("domain_name"));
        domainInfo.setClientId(rs.getString("client_id"));
        domainInfo.setClientSecret(rs.getString("client_secret"));
        domainInfo.setCreateTime(rs.getDate("create_time"));
        domainInfo.setCreateUser(rs.getString("create_user"));
        domainInfo.setStatus(rs.getInt("status"));
        return domainInfo;

    }
}
