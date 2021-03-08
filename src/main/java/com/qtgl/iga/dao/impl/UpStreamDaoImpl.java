package com.qtgl.iga.dao.impl;

import com.qtgl.iga.bo.UpStream;
import com.qtgl.iga.dao.UpStreamDao;
import com.qtgl.iga.dao.mapper.UpStreamRowMapper;
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

    @Override
    public List<UpStream> findAll(Map<String, Object> arguments) {
        String sql = "select  * from t_mgr_upstream where 1 =1";
        StringBuffer stb = new StringBuffer(sql);
        Iterator<Map.Entry<String, Object>> it = arguments.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Object> entry = it.next();
            if (entry.getKey().equals("id")) {
                stb.append("and id =" + entry.getValue());
            }
            if (entry.getKey().equals("filter")) {
                stb.append("and ");
            }
            System.out.println("key = " + entry.getKey() + ", value = " + entry.getValue());
        }
        return jdbcIGA.query(sql, new UpStreamRowMapper());
    }

    @Override
    @Transactional
    public UpStream saveUpStream(UpStream upStream) {
        String sql = "insert into t_mgr_upstream  values(?,?,?,?,?,?,?,?,?)";
        //生成主键和时间
        String id = UUID.randomUUID().toString().replace("-", "");
        upStream.setId(id);
        Date date = new Date();
        upStream.setCreateTime(date);
        return jdbcIGA.update(sql, new PreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement preparedStatement) throws SQLException {
                preparedStatement.setObject(1, id);
                preparedStatement.setObject(2, upStream.getAppCode());
                preparedStatement.setObject(3, upStream.getAppName());
                preparedStatement.setObject(4, upStream.getDataCode());
                preparedStatement.setObject(5, date);
                preparedStatement.setObject(6, upStream.getCreateUser());
                preparedStatement.setObject(7, upStream.getState());
                preparedStatement.setObject(8, upStream.getColor());
                preparedStatement.setObject(9, upStream.getDomain());
            }
        }) > 0 ? upStream : null;
    }

    @Override
    @Transactional
    public UpStream deleteUpStream(Map<String, Object> arguments) throws Exception {
        Object[] objects = new Object[1];
        objects[0] = arguments.get("id");
        List<UpStream> upStreamList = jdbcIGA.queryForList("select  * from t_mgr_upstream  where id =? and domain=?", UpStream.class, arguments.get("id"), arguments.get("domain"));
        if (null == upStreamList || upStreamList.size() > 1) {
            throw new Exception("数据异常，删除失败");
        }
        UpStream upStream = upStreamList.get(0);
        if (upStream.getState() == 0) {
            throw new Exception("上游源已启用,不能进行删除操作");
        }
        //检查源下的类型是否都处于停用 或者删除。



        //删除
        String sql = "delete from t_mgr_upstream  where id =?";
        return jdbcIGA.update(sql, new PreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement preparedStatement) throws SQLException {
                preparedStatement.setObject(1, arguments.get("id"));

            }
        }) > 0 ? upStream : null;


    }

    @Override
    @Transactional
    public UpStream updateUpStream(UpStream upStream) {
        String sql = "update t_mgr_upstream  set app_code = ?,app_name = ?,data_code = ?,create_user = ?,state = ?,color = ?,domain = ? where id=?";
        return jdbcIGA.update(sql, new PreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement preparedStatement) throws SQLException {
                preparedStatement.setObject(1, upStream.getAppCode());
                preparedStatement.setObject(2, upStream.getAppName());
                preparedStatement.setObject(3, upStream.getDataCode());
                preparedStatement.setObject(4, upStream.getCreateUser());
                preparedStatement.setObject(5, upStream.getState());
                preparedStatement.setObject(6, upStream.getColor());
                preparedStatement.setObject(7, upStream.getDomain());
                preparedStatement.setObject(8, upStream.getId());
            }
        }) > 0 ? upStream : null;
    }
}
