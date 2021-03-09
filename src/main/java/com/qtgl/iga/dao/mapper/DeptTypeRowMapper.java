package com.qtgl.iga.dao.mapper;

import com.qtgl.iga.bo.DeptType;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * <FileName> DeptTypeRowMapper
 * <Desc>
 **/
public class DeptTypeRowMapper implements RowMapper<DeptType> {
    @Override
    public DeptType mapRow(ResultSet rs, int i) throws SQLException {
        DeptType deptType = new DeptType();
        deptType.setId(rs.getString("id"));
        deptType.setName(rs.getString("name"));
        deptType.setCode(rs.getString("code"));
        deptType.setDescription(rs.getString("description"));
        deptType.setCreateTime(rs.getDate("create_time"));
        deptType.setUpdateTime(rs.getDate("update_time"));
        deptType.setCreateUser(rs.getString("create_user"));
        deptType.setDomain(rs.getString("domain"));
        return deptType;

    }
}
