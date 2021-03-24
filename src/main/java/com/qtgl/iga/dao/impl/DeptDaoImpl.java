package com.qtgl.iga.dao.impl;

import com.qtgl.iga.bean.DeptBean;
import com.qtgl.iga.bo.Dept;
import com.qtgl.iga.dao.DeptDao;
import org.springframework.cglib.beans.BeanMap;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;


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
        if (mapList.size() == 1) {
            for (Map<String, Object> map : mapList) {

                BeanMap beanMap = BeanMap.create(dept);
                beanMap.putAll(map);
            }
            return dept;
        }
        return null;
    }

    @Override
    public List<Dept> findByTenantId(String id) {
        String sql = "select id, dept_code as deptCode , dept_name as deptName , parent_code as parentCode , " +
                "del_mark as delMark , independent , tenant_id as tenantId , update_time as updateTime , source , tags ," +
                "data_source as dataSource , description , orphan, meta ,tree_type as treeType , type , create_time as createTime ," +
                " active from dept where tenant_id = ? ";

        List<Map<String, Object>> mapList = jdbcSSOAPI.queryForList(sql, id);
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
    public ArrayList<DeptBean> updateDept(ArrayList<DeptBean> list, String tenantId) {
        String str = "insert into dept (id,dept_code, dept_name, parent_code, del_mark ,tenant_id ,source, data_source, description, meta,update_time,tags,independent) values" +
                "(?,?,?,?,?,?,?,?,?,?,?,?,?)";
        boolean contains = false;

        int[] ints = jdbcSSOAPI.batchUpdate(str, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                preparedStatement.setObject(1, UUID.randomUUID().toString().replace("-", ""));
                preparedStatement.setObject(2, list.get(i).getCode());
                preparedStatement.setObject(3, list.get(i).getName());
                preparedStatement.setObject(4, list.get(i).getParentCode());
                preparedStatement.setObject(5, 0);
                preparedStatement.setObject(6, tenantId);
                preparedStatement.setObject(7, list.get(i).getSource());
                preparedStatement.setObject(8, list.get(i).getDataSource());
                preparedStatement.setObject(9, list.get(i).getDescription());
                preparedStatement.setObject(10, list.get(i).getMeta());
                preparedStatement.setObject(11, list.get(i).getCreateTime() == null ? LocalDateTime.now() : list.get(i).getCreateTime());
                preparedStatement.setObject(12, list.get(i).getTags());
                preparedStatement.setObject(13, list.get(i).getIndependent());

            }

            @Override
            public int getBatchSize() {
                return list.size();
            }
        });
        contains = contains || Arrays.toString(ints).contains("-1");


        return contains ? null : list;
    }

    @Override
    public ArrayList<DeptBean> saveDept(ArrayList<DeptBean> list, String tenantId) {
        String str = "insert into dept (id,dept_code, dept_name, parent_code, del_mark ,tenant_id ,source, data_source, description, meta,create_time,tags,independent) values" +
                "(?,?,?,?,?,?,?,?,?,?,?,?,?)";
        boolean contains = false;

        int[] ints = jdbcSSOAPI.batchUpdate(str, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                preparedStatement.setObject(1, UUID.randomUUID().toString().replace("-", ""));
                preparedStatement.setObject(2, list.get(i).getCode());
                preparedStatement.setObject(3, list.get(i).getName());
                preparedStatement.setObject(4, list.get(i).getParentCode());
                preparedStatement.setObject(5, 0);
                preparedStatement.setObject(6, tenantId);
                preparedStatement.setObject(7, list.get(i).getSource());
                preparedStatement.setObject(8, list.get(i).getDataSource());
                preparedStatement.setObject(9, list.get(i).getDescription());
                preparedStatement.setObject(10, list.get(i).getMeta());
                preparedStatement.setObject(11, list.get(i).getCreateTime() == null ? LocalDateTime.now() : list.get(i).getCreateTime());
                preparedStatement.setObject(12, list.get(i).getTags());
                preparedStatement.setObject(13, list.get(i).getIndependent());

            }

            @Override
            public int getBatchSize() {
                return list.size();
            }
        });
        contains = contains || Arrays.toString(ints).contains("-1");


        return contains ? null : list;
    }

    @Override
    public ArrayList<DeptBean> deleteDept(ArrayList<DeptBean> list) {
        String sql = "delete from dept where dept_code in(";
        StringBuffer stb = new StringBuffer(sql);
        //存入参数
        List<Object> param = new ArrayList<>();

        dealData(list, stb, param);
        String str = stb.deleteCharAt(stb.length()).append(")").toString();
        int update = jdbcSSOAPI.update(str, param.toArray());
        return update > 0 ? list : null;
    }

    private void dealData(ArrayList<DeptBean> list, StringBuffer stb, List<Object> param) {
        for (DeptBean deptBean : list) {
            stb.append(deptBean.getCode()).append(",");
            param.add(deptBean.getCode());
        }

    }
}
