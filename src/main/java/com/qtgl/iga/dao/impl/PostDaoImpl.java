package com.qtgl.iga.dao.impl;

import com.qtgl.iga.bean.DeptBean;
import com.qtgl.iga.bo.Post;
import com.qtgl.iga.dao.PostDao;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.StringUtils;
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
public class PostDaoImpl implements PostDao {


    @Resource(name = "jdbcSSO")
    JdbcTemplate jdbcSSO;


    @Override
    public List<DeptBean> findByTenantId(String id) {
        //
        String sql = "select  user_type as code , name, parent_code as parentCode , " +
                " update_time as createTime,"  +
                "source,data_source as dataSource from user_type where tenant_id = ? and del_mark=0 ";

        List<Map<String, Object>> mapList = jdbcSSO.queryForList(sql, id);
        return getUserTypes(mapList);
    }

    private List<DeptBean> getUserTypes(List<Map<String, Object>> mapList) {
        ArrayList<DeptBean> list = new ArrayList<>();
        if (null != mapList && mapList.size() > 0) {
            for (Map<String, Object> map : mapList) {
                DeptBean post = new DeptBean();
                BeanMap beanMap = BeanMap.create(post);
                beanMap.putAll(map);
                list.add(post);
            }
            return list;
        }

        return null;
    }

    @Override
    public ArrayList<DeptBean> updateDept(ArrayList<DeptBean> list, String tenantId) {

        String str = "update user_type set  name=?, parent_code=?, del_mark=? ,tenant_id =?" +
                ", data_source=?, description=?, meta=?,update_time=?,tags=?,source=?" +
                "where user_type =?";
        boolean contains = false;

        int[] ints = jdbcSSO.batchUpdate(str, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                preparedStatement.setObject(1, list.get(i).getName());
                preparedStatement.setObject(2, list.get(i).getParentCode());
                preparedStatement.setObject(3, 0);
                preparedStatement.setObject(4, tenantId);
                preparedStatement.setObject(5, "pull");
                preparedStatement.setObject(6, list.get(i).getDescription());
                preparedStatement.setObject(7, list.get(i).getMeta());
                preparedStatement.setObject(8, list.get(i).getCreateTime() == null ? LocalDateTime.now() : list.get(i).getCreateTime());
                preparedStatement.setObject(9, list.get(i).getTags());
                preparedStatement.setObject(10, list.get(i).getSource());
                preparedStatement.setObject(11, list.get(i).getCode());

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
        String str = "insert into user_type (id,user_type, name, parent_code, can_login ,tenant_id ,tags, data_source, description, meta,create_time,del_mark,active,active_time,update_time,source) values" +
                "(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        boolean contains = false;

        int[] ints = jdbcSSO.batchUpdate(str, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                preparedStatement.setObject(1, UUID.randomUUID().toString().replace("-", ""));
                preparedStatement.setObject(2, list.get(i).getCode());
                preparedStatement.setObject(3, list.get(i).getName());
                preparedStatement.setObject(4, list.get(i).getParentCode());
                preparedStatement.setObject(5, 0);
                preparedStatement.setObject(6, tenantId);
                preparedStatement.setObject(7, list.get(i).getTags());
                preparedStatement.setObject(8, "pull");
                preparedStatement.setObject(9, list.get(i).getDescription());
                preparedStatement.setObject(10, list.get(i).getMeta());
                preparedStatement.setObject(11, list.get(i).getCreateTime() == null ? LocalDateTime.now() : list.get(i).getCreateTime());
                preparedStatement.setObject(12, 0);
                preparedStatement.setObject(13, 0);
                preparedStatement.setObject(14, LocalDateTime.now());
                preparedStatement.setObject(15, LocalDateTime.now());
                preparedStatement.setObject(16, list.get(i).getSource());
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
        String str = "update user_type set   del_mark= ? , active = ?,active_time= ?  " +
                "where user_type =?";
        boolean contains = false;

        int[] ints = jdbcSSO.batchUpdate(str, new BatchPreparedStatementSetter() {
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

    @Override
    public List<DeptBean> findRootData(String tenantId) {
        String sql = "select  user_type as code , name as name , parent_code as parentCode , " +
                " tags ,data_source as dataSource , description , meta,source,post_type as postType  " +
                " from user_type where tenant_id=? and del_mark=0 and data_source=?";

        List<Map<String, Object>> mapList = jdbcSSO.queryForList(sql, tenantId, "builtin");
        ArrayList<DeptBean> list = new ArrayList<>();
        if (null != mapList && mapList.size() > 0) {
            for (Map<String, Object> map : mapList) {
                DeptBean deptBean = new DeptBean();
                try {
                    BeanUtils.populate(deptBean, map);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
                if (StringUtils.isBlank(deptBean.getParentCode())) {
                    deptBean.setParentCode("");
                }
                list.add(deptBean);
            }
            return list;
        }

        return null;
    }

    @Override
    public List<DeptBean> findPostType(String id) {
        String sql = "select  user_type as code , name as name , parent_code as parentCode , " +
                " tags ,data_source as dataSource , description , meta,source,post_type as postType  " +
                " from user_type where tenant_id=? and del_mark=0";

        List<Map<String, Object>> mapList = jdbcSSO.queryForList(sql, id);
        ArrayList<DeptBean> list = new ArrayList<>();
        if (null != mapList && mapList.size() > 0) {
            for (Map<String, Object> map : mapList) {
                DeptBean deptBean = new DeptBean();
                try {
                    BeanUtils.populate(deptBean, map);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
                if (StringUtils.isBlank(deptBean.getParentCode())) {
                    deptBean.setParentCode("");
                }
                list.add(deptBean);
            }
            return list;
        }

        return null;
    }
}
