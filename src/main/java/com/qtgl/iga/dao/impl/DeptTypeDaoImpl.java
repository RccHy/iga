package com.qtgl.iga.dao.impl;

import com.qtgl.iga.bo.DeptType;
import com.qtgl.iga.dao.DeptTypeDao;
import com.qtgl.iga.dao.mapper.DeptTypeRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.List;


@Repository
public class DeptTypeDaoImpl implements DeptTypeDao {


    @Resource(name = "jdbcIGA")
    JdbcTemplate jdbcIGA;

    @Override
    public List<DeptType> getAllDeptTypes() {
        return jdbcIGA.query("select  * from t_mgr_dept_type ", new DeptTypeRowMapper());
    }
}
