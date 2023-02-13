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
import java.util.List;
import java.util.UUID;


@Repository
@Component
public class UserLogDaoImpl implements UserLogDao {


    @Resource(name = "jdbcSSOAPI")
    JdbcTemplate jdbcSSOAPI;


    @Override
    public ArrayList<OccupyDto> saveUserLog(ArrayList<OccupyDto> list, String tenantId) {
        if (null != list && list.size() > 0) {
            // 没有同样时间区间的再新增  EXISTS
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
            // 对 list 分批执行，每批次5000条,不足5000按足量执行
            int batchSize = 5000;
            int batchCount = (list.size() + batchSize - 1) / batchSize;
            for (int i = 0; i < batchCount; i++) {
                int fromIndex = i * batchSize;
                int toIndex = Math.min((i + 1) * batchSize, list.size());
                List<OccupyDto> subList = list.subList(fromIndex, toIndex);
                int[] ints = jdbcSSOAPI.batchUpdate(str, new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                        preparedStatement.setObject(1, UUID.randomUUID().toString().replace("-", ""));
                        preparedStatement.setObject(2, subList.get(i).getOccupyId());
                        preparedStatement.setObject(3, subList.get(i).getValidStartTime());
                        preparedStatement.setObject(4, subList.get(i).getValidEndTime());
                        preparedStatement.setObject(5, subList.get(i).getSource());
                        preparedStatement.setObject(6, "PULL");
                        preparedStatement.setObject(7, tenantId);
                        preparedStatement.setObject(8, subList.get(i).getOccupyId());
                        preparedStatement.setObject(9, subList.get(i).getValidStartTime());
                        preparedStatement.setObject(10, subList.get(i).getValidEndTime());
                    }

                    @Override
                    public int getBatchSize() {
                        return subList.size();
                    }
                });
                contains = Arrays.toString(ints).contains("-1");
            }



            return contains ? null : list;
        }
        return null;
    }
}
