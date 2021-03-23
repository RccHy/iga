package com.qtgl.iga.dao.impl;

import com.qtgl.iga.bo.Dept;
import com.qtgl.iga.dao.DeptDao;
import org.springframework.cglib.beans.BeanMap;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Repository
@Component
public class DeptDaoImpl implements DeptDao {


    @Resource(name = "jdbcSSOAPI")
    JdbcTemplate jdbcSSOAPI;

    @Override
    public List<Dept> getAllDepts() {
        String sql = "select id, code, name, type_id as typeId,create_time as createTime from dept";

        List<Map<String, Object>> mapList = jdbcSSOAPI.queryForList(sql);
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

    @Override
    public Dept findById(String id) {
        String sql = "select id, code, name, type_id as typeId,create_time as createTime from dept where id= ? ";

        List<Map<String, Object>> mapList = jdbcSSOAPI.queryForList(sql, id);
        Dept dept = new Dept();
        if (null != mapList && mapList.size() > 0) {
            for (Map<String, Object> map : mapList) {

                BeanMap beanMap = BeanMap.create(dept);
                beanMap.putAll(map);
            }
            return dept;
        }
        return null;
    }
}
