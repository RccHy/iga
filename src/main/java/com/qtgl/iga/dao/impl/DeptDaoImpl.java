package com.qtgl.iga.dao.impl;

import com.qtgl.iga.bo.Dept;
import com.qtgl.iga.dao.DeptDao;
import org.springframework.cglib.beans.BeanMap;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Repository
public class DeptDaoImpl implements DeptDao {


    @Resource(name = "jdbcIGA")
    JdbcTemplate jdbcIGA;

    @Override
    public List<Dept> getAllDepts() {
        String sql = "select id, code, name, type_id as typeId,create_time as createTime from dept";

        List<Map<String, Object>> mapList = jdbcIGA.queryForList(sql);
        ArrayList<Dept> list = new ArrayList<>();
        if (null != mapList && mapList.size() > 0) {
            for (Map<String, Object> map : mapList) {
                Dept dept = new Dept();
                BeanMap beanMap = BeanMap.create(dept);
                beanMap.putAll(map);
                list.add(dept);
            }
            return list;
        }

        return null;
    }
}
