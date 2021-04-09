package com.qtgl.iga.dao.impl;

import com.qtgl.iga.bean.DeptBean;
import com.qtgl.iga.bo.Dept;
import com.qtgl.iga.dao.DeptDao;
import org.apache.commons.beanutils.BeanUtils;
import org.springframework.cglib.beans.BeanMap;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.lang.reflect.InvocationTargetException;
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
        return getDepts(mapList);
    }

    @Override
    public Dept findById(String id) {
        String sql = "select id, code, name, type_id as typeId,create_time as createTime,abbreviation from dept where id= ? ";

        List<Map<String, Object>> mapList = jdbcSSOAPI.queryForList(sql, id);
        Dept dept = new Dept();
        if (null != mapList && mapList.size() == 1) {
            for (Map<String, Object> map : mapList) {

                BeanMap beanMap = BeanMap.create(dept);
                beanMap.putAll(map);
            }
            return dept;
        }
        return null;
    }

    @Override
    public List<DeptBean> findByTenantId(String id, String treeType,Integer delMark) {
        String sql = "select dept_code as code , dept_name as name , parent_code as parentCode , " +
                " update_time as createTime , source, tree_type as treeType,abbreviation  from dept where tenant_id = ? ";
        List<Object> param = new ArrayList<>();
        param.add(id);
        if (null != treeType) {
            sql = sql + " and tree_type=? ";
            param.add(treeType);
        }
        if (null != treeType) {
            sql = sql + " and del_mark=? ";
            param.add(delMark);
        }
        List<Map<String, Object>> mapList = jdbcSSOAPI.queryForList(sql, param.toArray());
        return getDeptBeans(mapList);
    }

    private List<Dept> getDepts(List<Map<String, Object>> mapList) {
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

    private List<DeptBean> getDeptBeans(List<Map<String, Object>> mapList) {
        ArrayList<DeptBean> list = new ArrayList<>();
        if (null != mapList && mapList.size() > 0) {
            for (Map<String, Object> map : mapList) {
                DeptBean dept = new DeptBean();
                try {
                    BeanUtils.populate(dept, map);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
                list.add(dept);
            }
            return list;
        }

        return null;
    }

    @Override
    public ArrayList<DeptBean> updateDept(ArrayList<DeptBean> list, String tenantId) {
        String str = "update dept set  dept_name=?, parent_code=?, del_mark=? ,tenant_id =?" +
                ",source =?, data_source=?, description=?, meta=?,update_time=?,tags=?,independent=?,tree_type= ?,active=? ,abbreviation=?,del_mark=0 " +
                "where dept_code =? and update_time< ?";
        boolean contains = false;

        int[] ints = jdbcSSOAPI.batchUpdate(str, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                preparedStatement.setObject(1, list.get(i).getName());
                preparedStatement.setObject(2, list.get(i).getParentCode());
                preparedStatement.setObject(3, 0);
                preparedStatement.setObject(4, tenantId);
                preparedStatement.setObject(5, list.get(i).getSource());
                preparedStatement.setObject(6, "pull");
                preparedStatement.setObject(7, list.get(i).getDescription());
                preparedStatement.setObject(8, list.get(i).getMeta());
                preparedStatement.setObject(9, list.get(i).getCreateTime() == null ? LocalDateTime.now() : list.get(i).getCreateTime());
                preparedStatement.setObject(10, list.get(i).getTags());
                preparedStatement.setObject(11, list.get(i).getIndependent());
                preparedStatement.setObject(12, list.get(i).getTreeType());
                preparedStatement.setObject(13, 0);
                preparedStatement.setObject(14, null == list.get(i).getAbbreviation() ? null : list.get(i).getAbbreviation());
                preparedStatement.setObject(15, list.get(i).getCode());
                preparedStatement.setObject(16, list.get(i).getCreateTime() == null ? LocalDateTime.now() : list.get(i).getCreateTime());

            }

            @Override
            public int getBatchSize() {
                return list.size();
            }
        });

        contains = Arrays.toString(ints).contains("-1");


        return contains ? null : list;
    }

    @Override
    public ArrayList<DeptBean> saveDept(ArrayList<DeptBean> list, String tenantId) {
        String str = "insert into dept (id,dept_code, dept_name, parent_code, del_mark ,tenant_id ,source, data_source, description, meta,create_time,tags,independent,active,active_time,tree_type,dept_index,abbreviation,update_time) values" +
                "(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
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
                preparedStatement.setObject(8, "pull");
                preparedStatement.setObject(9, list.get(i).getDescription());
                preparedStatement.setObject(10, list.get(i).getMeta());
                preparedStatement.setObject(11, list.get(i).getCreateTime() == null ? LocalDateTime.now() : list.get(i).getCreateTime());
                preparedStatement.setObject(12, list.get(i).getTags());
                preparedStatement.setObject(13, list.get(i).getIndependent());
                preparedStatement.setObject(14, 0);
                preparedStatement.setObject(15, LocalDateTime.now());
                preparedStatement.setObject(16, list.get(i).getTreeType());
                preparedStatement.setObject(17, null == list.get(i).getDeptIndex() ? null : list.get(i).getDeptIndex());
                preparedStatement.setObject(18, null == list.get(i).getAbbreviation() ? null : list.get(i).getAbbreviation());
                preparedStatement.setObject(19, LocalDateTime.now());
            }

            @Override
            public int getBatchSize() {
                return list.size();
            }
        });
        contains = Arrays.toString(ints).contains("-1");


        return contains ? null : list;
    }

    @Override
    public ArrayList<DeptBean> deleteDept(ArrayList<DeptBean> list) {
        String str = "update dept set   del_mark= ? , active = ?,active_time= ?  " +
                "where dept_code =?";
        boolean contains = false;

        int[] ints = jdbcSSOAPI.batchUpdate(str, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                preparedStatement.setObject(1, 1);
                preparedStatement.setObject(2, 1);
                preparedStatement.setObject(3, LocalDateTime.now());
                preparedStatement.setObject(4, list.get(i).getCode());
            }

            @Override
            public int getBatchSize() {
                return list.size();
            }
        });
        contains = Arrays.toString(ints).contains("-1");


        return contains ? null : list;
    }
}
