package com.qtgl.iga.dao.impl;


import com.qtgl.iga.bo.Upstream;
import com.qtgl.iga.dao.UpstreamDao;
import com.qtgl.iga.utils.FilterCodeEnum;
import com.qtgl.iga.utils.enumerate.ResultCode;
import com.qtgl.iga.utils.exception.CustomException;
import org.springframework.cglib.beans.BeanMap;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import java.sql.Timestamp;
import java.util.*;


@Repository
@Component
public class UpstreamDaoImpl implements UpstreamDao {


    @Resource(name = "jdbcIGA")
    JdbcTemplate jdbcIGA;


    @Override
    public List<Upstream> findAll(Map<String, Object> arguments, String domain) {
        String sql = "select id,app_code as appCode,app_name as appName,data_code as dataCode," +
                "create_time as createTime,create_user as createUser,active,color,domain ," +
                "active_time as activeTime,update_time as updateTime from t_mgr_upstream where 1 = 1 ";
        //拼接sql
        StringBuffer stb = new StringBuffer(sql);
        //存入参数
        List<Object> param = new ArrayList<>();

        dealData(arguments, stb, param);
        List<Map<String, Object>> mapList = jdbcIGA.queryForList(stb.toString(), param.toArray());

        ArrayList<Upstream> list = new ArrayList<>();
        if (null != mapList && mapList.size() > 0) {
            for (Map<String, Object> map : mapList) {
                Upstream upstream = new Upstream();
                BeanMap beanMap = BeanMap.create(upstream);
                beanMap.putAll(map);
                list.add(upstream);
            }
            return list;
        }
        return null;
    }


    @Override
    @Transactional
    public Upstream saveUpstream(Upstream upstream, String domain) throws Exception {
        //判重
        Object[] param = new Object[]{upstream.getAppCode(), upstream.getAppName()};
        List<Map<String, Object>> mapList = jdbcIGA.queryForList("select  * from t_mgr_upstream where app_code =? or app_name = ?", param);
        if (null != mapList && mapList.size() > 0) {
            throw new CustomException(ResultCode.REPEAT_UPSTREAM_ERROR, null, null, upstream.getAppCode(), upstream.getAppName());
        }
        String sql = "insert into t_mgr_upstream  values(?,?,?,?,?,?,?,?,?,?,?)";
        //生成主键和时间
        String id = UUID.randomUUID().toString().replace("-", "");
        upstream.setId(id);
        Timestamp date = new Timestamp(System.currentTimeMillis());
        upstream.setCreateTime(date);
        int update = jdbcIGA.update(sql, preparedStatement -> {
            preparedStatement.setObject(1, id);
            preparedStatement.setObject(2, upstream.getAppCode());
            preparedStatement.setObject(3, upstream.getAppName());
            preparedStatement.setObject(4, upstream.getDataCode());
            preparedStatement.setObject(5, date);
            preparedStatement.setObject(6, upstream.getCreateUser());
            preparedStatement.setObject(7, upstream.getActive());
            preparedStatement.setObject(8, upstream.getColor());
            preparedStatement.setObject(9, domain);
            preparedStatement.setObject(10, date);
            preparedStatement.setObject(11, date);

        });
        return update > 0 ? upstream : null;
    }

    @Override
    @Transactional
    public Integer deleteUpstream(String id) {

        //删除权威源数据
        String sql = "delete from t_mgr_upstream  where id =?";


        return jdbcIGA.update(sql, preparedStatement -> preparedStatement.setObject(1, id));


    }

    @Override
    public ArrayList<Upstream> getUpstreams(String id, String domain) {
        //查询权威源状态
        Object[] objects = new Object[2];
        objects[0] = id;
        objects[1] = domain;
        List<Map<String, Object>> mapList = jdbcIGA.queryForList("select id,app_code as appCode,app_name as appName,data_code as dataCode,create_time as createTime,create_user as createUser,active,color,domain ,active_time as activeTime,update_time as updateTime from t_mgr_upstream  where id =? and domain=?", objects);

        ArrayList<Upstream> upstreamList = new ArrayList<>();
        if (null != mapList && mapList.size() > 0) {
            for (Map<String, Object> map : mapList) {
                Upstream upstream = new Upstream();
                BeanMap beanMap = BeanMap.create(upstream);
                beanMap.putAll(map);
                upstreamList.add(upstream);
            }
        }
        return upstreamList;
    }

    @Override
    @Transactional
    public Upstream updateUpstream(Upstream upstream) {
        //判重
        Object[] param = new Object[]{upstream.getAppCode(), upstream.getAppName(), upstream.getId()};
        List<Map<String, Object>> mapList = jdbcIGA.queryForList("select  * from t_mgr_upstream where (app_code = ? or app_name = ?) and id != ?  ", param);
        if (null != mapList && mapList.size() > 0) {
            throw new CustomException(ResultCode.FAILED, "code 或 name 不能重复,修改失败");
        }


        String sql = "update t_mgr_upstream  set app_code = ?,app_name = ?,data_code = ?,create_user = ?,active = ?," +
                "color = ?,domain = ?,active_time = ?,update_time= ?  where id=? ";
        Timestamp date = new Timestamp(System.currentTimeMillis());
        int update = jdbcIGA.update(sql, preparedStatement -> {
            preparedStatement.setObject(1, upstream.getAppCode());
            preparedStatement.setObject(2, upstream.getAppName());
            preparedStatement.setObject(3, upstream.getDataCode());
            preparedStatement.setObject(4, upstream.getCreateUser());
            preparedStatement.setObject(5, upstream.getActive());
            preparedStatement.setObject(6, upstream.getColor());
            preparedStatement.setObject(7, upstream.getDomain());
            preparedStatement.setObject(8, date);
            preparedStatement.setObject(9, date);
            preparedStatement.setObject(10, upstream.getId());
        });
        return update > 0 ? upstream : null;
    }

    @Override
    public Upstream findById(String id) {
        String sql = "select id,app_code as appCode,app_name as appName,data_code as dataCode," +
                "create_time as createTime,create_user as createUser,active,color,domain ," +
                "active_time as activeTime,update_time as updateTime from t_mgr_upstream where id= ? ";

        List<Map<String, Object>> mapList = jdbcIGA.queryForList(sql, id);
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
                    if ("appCode".equals(str.getKey())) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            if ("like".equals(FilterCodeEnum.getDescByCode(soe.getKey()))) {
                                stb.append("and app_code ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add("%" + soe.getValue() + "%");
                            } else if ("in".equals(FilterCodeEnum.getDescByCode(soe.getKey())) || FilterCodeEnum.getDescByCode(soe.getKey()).equals("not in")) {
                                stb.append("and app_code ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ( ");
                                ArrayList<String> value1 = (ArrayList<String>) soe.getValue();
                                for (String s : value1) {
                                    stb.append(" ? ,");
                                    param.add(s);
                                }
                                stb.replace(stb.length() - 1, stb.length(), ")");
                            } else {
                                stb.append("and app_code ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add(soe.getValue());
                            }
                        }
                    }
                    if ("appName".equals(str.getKey())) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            if ("like".equals(FilterCodeEnum.getDescByCode(soe.getKey()))) {
                                stb.append("and app_name ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add("%" + soe.getValue() + "%");
                            } else if ("in".equals(FilterCodeEnum.getDescByCode(soe.getKey())) || FilterCodeEnum.getDescByCode(soe.getKey()).equals("not in")) {
                                stb.append("and app_name ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ( ");
                                ArrayList<String> value1 = (ArrayList<String>) soe.getValue();
                                for (String s : value1) {
                                    stb.append(" ? ,");
                                    param.add(s);
                                }
                                stb.replace(stb.length() - 1, stb.length(), ")");
                            } else {
                                stb.append("and app_name ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add(soe.getValue());
                            }
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
                    if ("dataCode".equals(str.getKey())) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            if (Objects.equals(FilterCodeEnum.getDescByCode(soe.getKey()), "like")) {
                                stb.append("and data_code ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add("%" + soe.getValue() + "%");
                            } else if (Objects.equals(FilterCodeEnum.getDescByCode(soe.getKey()), "in") || FilterCodeEnum.getDescByCode(soe.getKey()).equals("not in")) {
                                stb.append("and data_code ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ( ");
                                ArrayList<String> value1 = (ArrayList<String>) soe.getValue();
                                for (String s : value1) {
                                    stb.append(" ? ,");
                                    param.add(s);
                                }
                                stb.replace(stb.length() - 1, stb.length(), ")");
                            } else {
                                stb.append("and data_code ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add(soe.getValue());
                            }
                        }
                    }
                    if ("color".equals(str.getKey())) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            if (FilterCodeEnum.getDescByCode(soe.getKey()).equals("like")) {
                                stb.append("and color ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add("%" + soe.getValue() + "%");
                            } else if (FilterCodeEnum.getDescByCode(soe.getKey()).equals("in") || FilterCodeEnum.getDescByCode(soe.getKey()).equals("not in")) {
                                stb.append("and color ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ( ");
                                ArrayList<String> value1 = (ArrayList<String>) soe.getValue();
                                for (String s : value1) {
                                    stb.append(" ? ,");
                                    param.add(s);
                                }
                                stb.replace(stb.length() - 1, stb.length(), ")");
                            } else {
                                stb.append("and color ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                param.add(soe.getValue());
                            }
                        }
                    }

                }

            }
        }
    }
}
