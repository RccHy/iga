package com.qtgl.iga.dao.impl;


import com.qtgl.iga.bo.Upstream;
import com.qtgl.iga.bo.UpstreamType;
import com.qtgl.iga.dao.UpstreamDao;
import com.qtgl.iga.dao.UpstreamTypeDao;
import com.qtgl.iga.utils.FilterCodeEnum;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    UpstreamTypeDao upstreamTypeDao;

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
//        getChild(arguments,param,stb);
        System.out.println(stb.toString());
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
            throw new Exception("code 或 name 不能重复,添加组织机构树类别失败");
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
    public Upstream deleteUpstream(Map<String, Object> arguments, String domain) throws Exception {
        Object[] objects = new Object[2];
        objects[0] = arguments.get("id");
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
        if (null == upstreamList || upstreamList.size() > 1 || upstreamList.size() == 0) {
            throw new Exception("数据异常，删除失败");
        }
        Upstream upstream = upstreamList.get(0);
        if (upstream.getActive()) {
            throw new Exception("上游源已启用,不能进行删除操作");
        }
        //检查源下的类型是否都处于停用 或者删除。
        List<UpstreamType> byUpstreamId = upstreamTypeDao.findByUpstreamId(upstream.getId());
        if (null != byUpstreamId && byUpstreamId.size() != 0) {
            throw new Exception("数据异常，删除失败");
        }

        //删除上游源数据
        String sql = "delete from t_mgr_upstream  where id =?";
        int id = jdbcIGA.update(sql, preparedStatement -> preparedStatement.setObject(1, arguments.get("id")));
        //删除上游源数据类型
        upstreamTypeDao.deleteByUpstreamId(upstream.getId());

        return id > 0 ? upstream : null;


    }

    @Override
    @Transactional
    public Upstream updateUpstream(Upstream upstream) throws Exception {
        //判重
        Object[] param = new Object[]{upstream.getAppCode(), upstream.getAppName(), upstream.getId()};
        List<Map<String, Object>> mapList = jdbcIGA.queryForList("select  * from t_mgr_upstream where (app_code = ? or app_name = ?) and id != ?  ", param);
        if (null != mapList && mapList.size() > 0) {
            throw new Exception("code 或 name 不能重复,修改组织机构类别树失败");
        }
        if (!upstream.getActive()) {
            //判断类型是否都未启用
            List<UpstreamType> byUpstreamId = upstreamTypeDao.findByUpstreamId(upstream.getId());
            if (null != byUpstreamId && byUpstreamId.size() != 0) {
                throw new Exception("上游源禁用失败,请检查相关上游源类型状态");
            }
        }


        String sql = "update t_mgr_upstream  set app_code = ?,app_name = ?,data_code = ?,create_user = ?,active = ?," +
                "color = ?,domain = ?,active_time = ?,update_time= ?  where id=?";
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
            if (entry.getKey().equals("id")) {
                stb.append("and id= ? ");
                param.add(entry.getValue());
            }

            if (entry.getKey().equals("filter")) {
                HashMap<String, Object> map = (HashMap<String, Object>) entry.getValue();
                for (Map.Entry<String, Object> str : map.entrySet()) {
                    if (str.getKey().equals("appCode")) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            if (FilterCodeEnum.getDescByCode(soe.getKey()).equals("like")) {
                                stb.append("and app_code " + FilterCodeEnum.getDescByCode(soe.getKey()) + " ? ");
                                param.add("%" + soe.getValue() + "%");
                            } else if (FilterCodeEnum.getDescByCode(soe.getKey()).equals("in") || FilterCodeEnum.getDescByCode(soe.getKey()).equals("not in")) {
                                stb.append("and app_code " + FilterCodeEnum.getDescByCode(soe.getKey()) + " ( ");
                                ArrayList<String> value1 = (ArrayList<String>) soe.getValue();
                                for (String s : value1) {
                                    stb.append(" ? ,");
                                    param.add(s);
                                }
                                stb.replace(stb.length() - 1, stb.length(), ")");
                            } else {
                                stb.append("and app_code " + FilterCodeEnum.getDescByCode(soe.getKey()) + " ? ");
                                param.add(soe.getValue());
                            }
                        }
                    }
                    if (str.getKey().equals("appName")) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            if (FilterCodeEnum.getDescByCode(soe.getKey()).equals("like")) {
                                stb.append("and app_name " + FilterCodeEnum.getDescByCode(soe.getKey()) + " ? ");
                                param.add("%" + soe.getValue() + "%");
                            } else if (FilterCodeEnum.getDescByCode(soe.getKey()).equals("in") || FilterCodeEnum.getDescByCode(soe.getKey()).equals("not in")) {
                                stb.append("and app_name " + FilterCodeEnum.getDescByCode(soe.getKey()) + " ( ");
                                ArrayList<String> value1 = (ArrayList<String>) soe.getValue();
                                for (String s : value1) {
                                    stb.append(" ? ,");
                                    param.add(s);
                                }
                                stb.replace(stb.length() - 1, stb.length(), ")");
                            } else {
                                stb.append("and app_name " + FilterCodeEnum.getDescByCode(soe.getKey()) + " ? ");
                                param.add(soe.getValue());
                            }
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
                    if (str.getKey().equals("dataCode")) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            if (FilterCodeEnum.getDescByCode(soe.getKey()).equals("like")) {
                                stb.append("and data_code " + FilterCodeEnum.getDescByCode(soe.getKey()) + " ? ");
                                param.add("%" + soe.getValue() + "%");
                            } else if (FilterCodeEnum.getDescByCode(soe.getKey()).equals("in") || FilterCodeEnum.getDescByCode(soe.getKey()).equals("not in")) {
                                stb.append("and data_code " + FilterCodeEnum.getDescByCode(soe.getKey()) + " ( ");
                                ArrayList<String> value1 = (ArrayList<String>) soe.getValue();
                                for (String s : value1) {
                                    stb.append(" ? ,");
                                    param.add(s);
                                }
                                stb.replace(stb.length() - 1, stb.length(), ")");
                            } else {
                                stb.append("and data_code " + FilterCodeEnum.getDescByCode(soe.getKey()) + " ? ");
                                param.add(soe.getValue());
                            }
                        }
                    }
                    if (str.getKey().equals("color")) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            if (FilterCodeEnum.getDescByCode(soe.getKey()).equals("like")) {
                                stb.append("and color " + FilterCodeEnum.getDescByCode(soe.getKey()) + " ? ");
                                param.add("%" + soe.getValue() + "%");
                            } else if (FilterCodeEnum.getDescByCode(soe.getKey()).equals("in") || FilterCodeEnum.getDescByCode(soe.getKey()).equals("not in")) {
                                stb.append("and color " + FilterCodeEnum.getDescByCode(soe.getKey()) + " ( ");
                                ArrayList<String> value1 = (ArrayList<String>) soe.getValue();
                                for (String s : value1) {
                                    stb.append(" ? ,");
                                    param.add(s);
                                }
                                stb.replace(stb.length() - 1, stb.length(), ")");
                            } else {
                                stb.append("and color " + FilterCodeEnum.getDescByCode(soe.getKey()) + " ? ");
                                param.add(soe.getValue());
                            }
                        }
                    }

                }

            }
//            System.out.println("key = " + entry.getKey() + ", value = " + entry.getValue());
        }
    }
}
