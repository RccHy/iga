package com.qtgl.iga.dao.impl;


import com.qtgl.iga.bo.*;

import com.qtgl.iga.dao.UpstreamDao;
import com.qtgl.iga.dao.UpstreamTypeDao;
import com.qtgl.iga.utils.FilterCodeEnum;
import com.qtgl.iga.vo.UpstreamTypeVo;
import lombok.Setter;
import org.springframework.cglib.beans.BeanMap;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;


@Repository
public class UpstreamTypeDaoImpl implements UpstreamTypeDao {


    @Resource(name = "jdbcIGA")
    JdbcTemplate jdbcIGA;

    @Setter
    UpstreamDao upstreamDao;

    @Override
    public List<UpstreamTypeVo> findAll(Map<String, Object> arguments, String domain) {
        String sql = "select  id,upstream_id as upstreamId ,description,syn_type as synType,dept_type_id as deptTypeId," +
                "enable_prefix as enablePrefix,active,active_time as activeTime,root,create_time as createTime," +
                "update_time as updateTime, graphql_url as graphqlUrl,service_code as serviceCode,domain,dept_tree_type_id as deptTreeTypeId from  t_mgr_upstream_types where 1 = 1 ";
        //拼接sql
        StringBuffer stb = new StringBuffer(sql);
        //存入参数
        List<Object> param = new ArrayList<>();

        dealData(arguments, stb, param);
        List<Map<String, Object>> mapList = jdbcIGA.queryForList(stb.toString(), param.toArray());
        ArrayList<UpstreamTypeVo> list = new ArrayList<>();
        if (null != mapList && mapList.size() > 0) {
            for (Map<String, Object> map : mapList) {
                //上游源类型赋值
                UpstreamTypeVo upstreamTypeVo = new UpstreamTypeVo();
                BeanMap beanMap = BeanMap.create(upstreamTypeVo);
                beanMap.putAll(map);
                List<Map<String, Object>> filedList = jdbcIGA.queryForList("select id,upstream_type_id as upstreamTypeId,source_field as sourceField , target_field as targetField,create_time as createTime,update_time as updateTime,domain from t_mgr_upstream_types_field where upstream_type_id = ? ", upstreamTypeVo.getId());

                //映射字段
                ArrayList<UpstreamTypeField> upstreamTypeFields = getUpstreamTypeFields(filedList);
                upstreamTypeVo.setUpstreamTypeFields(upstreamTypeFields);

                //上游源查询

                Upstream upstream = getUpstream(upstreamTypeVo.getUpstreamId());
                upstreamTypeVo.setUpstream(upstream);

                //组织机构类型查询
                DeptType deptType = getDeptType(upstreamTypeVo.getDeptTypeId());
                upstreamTypeVo.setDeptType(deptType);

                //组织机构类型树查询
                DeptTreeType byId = getDeptTreeType(upstreamTypeVo.getDeptTreeTypeId());
                upstreamTypeVo.setDeptTreeType(byId);

                list.add(upstreamTypeVo);
            }
            return list;
        }

        return null;
    }

    private DeptTreeType getDeptTreeType(String deptTreeTypeId) {
        String sql = "select id, code, name, description," +
                "multiple_root_node as multipleRootNode, create_time as createTime," +
                "update_time as updateTime, create_user as createUser, domain " +
                "from t_mgr_dept_tree_type where id= ? ";

        List<Map<String, Object>> mapList = jdbcIGA.queryForList(sql, deptTreeTypeId);
        DeptTreeType deptTreeType = new DeptTreeType();
        if (null != mapList && mapList.size() > 0) {
            for (Map<String, Object> map : mapList) {

                BeanMap beanMap = BeanMap.create(deptTreeType);
                beanMap.putAll(map);
            }
            return deptTreeType;
        }
        return null;
    }

    private DeptType getDeptType(String deptTypeId) {
        String sql = "select id, code, name, description," +
                "create_time as createTime, update_time as updateTime, " +
                "create_user as createUser, domain from t_mgr_dept_type where id= ? ";

        List<Map<String, Object>> mapList = jdbcIGA.queryForList(sql, deptTypeId);
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

    private Upstream getUpstream(String upstreamId) {
        String sql = "select id,app_code as appCode,app_name as appName,data_code as dataCode," +
                "create_time as createTime,create_user as createUser,active,color,domain ," +
                "active_time as activeTime,update_time as updateTime from t_mgr_upstream where id= ? ";

        List<Map<String, Object>> mapList = jdbcIGA.queryForList(sql, upstreamId);
        Upstream upstream = new Upstream();
        if (null != mapList && mapList.size() > 0) {
            for (Map<String, Object> map : mapList) {

                BeanMap beanMap = BeanMap.create(upstream);
                beanMap.putAll(map);
            }
            return upstream;
        }
        return null;
    }


    private ArrayList<UpstreamTypeField> getUpstreamTypeFields(List<Map<String, Object>> filedList) {
        ArrayList<UpstreamTypeField> upstreamTypeFields = new ArrayList<>();
        for (Map<String, Object> stringObjectMap : filedList) {
            UpstreamTypeField upstreamTypeField = new UpstreamTypeField();
            BeanMap bean = BeanMap.create(upstreamTypeField);
            bean.putAll(stringObjectMap);
            upstreamTypeFields.add(upstreamTypeField);
        }
        return upstreamTypeFields;
    }


    @Override
    @Transactional
    public UpstreamType saveUpstreamType(UpstreamType upStreamType, String domain) {
        String sql = "insert into t_mgr_upstream_types  values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        //生成主键和时间
        String id = UUID.randomUUID().toString().replace("-", "");
        upStreamType.setId(id);
        Timestamp date = new Timestamp(new Date().getTime());
        upStreamType.setCreateTime(date);
        //添加上游源类型
        int update = jdbcIGA.update(sql, preparedStatement -> {
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
            preparedStatement.setObject(12, upStreamType.getGraphqlUrl());
            preparedStatement.setObject(13, upStreamType.getServiceCode());
            preparedStatement.setObject(14, domain);
            preparedStatement.setObject(15, upStreamType.getDeptTreeTypeId());
        });
        if (null != upStreamType.getUpstreamTypeFields() && upStreamType.getUpstreamTypeFields().size() > 0) {
            //添加类型映射
            String str = "insert into t_mgr_upstream_types_field values(?,?,?,?,?,?,?)";
            for (UpstreamTypeField upStreamTypeField : upStreamType.getUpstreamTypeFields()) {
                upStreamTypeField.setId(UUID.randomUUID().toString().replace("-", ""));
                upStreamTypeField.setUpstreamTypeId(id);
                Timestamp d = new Timestamp(new Date().getTime());
                upStreamTypeField.setCreateTime(d);
                upStreamTypeField.setUpdateTime(d);
                upStreamTypeField.setDomain(domain);
            }
            List<UpstreamTypeField> upStreamTypeFields = upStreamType.getUpstreamTypeFields();
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
    public UpstreamType deleteUpstreamType(Map<String, Object> arguments, String domain) throws Exception {
        Object[] objects = new Object[2];
        objects[0] = arguments.get("id");
        objects[1] = domain;
        List<Map<String, Object>> mapList = jdbcIGA.queryForList("select  id,upstream_id as upstreamId ,description,syn_type as synType," +
                "dept_type_id as deptTypeId,enable_prefix as enablePrefix,active,active_time as activeTime," +
                "root,create_time as createTime,update_time as updateTime, graphql_url as graphqlUrl," +
                "service_code as serviceCode,domain,dept_tree_type_id as deptTreeTypeId from  t_mgr_upstream_types  where id =? and domain=?", objects);
        ArrayList<UpstreamType> streamTypes = new ArrayList<>();
        if (null != mapList && mapList.size() > 0) {
            for (Map<String, Object> map : mapList) {
                UpstreamType upstreamType = new UpstreamType();
                BeanMap beanMap = BeanMap.create(upstreamType);
                beanMap.putAll(map);
                streamTypes.add(upstreamType);
            }
        }
        if (null == streamTypes || streamTypes.size() > 1) {
            throw new Exception("数据异常，删除失败");
        }
        UpstreamType upStreamType = streamTypes.get(0);
        if (upStreamType.getActive()) {
            throw new Exception("上游源类型已启用,不能进行删除操作");
        }

        //删除
        String sql = "delete from t_mgr_upstream_types  where id =?";
        int id = jdbcIGA.update(sql, preparedStatement -> preparedStatement.setObject(1, arguments.get("id")));
        //删除类型字段映射
        jdbcIGA.update("delete from t_mgr_upstream_types_field  where upstream_type_id =?", arguments.get("id"));
        return id > 0 ? upStreamType : null;


    }

    @Override
    @Transactional
    public UpstreamType updateUpstreamType(UpstreamType upStreamType) throws Exception {
        //修改前判断上游源启用状态
        if (!upStreamType.getActive()) {
            //判断类型是否都未启用
            Upstream byId = upstreamDao.findById(upStreamType.getUpstreamId());
            if (null != byId && !byId.getActive()) {
                throw new Exception("上游源类型启用失败,请检查相关上游源启用状态");
            }
        }

        String sql = "update t_mgr_upstream_types  set upstream_id = ?,description = ?," +
                "syn_type = ?,dept_type_id = ?,enable_prefix = ?,active = ?,active_time = ?," +
                "root = ?,create_time = ?,update_time = ?,service_code = ?," +
                "graphql_url = ?, domain = ? ,dept_tree_type_id = ? where id= ? ";
        Timestamp date = new Timestamp(new Date().getTime());
        upStreamType.setUpdateTime(date);
        int update = jdbcIGA.update(sql, preparedStatement -> {
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
            preparedStatement.setObject(14, upStreamType.getDeptTreeTypeId());
            preparedStatement.setObject(15, upStreamType.getId());

        });
        //修改
        String str = "update  t_mgr_upstream_types_field set upstream_type_id = ?, source_field= ?," +
                "target_field= ?,create_time= ?,update_time= ?,domain= ? " +
                "where id =?";

        List<UpstreamTypeField> upStreamTypeFields = upStreamType.getUpstreamTypeFields();
        Timestamp d = new Timestamp(new Date().getTime());
        if (null != upStreamTypeFields && upStreamTypeFields.size() > 0) {
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
        }
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
                            if (FilterCodeEnum.getDescByCode(soe.getKey()).equals("like")) {
                                stb.append("and dept_type_id " + FilterCodeEnum.getDescByCode(soe.getKey()) + " ? ");
                                param.add("%" + soe.getValue() + "%");
                            } else if (FilterCodeEnum.getDescByCode(soe.getKey()).equals("in") || FilterCodeEnum.getDescByCode(soe.getKey()).equals("not in")) {
                                stb.append("and dept_type_id " + FilterCodeEnum.getDescByCode(soe.getKey()) + " ( ");
                                ArrayList<String> value1 = (ArrayList<String>) soe.getValue();
                                for (String s : value1) {
                                    stb.append(" ? ,");
                                    param.add(s);
                                }
                                stb.replace(stb.length() - 1, stb.length(), ")");
                            } else {
                                stb.append("and dept_type_id " + FilterCodeEnum.getDescByCode(soe.getKey()) + " ? ");
                                param.add(soe.getValue());
                            }
                        }
                    }

                    if (str.getKey().equals("serviceCode")) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            if (FilterCodeEnum.getDescByCode(soe.getKey()).equals("like")) {
                                stb.append("and service_code ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add("%" + soe.getValue() + "%");
                            } else if (FilterCodeEnum.getDescByCode(soe.getKey()).equals("in") || FilterCodeEnum.getDescByCode(soe.getKey()).equals("not in")) {
                                stb.append("and service_code ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ( ");
                                ArrayList<String> value1 = (ArrayList<String>) soe.getValue();
                                for (String s : value1) {
                                    stb.append(" ? ,");
                                    param.add(s);
                                }
                                stb.replace(stb.length() - 1, stb.length(), ")");
                            } else {
                                stb.append("and service_code ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add(soe.getValue());
                            }
                        }
                    }
                    if (str.getKey().equals("synType")) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            if (Objects.equals(FilterCodeEnum.getDescByCode(soe.getKey()), "like")) {
                                stb.append("and syn_type ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add("%" + soe.getValue() + "%");
                            } else if (FilterCodeEnum.getDescByCode(soe.getKey()).equals("in") || FilterCodeEnum.getDescByCode(soe.getKey()).equals("not in")) {
                                stb.append("and syn_type ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ( ");
                                ArrayList<String> value1 = (ArrayList<String>) soe.getValue();
                                for (String s : value1) {
                                    stb.append(" ? ,");
                                    param.add(s);
                                }
                                stb.replace(stb.length() - 1, stb.length(), ")");
                            } else {
                                stb.append("and syn_type ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add(soe.getValue());
                            }
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
                                stb.append("and create_time ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
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
                                stb.append("and update_time ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add(soe.getValue());

                            }
                        }
                    }

                }

            }
        }
    }

    //检查源下的类型是否都处于停用 或者删除。
    public List<UpstreamType> findByUpstreamId(String upId) {
        Object[] params = new Object[1];
        params[0] = upId;
        String sql = "select  id,upstream_id as upstreamId ,description,syn_type as synType,dept_type_id as deptTypeId,enable_prefix as enablePrefix,active,active_time asactiveTime,root,create_time as createTime,update_time as updateTime," +
                " graphql_url as graphqlUrl,service_code as serviceCode,domain from  t_mgr_upstream_types where active = 0 and upstream_id = ?";
        List<Map<String, Object>> mapList = jdbcIGA.queryForList(sql, params);
        ArrayList<UpstreamType> list = new ArrayList<>();
        if (null != mapList && mapList.size() > 0) {
            for (Map<String, Object> map : mapList) {
                UpstreamType upstreamType = new UpstreamType();
                BeanMap beanMap = BeanMap.create(upstreamType);
                beanMap.putAll(map);
                list.add(upstreamType);
            }
            return list;
        }

        return null;

    }

    @Override
    public UpstreamType findById(String id) {
        String sql = "select  id,upstream_id as upstreamId ,description,syn_type as synType,dept_type_id as deptTypeId," +
                "enable_prefix as enablePrefix,active,active_time as activeTime,root,create_time as createTime," +
                "update_time as updateTime, graphql_url as graphqlUrl,service_code as serviceCode,domain,dept_tree_type_id as deptTreeTypeId from  t_mgr_upstream_types where id= ? ";

        List<Map<String, Object>> mapList = jdbcIGA.queryForList(sql, id);
        UpstreamType upstreamType = new UpstreamType();
        if (null != mapList && mapList.size() > 0) {
            for (Map<String, Object> map : mapList) {

                BeanMap beanMap = BeanMap.create(upstreamType);
                beanMap.putAll(map);
            }
            return upstreamType;
        }
        return null;

    }

    @Override
    public List<UpstreamTypeField> findFields(String url) {
        //查询类型id
        String sql = "select  id from  t_mgr_upstream_types where active = 1 and graphql_url = ?";
        Map<String, Object> map = jdbcIGA.queryForMap(sql, url);
        //查询映射字段
        List<Map<String, Object>> filedList = jdbcIGA.queryForList("select id,upstream_type_id as upstreamTypeId,source_field as sourceField , target_field as targetField,create_time as createTime,update_time as updateTime,domain from t_mgr_upstream_types_field where upstream_type_id = ? ", map.get("id"));
        ArrayList<UpstreamTypeField> upstreamTypeFields = getUpstreamTypeFields(filedList);
        return upstreamTypeFields;
    }

    public Integer deleteByUpstreamId(String id) {
        //删除字段映射表数据
        //查询所有类型
        List<Map<String, Object>> mapList = jdbcIGA.queryForList("select  id,upstream_id as upstreamId ,description,syn_type as synType,dept_type_id as deptTypeId,enable_prefix as enablePrefix,active,active_time asactiveTime,root,create_time as createTime,update_time as updateTime," +
                " graphql_url as graphqlUrl,service_code as serviceCode,domain,dept_tree_type_id as deptTreeTypeId from  t_mgr_upstream_types where upstream_id = ?", id);

        ArrayList<UpstreamType> typeList = new ArrayList<>();
        if (null != mapList && mapList.size() > 0) {
            for (Map<String, Object> map : mapList) {
                UpstreamType upstreamType = new UpstreamType();
                BeanMap beanMap = BeanMap.create(upstreamType);
                beanMap.putAll(map);
                typeList.add(upstreamType);
            }
        }
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
}
