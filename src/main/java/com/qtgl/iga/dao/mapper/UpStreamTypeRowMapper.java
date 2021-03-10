package com.qtgl.iga.dao.mapper;

import com.qtgl.iga.bo.UpstreamType;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * <FileName> DeptTypeRowMapper
 * <Desc>
 **/
public class UpStreamTypeRowMapper implements RowMapper<UpstreamType> {
    @Override
    public UpstreamType mapRow(ResultSet rs, int i) throws SQLException {
        UpstreamType upStream = new UpstreamType();
        upStream.setId(rs.getString("id"));
        upStream.setUpstreamId(rs.getString("upstream_id"));
        upStream.setDescription(rs.getString("description"));
        upStream.setSynType(rs.getString("syn_type"));
        upStream.setDeptTypeId(rs.getString("dept_typeId"));
        upStream.setEnablePrefix(rs.getBoolean("enable_prefix"));
        upStream.setActive(rs.getBoolean("active"));
        upStream.setActiveTime(rs.getTimestamp("active_time"));
        upStream.setRoot(rs.getBoolean("root"));
        upStream.setCreateTime(rs.getTimestamp("create_time"));
        upStream.setUpdateTime(rs.getTimestamp("update_time"));
        upStream.setServiceCode(rs.getString("service_code"));
        upStream.setGraphqlUrl(rs.getString("graphql_url"));
        upStream.setDomain(rs.getString("domain"));


        return upStream;

    }
}
