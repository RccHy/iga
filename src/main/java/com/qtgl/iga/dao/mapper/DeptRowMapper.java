package com.qtgl.iga.dao.mapper;

import com.qtgl.iga.bo.Dept;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * <FileName> DeptRowMapper
 * <Desc>
 **/
public class DeptRowMapper implements RowMapper<Dept> {
    @Override
    public Dept mapRow(ResultSet rs, int i) throws SQLException {
        Dept dept = new Dept();
        dept.setId(rs.getString("id"));
        dept.setName(rs.getString("name"));
        dept.setCode(rs.getString("code"));
        dept.setTypeId(rs.getString("typeId"));
        dept.setCreateTime(rs.getDate("create_time"));
        return dept;

    }
}
