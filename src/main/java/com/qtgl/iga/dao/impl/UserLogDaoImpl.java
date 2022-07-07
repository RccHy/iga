package com.qtgl.iga.dao.impl;

import com.qtgl.iga.bean.OccupyDto;
import com.qtgl.iga.dao.UserLogDao;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;


@Repository
@Component
public class UserLogDaoImpl implements UserLogDao {


    @Resource(name = "jdbcSSOAPI")
    JdbcTemplate jdbcSSOAPI;


    @Override
    public ArrayList<OccupyDto> saveUserLog(ArrayList<OccupyDto> list, String tenantId) {
        if (null != list && list.size() > 0) {
            //todo 没有同样时间区间的再新增  EXISTS
            String str = "insert into user_log (id,user_id, start_time, end_time, create_time , source, data_source,tenant_id) " +
                    " SELECT" +
                    " ?, " +
                    " ?, " +
                    " ?, " +
                    " ?, " +
                    " now(), " +
                    " ?, " +
                    " ?, " +
                    " ? " +
                    " FROM " +
                    " DUAL " +
                    " WHERE " +
                    " NOT EXISTS ( SELECT * FROM user_log WHERE user_id = ? AND start_time = ? AND end_time = ? ) ";
            boolean contains = false;

            int[] ints = jdbcSSOAPI.batchUpdate(str, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                    preparedStatement.setObject(1, UUID.randomUUID().toString().replace("-", ""));
                    preparedStatement.setObject(2, list.get(i).getOccupyId());
                    preparedStatement.setObject(3, list.get(i).getStartTime());
                    preparedStatement.setObject(4, list.get(i).getEndTime());
                    preparedStatement.setObject(5, list.get(i).getSource());
                    preparedStatement.setObject(6, "PULL");
                    preparedStatement.setObject(7, tenantId);
                    preparedStatement.setObject(8, list.get(i).getOccupyId());
                    preparedStatement.setObject(9, list.get(i).getStartTime());
                    preparedStatement.setObject(10, list.get(i).getEndTime());
                }

                @Override
                public int getBatchSize() {
                    return list.size();
                }
            });
            contains = Arrays.toString(ints).contains("-1");


            return contains ? null : list;
        }
        return null;
    }
}
