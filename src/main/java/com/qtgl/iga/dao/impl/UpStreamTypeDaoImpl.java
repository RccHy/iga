package com.qtgl.iga.dao.impl;

import com.qtgl.iga.bo.UpStreamType;
import com.qtgl.iga.bo.UpStreamTypeField;
import com.qtgl.iga.dao.UpStreamTypeDao;
import com.qtgl.iga.dao.mapper.UpStreamTypeRowMapper;
import com.qtgl.iga.utils.FilterCodeEnum;
import org.springframework.cglib.beans.BeanMap;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;


@Repository
public class UpStreamTypeDaoImpl implements UpStreamTypeDao {


    @Resource(name = "jdbcIGA")
    JdbcTemplate jdbcIGA;

    @Override
    public List<UpStreamType> findAll(Map<String, Object> arguments, String domain) {
        String sql = "select  * from t_mgr_upstream_types where 1 = 1 ";
        //拼接sql
        StringBuffer stb = new StringBuffer(sql);
        //存入参数
        List<Object> param = new ArrayList<>();

        dealData(arguments, stb, param);
//        getChild(arguments,param,stb);
        System.out.println(stb.toString());
        return jdbcIGA.query(stb.toString(), param.toArray(), new UpStreamTypeRowMapper());
    }


    @Override
    @Transactional
    public UpStreamType saveUpStreamType(UpStreamType upStreamType, String domain) {
        String sql = "insert into t_mgr_upstream_types  values(?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        //生成主键和时间
        String id = UUID.randomUUID().toString().replace("-", "");
        upStreamType.setId(id);
        Date date = new Date();
        upStreamType.setCreateTime(date);
        //添加上游源类型
        int update = jdbcIGA.update(sql, new PreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement preparedStatement) throws SQLException {
                preparedStatement.setObject(1, id);
                preparedStatement.setObject(2, upStreamType.getUpstreamId());
                preparedStatement.setObject(3, upStreamType.getDescription());
                preparedStatement.setObject(4, upStreamType.getSynType());
                preparedStatement.setObject(5, upStreamType.getDeptTypeId());
                preparedStatement.setObject(6, upStreamType.getEnablePrefix());
                preparedStatement.setObject(7, upStreamType.getActive());
                preparedStatement.setObject(8, upStreamType.getActiveTime());
                preparedStatement.setObject(9, upStreamType.getRoot());
                preparedStatement.setObject(10, upStreamType.getCreateTime());
                preparedStatement.setObject(11, null);
                preparedStatement.setObject(12, upStreamType.getServiceCode());
                preparedStatement.setObject(13, upStreamType.getGraphqlUrl());
                preparedStatement.setObject(14, domain);

            }
        });
        if (null != upStreamType.getUpStreamTypeFields() && upStreamType.getUpStreamTypeFields().size() > 0) {
            //添加类型映射
            String str = "insert into t_mgr_upstream_types_field values(?,?,?,?,?,?,?)";
            for (UpStreamTypeField upStreamTypeField : upStreamType.getUpStreamTypeFields()) {
                upStreamTypeField.setId(UUID.randomUUID().toString().replace("-", ""));
                upStreamTypeField.setUpstreamTypeId(id);
                Date d = new Date();
                upStreamTypeField.setCreateTime(d);
                upStreamTypeField.setUpdateTime(d);
                upStreamTypeField.setDomain(upStreamType.getDomain());
            }
            List<UpStreamTypeField> upStreamTypeFields = upStreamType.getUpStreamTypeFields();
            jdbcIGA.batchUpdate(str, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                    preparedStatement.setObject(1, upStreamTypeFields.get(i).getId());
                    preparedStatement.setObject(2, upStreamTypeFields.get(i).getUpstreamTypeId());
                    preparedStatement.setObject(3, upStreamTypeFields.get(i).getSourceField());
                    preparedStatement.setObject(4, upStreamTypeFields.get(i).getTargetField());
                    preparedStatement.setObject(5, upStreamTypeFields.get(i).getCreateTime());
                    preparedStatement.setObject(6, upStreamTypeFields.get(i).getUpdateTime());
                    preparedStatement.setObject(7, upStreamTypeFields.get(i).getDomain());
                }

                @Override
                public int getBatchSize() {
                    return upStreamTypeFields.size();
                }
            });
        }
        return (update > 0) ? upStreamType : null;
    }

    @Override
    @Transactional
    public UpStreamType deleteUpStreamType(Map<String, Object> arguments, String domain) throws Exception {
        Object[] objects = new Object[2];
        objects[0] = arguments.get("id");
        objects[1] = domain;
        List<UpStreamType> streamTypes = jdbcIGA.query("select  * from t_mgr_upstream_types  where id =? and domain=?", objects, new UpStreamTypeRowMapper());
        if (null == streamTypes || streamTypes.size() > 1) {
            throw new Exception("数据异常，删除失败");
        }
        UpStreamType upStreamType = streamTypes.get(0);
        if (upStreamType.getActive() == 0) {
            throw new Exception("上游源类型已启用,不能进行删除操作");
        }

        //删除
        String sql = "delete from t_mgr_upstream_types  where id =?";
        int id = jdbcIGA.update(sql, new PreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement preparedStatement) throws SQLException {
                preparedStatement.setObject(1, arguments.get("id"));

            }
        });
        //删除类型字段映射
        int result = jdbcIGA.update("delete from t_mgr_upstream_types_field  where upstream_type_id =?", arguments.get("id"));
        return (id > 0) && (result > 0) ? upStreamType : null;


    }

    @Override
    @Transactional
    public UpStreamType updateUpStreamType(UpStreamType upStreamType) {
        String sql = "update t_mgr_upstream_types  set upstream_id = ?,description = ?," +
                "syn_type = ?,dept_typeId = ?,enable_prefix = ?,active = ?,active_time = ?," +
                "root = ?,create_time = ?,update_time = ?,service_code = ?," +
                "graphql_url = ?, domain = ? where id= ? ";
        Date date = new Date();
        upStreamType.setUpdateTime(date);
        int update = jdbcIGA.update(sql, new PreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement preparedStatement) throws SQLException {
                preparedStatement.setObject(1, upStreamType.getUpstreamId());
                preparedStatement.setObject(2, upStreamType.getDescription());
                preparedStatement.setObject(3, upStreamType.getSynType());
                preparedStatement.setObject(4, upStreamType.getDeptTypeId());
                preparedStatement.setObject(5, upStreamType.getEnablePrefix());
                preparedStatement.setObject(6, upStreamType.getActive());
                preparedStatement.setObject(7, upStreamType.getActiveTime());
                preparedStatement.setObject(8, upStreamType.getRoot());
                preparedStatement.setObject(9, upStreamType.getCreateTime());
                preparedStatement.setObject(10, upStreamType.getUpdateTime());
                preparedStatement.setObject(11, upStreamType.getServiceCode());
                preparedStatement.setObject(12, upStreamType.getGraphqlUrl());
                preparedStatement.setObject(13, upStreamType.getDomain());
                preparedStatement.setObject(14, upStreamType.getId());
            }
        });
        //修改
        String str = "update  t_mgr_upstream_types_field set upstream_type_id = ?, source_field= ?," +
                "target_field= ?,create_time= ?,update_time= ?,domain= ? " +
                "where id =?";

        List<UpStreamTypeField> upStreamTypeFields = upStreamType.getUpStreamTypeFields();
        Date d = new Date();
        jdbcIGA.batchUpdate(str, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                preparedStatement.setObject(1, upStreamTypeFields.get(i).getUpstreamTypeId());
                preparedStatement.setObject(2, upStreamTypeFields.get(i).getSourceField());
                preparedStatement.setObject(3, upStreamTypeFields.get(i).getTargetField());
                preparedStatement.setObject(4, upStreamTypeFields.get(i).getCreateTime());
                preparedStatement.setObject(5, d);
                preparedStatement.setObject(6, upStreamTypeFields.get(i).getDomain());
                preparedStatement.setObject(7, upStreamTypeFields.get(i).getId());
            }

            @Override
            public int getBatchSize() {
                return upStreamTypeFields.size();
            }
        });
        return update > 0 ? upStreamType : null;
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
                    if (str.getKey().equals("deptTypeId")) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            stb.append(" and dept_typeId " + FilterCodeEnum.getDescByCode(soe.getKey()) + " ? ");
                            param.add(soe.getValue());
                        }
                    }

                    if (str.getKey().equals("serviceCode")) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            stb.append("and service_code " + FilterCodeEnum.getDescByCode(soe.getKey()) + " ? ");
                            param.add(soe.getValue());
                        }
                    }
                    if (str.getKey().equals("syn_type")) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            stb.append("and state " + FilterCodeEnum.getDescByCode(soe.getKey()) + " ? ");
                            param.add(soe.getValue());
                        }
                    }
                    if (str.getKey().equals("upstreamId")) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            stb.append("and upstream_id " + FilterCodeEnum.getDescByCode(soe.getKey()) + " ? ");
                            param.add(soe.getValue());
                        }
                    }
                    if (str.getKey().equals("active")) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            stb.append("and active " + FilterCodeEnum.getDescByCode(soe.getKey()) + " ? ");
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
            System.out.println("key = " + entry.getKey() + ", value = " + entry.getValue());
        }
    }

    //检查源下的类型是否都处于停用 或者删除。
    public List<UpStreamType> findByUpStreamId(String upId) {
        Object[] params = new Object[1];
        params[0] = upId;
        String sql = "select  * from t_mgr_upstream_types where active = 0 and upstream_id = ?";

        return jdbcIGA.query(sql, params, new UpStreamTypeRowMapper());
    }

    public int deleteByUpStreamId(String id) {
        //删除字段映射表数据
        //查询所有类型
        List<UpStreamType> typeList = jdbcIGA.query("select  * from t_mgr_upstream_types where upstream_id = ?", new UpStreamTypeRowMapper(), id);

        if (null != typeList && typeList.size() > 0) {
            jdbcIGA.batchUpdate("delete from t_mgr_upstream_types_field  where upstream_type_id =?", new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                    preparedStatement.setObject(1, typeList.get(i).getId());
                }

                @Override
                public int getBatchSize() {
                    return typeList.size();
                }
            });
        }

        Object[] params = new Object[1];
        params[0] = id;
        String sql = "delete from t_mgr_upstream_types where  upstream_id = ?";


        return jdbcIGA.update(sql, params);
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
}
