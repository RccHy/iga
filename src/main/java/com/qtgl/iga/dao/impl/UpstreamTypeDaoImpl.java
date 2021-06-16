package com.qtgl.iga.dao.impl;


import com.qtgl.iga.bo.*;

import com.qtgl.iga.dao.UpstreamTypeDao;
import com.qtgl.iga.utils.FilterCodeEnum;
import com.qtgl.iga.utils.enumerate.ResultCode;
import com.qtgl.iga.utils.exception.CustomException;
import com.qtgl.iga.vo.UpstreamTypeVo;
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


    @Override
    public List<UpstreamTypeVo> findAll(Map<String, Object> arguments, String domain) {
        String sql = "select  id,upstream_id as upstreamId ,description,syn_type as synType,dept_type_id as deptTypeId," +
                "enable_prefix as enablePrefix,active,active_time as activeTime,root,create_time as createTime," +
                "update_time as updateTime, graphql_url as graphqlUrl,service_code as serviceCode,domain,dept_tree_type_id as deptTreeTypeId , is_page as isPage, syn_way as synWay from  t_mgr_upstream_types where 1 = 1  and active = 1 ";
        //拼接sql
        StringBuffer stb = new StringBuffer(sql);
        //存入参数
        List<Object> param = new ArrayList<>();

        dealData(arguments, stb, param);
        List<Map<String, Object>> mapList = jdbcIGA.queryForList(stb.toString(), param.toArray());
        ArrayList<UpstreamTypeVo> list = new ArrayList<>();
        if (null != mapList && mapList.size() > 0) {
            for (Map<String, Object> map : mapList) {
                //权威源类型赋值
                UpstreamTypeVo upstreamTypeVo = new UpstreamTypeVo();
                BeanMap beanMap = BeanMap.create(upstreamTypeVo);
                beanMap.putAll(map);
                List<Map<String, Object>> filedList = jdbcIGA.queryForList("select id,upstream_type_id as upstreamTypeId,source_field as sourceField , target_field as targetField,create_time as createTime,update_time as updateTime,domain from t_mgr_upstream_types_field where upstream_type_id = ? ", upstreamTypeVo.getId());

                //映射字段
                ArrayList<UpstreamTypeField> upstreamTypeFields = getUpstreamTypeFields(filedList);
                upstreamTypeVo.setUpstreamTypeFields(upstreamTypeFields);

                //权威源查询

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
                "active_time as activeTime,update_time as updateTime  from t_mgr_upstream where id= ? ";

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


        String sql = "insert into t_mgr_upstream_types  values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        //生成主键和时间
        String id = UUID.randomUUID().toString().replace("-", "");
        upStreamType.setId(id);
        Timestamp date = new Timestamp(System.currentTimeMillis());
        upStreamType.setCreateTime(date);
        upStreamType.setDomain(domain);
        //添加权威源类型
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
            preparedStatement.setObject(16, upStreamType.getIsPage());
            preparedStatement.setObject(17, upStreamType.getSynWay());
        });
        if (null != upStreamType.getUpstreamTypeFields() && upStreamType.getUpstreamTypeFields().size() > 0) {
            saveUpstreamTypeField(upStreamType);
        }
        return (update > 0) ? upStreamType : null;
    }

    private int[] saveUpstreamTypeField(UpstreamType upStreamType) {
        //添加类型映射
        String str = "insert into t_mgr_upstream_types_field values(?,?,?,?,?,?,?)";
        for (UpstreamTypeField upStreamTypeField : upStreamType.getUpstreamTypeFields()) {
            upStreamTypeField.setId(UUID.randomUUID().toString().replace("-", ""));
            upStreamTypeField.setUpstreamTypeId(upStreamType.getId());
            Timestamp d = new Timestamp(System.currentTimeMillis());
            upStreamTypeField.setCreateTime(d);
            upStreamTypeField.setUpdateTime(d);
            upStreamTypeField.setDomain(upStreamType.getDomain());
        }
        List<UpstreamTypeField> upStreamTypeFields = upStreamType.getUpstreamTypeFields();
        return jdbcIGA.batchUpdate(str, new BatchPreparedStatementSetter() {
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

    @Override
    @Transactional
    public UpstreamType deleteUpstreamType(String id, String domain) throws Exception {
        Object[] objects = new Object[2];
        objects[0] = id;
        objects[1] = domain;
        List<Map<String, Object>> mapList = jdbcIGA.queryForList("select  id,upstream_id as upstreamId ,description,syn_type as synType," +
                "dept_type_id as deptTypeId,enable_prefix as enablePrefix,active,active_time as activeTime," +
                "root,create_time as createTime,update_time as updateTime, graphql_url as graphqlUrl," +
                "service_code as serviceCode,domain,dept_tree_type_id as deptTreeTypeId,syn_way as synWay from  t_mgr_upstream_types  where id =? and domain= ?", objects);
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
            throw new CustomException(ResultCode.FAILED, "数据异常，删除失败");
        }


        //删除
        String sql = "delete from t_mgr_upstream_types  where id =?";
        int i = jdbcIGA.update(sql, preparedStatement -> preparedStatement.setObject(1, id));
        //删除类型字段映射
        jdbcIGA.update("delete from t_mgr_upstream_types_field  where upstream_type_id =?", id);
        return i > 0 ? new UpstreamType() : null;


    }

    @Override
    @Transactional
    public UpstreamType updateUpstreamType(UpstreamType upStreamType) {

        String sql = "update t_mgr_upstream_types  set upstream_id = ?,description = ?," +
                "syn_type = ?,dept_type_id = ?,enable_prefix = ?,active = ?,active_time = ?," +
                "root = ?,create_time = ?,update_time = ?,service_code = ?," +
                "graphql_url = ?, domain = ? ,dept_tree_type_id = ? ,is_page = ? , syn_way = ? where id= ? ";
        Timestamp date = new Timestamp(System.currentTimeMillis());
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
            preparedStatement.setObject(15, upStreamType.getIsPage());
            preparedStatement.setObject(16, upStreamType.getSynWay());
            preparedStatement.setObject(17, upStreamType.getId());

        });
        int[] ints = updateUpstreamTypeFields(upStreamType);
        if (null == ints) {
            throw new CustomException(ResultCode.FAILED, "修改权威源类型失败");
        }

        return (update <= 0 || Arrays.toString(ints).contains("-1")) ? null : upStreamType;
    }

    private int[] updateUpstreamTypeFields(UpstreamType upStreamType) {
        //删除
        String del = "delete from t_mgr_upstream_types_field  where upstream_type_id =?";
        int update = jdbcIGA.update(del, upStreamType.getId());
        //新增
        if (update >= 0) {
            return saveUpstreamTypeField(upStreamType);
        }


        return null;
    }


    private void dealData(Map<String, Object> arguments, StringBuffer stb, List<Object> param) {
        Iterator<Map.Entry<String, Object>> it = arguments.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Object> entry = it.next();
            if ("id".equals(entry.getKey())) {
                stb.append("and id= ? ");
                param.add(entry.getValue());
            }

            if ("filter".equals(entry.getKey())) {
                HashMap<String, Object> map = (HashMap<String, Object>) entry.getValue();
                for (Map.Entry<String, Object> str : map.entrySet()) {
                    if ("deptTypeId".equals(str.getKey())) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            if ("like".equals(FilterCodeEnum.getDescByCode(soe.getKey()))) {
                                stb.append("and dept_type_id ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add("%" + soe.getValue() + "%");
                            } else if ("in".equals(FilterCodeEnum.getDescByCode(soe.getKey())) || FilterCodeEnum.getDescByCode(soe.getKey()).equals("not in")) {
                                stb.append("and dept_type_id ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ( ");
                                ArrayList<String> value1 = (ArrayList<String>) soe.getValue();
                                for (String s : value1) {
                                    stb.append(" ? ,");
                                    param.add(s);
                                }
                                stb.replace(stb.length() - 1, stb.length(), ")");
                            } else {
                                stb.append("and dept_type_id ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add(soe.getValue());
                            }
                        }
                    }

                    if ("serviceCode".equals(str.getKey())) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            if ("like".equals(FilterCodeEnum.getDescByCode(soe.getKey()))) {
                                stb.append("and service_code ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add("%" + soe.getValue() + "%");
                            } else if ("in".equals(FilterCodeEnum.getDescByCode(soe.getKey())) || FilterCodeEnum.getDescByCode(soe.getKey()).equals("not in")) {
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
                    if ("synType".equals(str.getKey())) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            if (Objects.equals(FilterCodeEnum.getDescByCode(soe.getKey()), "like")) {
                                stb.append("and syn_type ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add("%" + soe.getValue() + "%");
                            } else if ("in".equals(FilterCodeEnum.getDescByCode(soe.getKey())) || FilterCodeEnum.getDescByCode(soe.getKey()).equals("not in")) {
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
                    if ("upstreamId".equals(str.getKey())) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            stb.append("and upstream_id ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                            param.add(soe.getValue());
                        }
                    }
                    if ("active".equals(str.getKey())) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            stb.append("and active ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                            param.add(soe.getValue());
                        }
                    }
                    if ("createTime".equals(str.getKey())) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            //判断是否是区间
                            if ("gt".equals(soe.getKey()) || "lt".equals(soe.getKey())
                                    || "gte".equals(soe.getKey()) || "lte".equals(soe.getKey())) {
                                stb.append("and create_time ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add(soe.getValue());

                            }
                        }
                    }
                    if ("updateTime".equals(str.getKey())) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            //判断是否是区间
                            if ("gt".equals(soe.getKey()) || "lt".equals(soe.getKey())
                                    || "gte".equals(soe.getKey()) || "lte".equals(soe.getKey())) {
                                stb.append("and update_time ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add(soe.getValue());

                            }
                        }
                    }

                    if ("synWay".equals(str.getKey())) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            stb.append("and syn_way ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                            param.add(soe.getValue());
                        }
                    }

                }

            }
        }
    }

    /**
     * 检查源下的类型是否都处于停用 或者删除。
     */
    @Override
    public List<UpstreamType> findByUpstreamId(String upId) {
        Object[] params = new Object[1];
        params[0] = upId;
        String sql = "select  id,upstream_id as upstreamId ,description,syn_type as synType,dept_type_id as deptTypeId,enable_prefix as enablePrefix,active,active_time asactiveTime,root,create_time as createTime,update_time as updateTime," +
                " graphql_url as graphqlUrl,service_code as serviceCode,domain,syn_way as synWay,is_page as isPage from  t_mgr_upstream_types where  upstream_id = ?";
        List<Map<String, Object>> mapList = jdbcIGA.queryForList(sql, params);
        ArrayList<UpstreamType> list = new ArrayList<>();
        if (null != mapList && mapList.size() > 0) {
            for (Map<String, Object> map : mapList) {
                UpstreamType upstreamType = new UpstreamType();
                BeanMap beanMap = BeanMap.create(upstreamType);
                beanMap.putAll(map);
                list.add(upstreamType);
            }
            for (UpstreamType upstreamType : list) {
                List<UpstreamTypeField> fields = findFields(upstreamType.getId());
                upstreamType.setUpstreamTypeFields(fields);
            }
            return list;
        }


        return null;

    }

    @Override
    public UpstreamType findById(String id) {
        String sql = "select  id,upstream_id as upstreamId ,description,syn_type as synType,dept_type_id as deptTypeId," +
                "enable_prefix as enablePrefix,active,active_time as activeTime,root,create_time as createTime," +
                "update_time as updateTime, graphql_url as graphqlUrl,service_code as serviceCode,domain,dept_tree_type_id as deptTreeTypeId , " +
                "is_page as isPage,syn_way as synWay  from  t_mgr_upstream_types where id= ?   ";

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
    public List<UpstreamTypeField> findFields(String id) {

        //查询映射字段
        List<Map<String, Object>> filedList = jdbcIGA.queryForList("select id,upstream_type_id as upstreamTypeId,source_field as sourceField , target_field as targetField,create_time as createTime,update_time as updateTime,domain from t_mgr_upstream_types_field where upstream_type_id = ? ", id);
        return getUpstreamTypeFields(filedList);
    }


    @Override
    public Integer deleteByUpstreamId(String id, String domain) {
        //删除字段映射表数据
        //查询所有类型
        List<Map<String, Object>> mapList = jdbcIGA.queryForList("select  id,upstream_id as upstreamId ,description,syn_type as synType,dept_type_id as deptTypeId,enable_prefix as enablePrefix,active,active_time asactiveTime,root,create_time as createTime,update_time as updateTime," +
                " graphql_url as graphqlUrl,service_code as serviceCode,domain,dept_tree_type_id as deptTreeTypeId ,syn_way as synWay from  t_mgr_upstream_types where upstream_id = ? and domain =? ", id, domain);

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
