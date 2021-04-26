package com.qtgl.iga.dao.impl;

import com.alibaba.druid.util.StringUtils;
import com.qtgl.iga.bo.TaskLog;
import com.qtgl.iga.dao.TaskLogDao;
import org.apache.commons.beanutils.BeanUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
@Component
public class TaskLogDaoImpl implements TaskLogDao {

    @Resource(name = "jdbcIGA")
    JdbcTemplate jdbcIGA;

    @Override
    public List<TaskLog> findAll(String domain) {
        try {
            List<TaskLog> taskLogs=new ArrayList<>();
            String sql = "select * from t_mgr_task_log where domain=? order by create_time desc";
            List<Map<String, Object>> taskLogMap = jdbcIGA.queryForList(sql, domain);
            for (Map<String, Object> map : taskLogMap) {
                TaskLog taskLog = new TaskLog();
                BeanUtils.populate(taskLog, map);
                taskLogs.add(taskLog);
            }
            return taskLogs;
        }catch (Exception e){
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public Integer save(TaskLog taskLog,String domain,String type) {
        int update =0;
        if("save".equals(type)){
            String sql="INSERT INTO t_mgr_task_log (id, status, dept_no, post_no, person_no, occupy_no, create_time, domain)" +
                    " VALUES (?, ?, ?, ?, ?, ?, now(), ?);";
            update = jdbcIGA.update(sql, taskLog.getId(), 0, 0, 0, 0, 0, domain);
        }else{
            String sql="UPDATE t_mgr_task_log " +
                    "SET status = ?, dept_no = ?, post_no = ?, person_no = ?, occupy_no = ?, update_time = now()  WHERE id = ?;";

            update = jdbcIGA.update(sql, taskLog.getStatus(),taskLog.getDeptNo(), taskLog.getPostNo(),
                    taskLog.getPersonNo(), taskLog.getOccupyNo(),taskLog.getId() );
        }

        return update;
    }
}
