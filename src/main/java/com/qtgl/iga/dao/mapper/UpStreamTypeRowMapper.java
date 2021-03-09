package com.qtgl.iga.dao.mapper;

import com.qtgl.iga.bo.UpStreamType;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * <FileName> DeptTypeRowMapper
 * <Desc>
 **/
public class UpStreamTypeRowMapper implements RowMapper<UpStreamType> {
    @Override
    public UpStreamType mapRow(ResultSet rs, int i) throws SQLException {
        UpStreamType upStream = new UpStreamType();
        upStream.setId(rs.getString("id"));
        upStream.setUpstreamId(rs.getString("upstream_id"));
        upStream.setDescription(rs.getString("description"));
        upStream.setSynType(rs.getString("syn_type"));
        upStream.setDeptTypeId(rs.getString("dept_typeId"));
        upStream.setEnablePrefix(rs.getBoolean("enable_prefix"));
        upStream.setActive(rs.getInt("active"));
        upStream.setActiveTime(rs.getDate("active_time"));
        upStream.setRoot(rs.getBoolean("root"));
        upStream.setCreateTime(rs.getDate("create_time"));
        upStream.setUpdateTime(rs.getDate("update_time"));
        upStream.setServiceCode(rs.getString("service_code"));
        upStream.setGraphqlUrl(rs.getString("graphql_url"));
        upStream.setDomain(rs.getString("domain"));


        return upStream;

    }
}
