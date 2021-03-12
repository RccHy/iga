package com.qtgl.iga.dao.impl;

import com.qtgl.iga.bo.DeptType;
import com.qtgl.iga.dao.DeptTypeDao;
import com.qtgl.iga.utils.FilterCodeEnum;
import org.springframework.cglib.beans.BeanMap;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.sql.Timestamp;
import java.util.*;


@Repository
@Component
public class DeptTypeDaoImpl implements DeptTypeDao {


    @Resource(name = "jdbcIGA")
    JdbcTemplate jdbcIGA;

    @Override
    public List<DeptType> getAllDeptTypes(Map<String, Object> arguments, String domain) {
        String sql = "select id, code, name, description," +
                "create_time as createTime, update_time as updateTime, " +
                "create_user as createUser, domain from t_mgr_dept_type where 1 = 1 ";
        //拼接sql
        StringBuffer stb = new StringBuffer(sql);
        //存入参数
        List<Object> param = new ArrayList<>();

        dealData(arguments, stb, param);

        List<Map<String, Object>> mapList = jdbcIGA.queryForList(stb.toString(), param.toArray());
        ArrayList<DeptType> list = new ArrayList<>();
        if (null != mapList && mapList.size() > 0) {

            for (Map<String, Object> map : mapList) {
                DeptType deptType = new DeptType();
                BeanMap beanMap = BeanMap.create(deptType);
                beanMap.putAll(map);
                list.add(deptType);
            }
            return list;
        }

        return null;
    }

    @Override
    public DeptType deleteSchemaField(Map<String, Object> arguments, String domain) throws Exception {
        Object[] objects = new Object[2];
        objects[0] = arguments.get("id");
        objects[1] = domain;
        List<Map<String, Object>> mapList = jdbcIGA.queryForList("select id,code,name,description,create_time as createTime," +
                "update_time as updateTime,create_user as createUser, domain from  t_mgr_dept_type  where id =? and domain= ? ", objects);
        ArrayList<DeptType> list = new ArrayList<>();
        if (null != mapList && mapList.size() > 0) {

            for (Map<String, Object> map : mapList) {
                DeptType deptType = new DeptType();
                BeanMap beanMap = BeanMap.create(deptType);
                beanMap.putAll(map);
                list.add(deptType);
            }
        }
        if (null == list || list.size() > 1 || list.size() == 0) {
            throw new Exception("数据异常，删除失败");
        }
        DeptType deptType = list.get(0);


        //删除组织类别数据
        String sql = "delete from t_mgr_dept_type  where id =?";
        int id = jdbcIGA.update(sql, preparedStatement -> preparedStatement.setObject(1, arguments.get("id")));


        return id > 0 ? deptType : null;
    }

    @Override
    public DeptType saveSchemaField(DeptType deptType, String domain) throws Exception {
        //判重
        Object[] param = new Object[]{deptType.getCode(), deptType.getName()};
        List<Map<String, Object>> mapList = jdbcIGA.queryForList("select id,code,name,description,create_time as createTime" +
                ",update_time as updateTime,create_user as createUser, domain from t_mgr_dept_type where code =? or name = ?", param);
        if (null != mapList && mapList.size() > 0) {
            throw new Exception("code 或 name 不能重复,添加组织机构类别失败");
        }

        String sql = "insert into t_mgr_dept_type  values(?,?,?,?,?,?,?,?)";
        //生成主键和时间
        String id = UUID.randomUUID().toString().replace("-", "");
        deptType.setId(id);
        Timestamp date = new Timestamp(new Date().getTime());
        deptType.setCreateTime(date);
        deptType.setUpdateTime(date);
        int update = jdbcIGA.update(sql, preparedStatement -> {
            preparedStatement.setObject(1, id);
            preparedStatement.setObject(2, deptType.getCode());
            preparedStatement.setObject(3, deptType.getName());
            preparedStatement.setObject(4, deptType.getDescription());
            preparedStatement.setObject(5, deptType.getCreateTime());
            preparedStatement.setObject(6, deptType.getUpdateTime());
            preparedStatement.setObject(7, deptType.getCreateUser());
            preparedStatement.setObject(8, domain);

        });
        return update > 0 ? deptType : null;
    }

    @Override
    public DeptType updateSchemaField(DeptType deptType) throws Exception {
        //判重
        Object[] param = new Object[]{deptType.getCode(), deptType.getName(), deptType.getId()};
        List<Map<String, Object>> mapList = jdbcIGA.queryForList("select id,code,name,description,create_time as createTime," +
                "update_time as updateTime,create_user as createUser, domain from t_mgr_dept_type where (code = ? or name = ?) and id != ?  ", param);
        if (null != mapList && mapList.size() > 0) {
            throw new Exception("code 或 name 不能重复,修改组织机构类别失败");
        }
        String sql = "update t_mgr_dept_type  set code = ?,name = ?,description = ?,create_time = ?," +
                "update_time = ?,create_user = ?,domain= ?  where id=?";
        Timestamp date = new Timestamp(new Date().getTime());
        return jdbcIGA.update(sql, preparedStatement -> {
            preparedStatement.setObject(1, deptType.getCode());
            preparedStatement.setObject(2, deptType.getName());
            preparedStatement.setObject(3, deptType.getDescription());
            preparedStatement.setObject(4, deptType.getCreateTime());
            preparedStatement.setObject(5, date);
            preparedStatement.setObject(6, deptType.getCreateUser());
            preparedStatement.setObject(7, deptType.getDomain());
            preparedStatement.setObject(8, deptType.getId());
        }) > 0 ? deptType : null;
    }

    @Override
    public DeptType findById(String id) {
        String sql = "select id, code, name, description," +
                "create_time as createTime, update_time as updateTime, " +
                "create_user as createUser, domain from t_mgr_dept_type where id= ? ";

        List<Map<String, Object>> mapList = jdbcIGA.queryForList(sql, id);
        DeptType deptType = new DeptType();
        if (null != mapList && mapList.size() > 0) {
            for (Map<String, Object> map : mapList) {

                BeanMap beanMap = BeanMap.create(deptType);
                beanMap.putAll(map);
            }
            return deptType;
        }
        return null;
    }

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
                            if (FilterCodeEnum.getDescByCode(soe.getKey()).equals("like")) {
                                stb.append("and code " + FilterCodeEnum.getDescByCode(soe.getKey()) + " ? ");
                                param.add("%" + soe.getValue() + "%");
                            } else if (FilterCodeEnum.getDescByCode(soe.getKey()).equals("in") || FilterCodeEnum.getDescByCode(soe.getKey()).equals("not in")) {
                                stb.append("and code " + FilterCodeEnum.getDescByCode(soe.getKey()) + " ( ");
                                ArrayList<String> value1 = (ArrayList<String>) soe.getValue();
                                for (String s : value1) {
                                    stb.append(" ? ,");
                                    param.add(s);
                                }
                                stb.replace(stb.length() - 1, stb.length(), ")");
                            } else {
                                stb.append("and code " + FilterCodeEnum.getDescByCode(soe.getKey()) + " ? ");
                                param.add(soe.getValue());
                            }
                        }
                    }
                    if (str.getKey().equals("name")) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            if (FilterCodeEnum.getDescByCode(soe.getKey()).equals("like")) {
                                stb.append("and name " + FilterCodeEnum.getDescByCode(soe.getKey()) + " ? ");
                                param.add("%" + soe.getValue() + "%");
                            } else if (FilterCodeEnum.getDescByCode(soe.getKey()).equals("in") || FilterCodeEnum.getDescByCode(soe.getKey()).equals("not in")) {
                                stb.append("and name " + FilterCodeEnum.getDescByCode(soe.getKey()) + " ( ");
                                ArrayList<String> value1 = (ArrayList<String>) soe.getValue();
                                for (String s : value1) {
                                    stb.append(" ? ,");
                                    param.add(s);
                                }
                                stb.replace(stb.length() - 1, stb.length(), ")");
                            } else {
                                stb.append("and name " + FilterCodeEnum.getDescByCode(soe.getKey()) + " ? ");
                                param.add(soe.getValue());
                            }
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