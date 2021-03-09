package com.qtgl.iga.dao.mapper;

import com.qtgl.iga.bo.UpStream;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * <FileName> DeptTypeRowMapper
 * <Desc>
 **/
public class UpStreamRowMapper implements RowMapper<UpStream> {
    @Override
    public UpStream mapRow(ResultSet rs, int i) throws SQLException {
        UpStream upStream = new UpStream();
        upStream.setId(rs.getString("id"));
        upStream.setAppCode(rs.getString("app_code"));
        upStream.setAppName(rs.getString("app_name"));
        upStream.setDataCode(rs.getString("data_code"));
        upStream.setCreateTime(rs.getDate("create_time"));
        upStream.setActiveTime(rs.getDate("active_time"));
        upStream.setUpdateTime(rs.getDate("update_time"));
        upStream.setCreateUser(rs.getString("create_user"));
        upStream.setActive(rs.getBoolean("active"));
        upStream.setColor(rs.getString("color"));
        upStream.setDomain(rs.getString("domain"));
        return upStream;

    }
}
