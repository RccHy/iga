package com.qtgl.iga.dao.impl;

import com.qtgl.iga.bo.UpStream;
import com.qtgl.iga.bo.UpStreamType;
import com.qtgl.iga.dao.UpStreamDao;
import com.qtgl.iga.dao.mapper.UpStreamRowMapper;
import com.qtgl.iga.utils.FilterCodeEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;


@Repository
public class UpStreamDaoImpl implements UpStreamDao {


    @Resource(name = "jdbcIGA")
    JdbcTemplate jdbcIGA;

    @Autowired
    UpStreamTypeDaoImpl upStreamTypeDao;

    @Override
    public List<UpStream> findAll(Map<String, Object> arguments, String domain) {
        String sql = "select  * from t_mgr_upstream where 1 = 1 ";
        //拼接sql
        StringBuffer stb = new StringBuffer(sql);
        //存入参数
        List<Object> param = new ArrayList<>();

        dealData(arguments, stb, param);
//        getChild(arguments,param,stb);
        System.out.println(stb.toString());
        return jdbcIGA.query(stb.toString(), param.toArray(), new UpStreamRowMapper());
    }


    @Override
    @Transactional
    public UpStream saveUpStream(UpStream upStream, String domain) {
        String sql = "insert into t_mgr_upstream  values(?,?,?,?,?,?,?,?,?,?,?)";
        //生成主键和时间
        String id = UUID.randomUUID().toString().replace("-", "");
        upStream.setId(id);
        Date date = new Date();
        upStream.setCreateTime(date);
        int update = jdbcIGA.update(sql, new PreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement preparedStatement) throws SQLException {
                preparedStatement.setObject(1, id);
                preparedStatement.setObject(2, upStream.getAppCode());
                preparedStatement.setObject(3, upStream.getAppName());
                preparedStatement.setObject(4, upStream.getDataCode());
                preparedStatement.setObject(5, date);
                preparedStatement.setObject(6, upStream.getCreateUser());
                preparedStatement.setObject(7, upStream.getActive());
                preparedStatement.setObject(8, upStream.getColor());
                preparedStatement.setObject(9, domain);
                preparedStatement.setObject(10, date);
                preparedStatement.setObject(11, date);

            }
        });
        return update > 0 ? upStream : null;
    }

    @Override
    @Transactional
    public UpStream deleteUpStream(Map<String, Object> arguments, String domain) throws Exception {
        Object[] objects = new Object[2];
        objects[0] = arguments.get("id");
        objects[1] = domain;
        List<UpStream> upStreamList = jdbcIGA.query("select  * from t_mgr_upstream  where id =? and domain=?", objects, new UpStreamRowMapper());
        if (null == upStreamList || upStreamList.size() > 1 || upStreamList.size() == 0) {
            throw new Exception("数据异常，删除失败");
        }
        UpStream upStream = upStreamList.get(0);
        if (upStream.getActive()) {
            throw new Exception("上游源已启用,不能进行删除操作");
        }
        //检查源下的类型是否都处于停用 或者删除。
        List<UpStreamType> byUpStreamId = upStreamTypeDao.findByUpStreamId(upStream.getId());
        if (null == byUpStreamId || byUpStreamId.size() != 0) {
            throw new Exception("数据异常，删除失败");
        }

        //删除上游源数据
        String sql = "delete from t_mgr_upstream  where id =?";
        int id = jdbcIGA.update(sql, new PreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement preparedStatement) throws SQLException {
                preparedStatement.setObject(1, arguments.get("id"));

            }
        });
        //删除上游源数据类型
        int i = upStreamTypeDao.deleteByUpStreamId(upStream.getId());

        return (id > 0 && i > 0) ? upStream : null;


    }

    @Override
    @Transactional
    public UpStream updateUpStream(UpStream upStream) {
        String sql = "update t_mgr_upstream  set app_code = ?,app_name = ?,data_code = ?,create_user = ?,active = ?," +
                "color = ?,domain = ?,active_time = ?,update_time= ?  where id=?";
        Date date = new Date();
        return jdbcIGA.update(sql, new PreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement preparedStatement) throws SQLException {
                preparedStatement.setObject(1, upStream.getAppCode());
                preparedStatement.setObject(2, upStream.getAppName());
                preparedStatement.setObject(3, upStream.getDataCode());
                preparedStatement.setObject(4, upStream.getCreateUser());
                preparedStatement.setObject(5, upStream.getActive());
                preparedStatement.setObject(6, upStream.getColor());
                preparedStatement.setObject(7, upStream.getDomain());
                preparedStatement.setObject(8, date);
                preparedStatement.setObject(9, date);
                preparedStatement.setObject(10, upStream.getId());
            }
        }) > 0 ? upStream : null;
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
                    if (str.getKey().equals("appCode")) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            stb.append(" and app_code " + FilterCodeEnum.getDescByCode(soe.getKey()) + " ? ");
                            param.add(soe.getValue());
                        }
                    }
                    if (str.getKey().equals("appName")) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            stb.append("and app_name " + FilterCodeEnum.getDescByCode(soe.getKey()) + " ? ");
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
                    if (str.getKey().equals("dataCode")) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            stb.append("and data_code " + FilterCodeEnum.getDescByCode(soe.getKey()) + " ? ");
                            param.add(soe.getValue());
                        }
                    }
                    if (str.getKey().equals("color")) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            stb.append("and color " + FilterCodeEnum.getDescByCode(soe.getKey()) + " ? ");
                            param.add(soe.getValue());
                        }
                    }

                }

            }
            System.out.println("key = " + entry.getKey() + ", value = " + entry.getValue());
        }
    }
}
