package com.qtgl.iga.dao.impl;

import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.bo.IncrementalTask;
import com.qtgl.iga.dao.IncrementalTaskDao;
import org.springframework.cglib.beans.BeanMap;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
@Component
public class IncrementalTaskDaoImpl implements IncrementalTaskDao {
    @Resource(name = "jdbcIGA")
    JdbcTemplate jdbcIGA;

    @Override
    public List<IncrementalTask> findByDomainAndType(String domainId, String synType, String upstreamId) {

        String sql = "select id, type, time , create_time as createTime,upstream_type_id as upstreamTypeId," +
                "domain  from incremental_task where domain =? and type=? and upstream_type_id =?  order by create_time desc limit 1";

        List<Map<String, Object>> mapList = jdbcIGA.queryForList(sql, domainId, synType, upstreamId);

        ArrayList<IncrementalTask> incrementalTasks = new ArrayList<>();
        if (null != mapList && mapList.size() > 0) {
            for (Map<String, Object> map : mapList) {
                IncrementalTask incrementalTask = new IncrementalTask();
                BeanMap beanMap = BeanMap.create(incrementalTask);
                beanMap.putAll(map);
                incrementalTasks.add(incrementalTask);
            }
            return incrementalTasks;
        }
        return null;

    }

    @Override
    public void saveAll(List<IncrementalTask> incrementalTasks, DomainInfo domainInfo) {
        String str = "INSERT INTO `incremental_task`(`id`, `type`, `time`, `create_time`, `upstream_type_id`, `domain`) VALUES (?, ?, ?, ?, ?, ?) ";
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        jdbcIGA.batchUpdate(str, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement preparedStatement, int i) throws SQLException {
                preparedStatement.setObject(1, UUID.randomUUID().toString());
                preparedStatement.setObject(2, incrementalTasks.get(i).getType());
                preparedStatement.setObject(3, incrementalTasks.get(i).getTime());
                preparedStatement.setObject(4, timestamp);
                preparedStatement.setObject(5, incrementalTasks.get(i).getUpstreamTypeId());
                preparedStatement.setObject(6, domainInfo.getId());

            }

            @Override
            public int getBatchSize() {
                return incrementalTasks.size();
            }
        });
    }
}
