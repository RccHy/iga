package com.qtgl.iga.dao.impl;

import com.qtgl.iga.bo.Dept;
import com.qtgl.iga.dao.DeptDao;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.List;


@Repository
public class DeptDaoImpl implements DeptDao {


    @Resource(name = "jdbcIGA")
    JdbcTemplate jdbcIGA;

    @Override
    public List<Dept> getAllDepts() {
        return jdbcIGA.queryForList("select  * from Dept ",Dept.class);
    }
}
