package com.qtgl.iga.dao.impl;

import com.qtgl.iga.bo.DeptTreeType;
import com.qtgl.iga.bo.UpStream;
import com.qtgl.iga.bo.UpStreamType;
import com.qtgl.iga.dao.DeptTreeTypeDao;
import com.qtgl.iga.dao.UpStreamDao;
import com.qtgl.iga.dao.mapper.UpStreamRowMapper;
import com.qtgl.iga.utils.FilterCodeEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cglib.beans.BeanMap;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;


@Repository
public class DeptTreeTypeDaoImpl implements DeptTreeTypeDao {


    @Resource(name = "jdbcIGA")
    JdbcTemplate jdbcIGA;

    @Autowired
    UpStreamTypeDaoImpl upStreamTypeDao;

    @Override
    public List<DeptTreeType> findAll(Map<String, Object> arguments, String domain) {
        String sql = "select  * from t_mgr_dept_tree_type where 1 = 1 ";
        //拼接sql
        StringBuffer stb = new StringBuffer(sql);
        //存入参数
        List<Object> param = new ArrayList<>();

        dealData(arguments, stb, param);
//        getChild(arguments,param,stb);
        System.out.println(stb.toString());
        List<Map<String, Object>> mapList = jdbcIGA.queryForList(stb.toString(), param.toArray());
        ArrayList<DeptTreeType> list = new ArrayList<>();
        if (null != mapList && mapList.size() > 0) {

            for (Map<String, Object> map : mapList) {
                DeptTreeType deptTreeType = new DeptTreeType();
                BeanMap beanMap = BeanMap.create(deptTreeType);
                beanMap.putAll(map);
                list.add(deptTreeType);
            }
            return list;
        }

        return null;
    }


    @Override
    @Transactional
    public DeptTreeType saveDeptTreeType(DeptTreeType deptTreeType, String domain) {
        String sql = "insert into t_mgr_dept_tree_type  values(?,?,?,?,?,?,?,?,?)";
        //生成主键和时间
        String id = UUID.randomUUID().toString().replace("-", "");
        deptTreeType.setId(id);
        Date date = new Date();
        deptTreeType.setCreateTime(date);
        deptTreeType.setUpdateTime(date);
        int update = jdbcIGA.update(sql, new PreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement preparedStatement) throws SQLException {
                preparedStatement.setObject(1, id);
                preparedStatement.setObject(2, deptTreeType.getCode());
                preparedStatement.setObject(3, deptTreeType.getName());
                preparedStatement.setObject(4, deptTreeType.getDescription());
                preparedStatement.setObject(5, deptTreeType.getMultipleRootNode());
                preparedStatement.setObject(6, deptTreeType.getCreateTime());
                preparedStatement.setObject(7, deptTreeType.getUpdateTime());
                preparedStatement.setObject(8, deptTreeType.getCreateUser());
                preparedStatement.setObject(9, domain);

            }
        });
        return update > 0 ? deptTreeType : null;
    }

    @Override
    @Transactional
    public DeptTreeType deleteDeptTreeType(Map<String, Object> arguments, String domain) throws Exception {
        Object[] objects = new Object[2];
        objects[0] = arguments.get("id");
        objects[1] = domain;
        List<Map<String, Object>> mapList = jdbcIGA.queryForList("select  * from t_mgr_dept_tree_type  where id =? and domain=?", objects);
        ArrayList<DeptTreeType> list = new ArrayList<>();
        if (null != mapList && mapList.size() > 0) {

            for (Map<String, Object> map : mapList) {
                DeptTreeType deptTreeType = new DeptTreeType();
                BeanMap beanMap = BeanMap.create(deptTreeType);
                beanMap.putAll(map);
                list.add(deptTreeType);
            }
        }
        if (null == list || list.size() > 1 || list.size() == 0) {
            throw new Exception("数据异常，删除失败");
        }
        DeptTreeType deptTreeType = list.get(0);


        //删除组织类别数据
        String sql = "delete from t_mgr_dept_tree_type  where id =?";
        int id = jdbcIGA.update(sql, new PreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement preparedStatement) throws SQLException {
                preparedStatement.setObject(1, arguments.get("id"));

            }
        });
        //删除组织类别树数据


        return id > 0 ? deptTreeType : null;

    }

    @Override
    @Transactional
    public DeptTreeType updateDeptTreeType(DeptTreeType deptTreeType) {
        String sql = "update t_mgr_dept_tree_type  set code = ?,name = ?,description = ?,multiple_root_node = ?,create_time = ?," +
                "update_time = ?,create_user = ?,domain= ?  where id=?";
        Date date = new Date();
        return jdbcIGA.update(sql, new PreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement preparedStatement) throws SQLException {
                preparedStatement.setObject(1, deptTreeType.getCode());
                preparedStatement.setObject(2, deptTreeType.getName());
                preparedStatement.setObject(3, deptTreeType.getDescription());
                preparedStatement.setObject(4, deptTreeType.getMultipleRootNode());
                preparedStatement.setObject(5, deptTreeType.getCreateTime());
                preparedStatement.setObject(6, date);
                preparedStatement.setObject(7, deptTreeType.getCreateUser());
                preparedStatement.setObject(8, deptTreeType.getDomain());
                preparedStatement.setObject(9, deptTreeType.getId());
            }
        }) > 0 ? deptTreeType : null;
    }


//    private void getChild(Map<String,Object> map,List<Object> param,StringBuffer sql) {
//        for (Map.Entry<String,Object> entry : map.entrySet()){
//
//
//            if (entry.getKey().equals("filter")){
//                Map<String,Object> value = (Map<String, Object>) entry.getValue();
//                getChild(value,param,sql);
//            }
//
//            sql.append("and "+entry.getKey()+" = ?" );
//            param.add(entry.getValue());
//
//
//        }
//    }

    private void dealData(Map<String, Object> arguments, StringBuffer stb, List<Object> param) {
        Iterator<Map.Entry<String, Object>> it = arguments.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Object> entry = it.next();
            if (entry.getKey().equals("id")) {
                stb.append("and id= ? ");
                param.add(entry.getValue());
            }

            if (entry.getKey().equals("filter")) {
                HashMap<String, Object> map = (HashMap<String, Object>) entry.getValue();
                for (Map.Entry<String, Object> str : map.entrySet()) {
                    if (str.getKey().equals("code")) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            stb.append(" and code " + FilterCodeEnum.getDescByCode(soe.getKey()) + " ? ");
                            param.add(soe.getValue());
                        }
                    }
                    if (str.getKey().equals("name")) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            stb.append("and name " + FilterCodeEnum.getDescByCode(soe.getKey()) + " ? ");
                            param.add(soe.getValue());
                        }
                    }
                    if (str.getKey().equals("createTime")) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            //判断是否是区间
                            if (soe.getKey().equals("gt") || soe.getKey().equals("lt")
                                    || soe.getKey().equals("gte") || soe.getKey().equals("lte")) {
                                stb.append("and create_time " + FilterCodeEnum.getDescByCode(soe.getKey()) + " ? ");
                                param.add(soe.getValue());

                            }
                        }
                    }

                    if (str.getKey().equals("updateTime")) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            //判断是否是区间
                            if (soe.getKey().equals("gt") || soe.getKey().equals("lt")
                                    || soe.getKey().equals("gte") || soe.getKey().equals("lte")) {
                                stb.append("and update_time " + FilterCodeEnum.getDescByCode(soe.getKey()) + " ? ");
                                param.add(soe.getValue());

                            }
                        }
                    }

                }
            }
        }
    }
}
