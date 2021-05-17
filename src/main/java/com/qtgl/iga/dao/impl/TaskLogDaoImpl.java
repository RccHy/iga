package com.qtgl.iga.dao.impl;

import com.qtgl.iga.bean.TaskLogConnection;
import com.qtgl.iga.bean.TaskLogEdge;
import com.qtgl.iga.bo.TaskLog;
import com.qtgl.iga.dao.TaskLogDao;
import com.qtgl.iga.utils.FilterCodeEnum;
import com.qtgl.iga.utils.MyBeanUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.*;

@Repository
@Component
public class TaskLogDaoImpl implements TaskLogDao {

    @Resource(name = "jdbcIGA")
    JdbcTemplate jdbcIGA;

    @Override
    public List<TaskLog> findAll(String domain) {
        try {
            List<TaskLog> taskLogs = new ArrayList<>();
            String sql = "select id,status,dept_no as deptNo,post_no as postNo,person_no as personNo" +
                    ",occupy_no occupyNo ,create_time as createTime , update_time as updateTime from t_mgr_task_log where domain=?  order by create_time desc";
            List<Map<String, Object>> taskLogMap = jdbcIGA.queryForList(sql, domain);
            if (null != taskLogMap && taskLogMap.size() > 0) {
                for (Map<String, Object> map : taskLogMap) {
                    TaskLog taskLog = new TaskLog();
                    MyBeanUtils.populate(taskLog, map);
                    taskLogs.add(taskLog);
                }
                return taskLogs;
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public Integer save(TaskLog taskLog, String domain, String type) {
        int update = 0;
        if("skip".equals(type)){
            String sql = "INSERT INTO t_mgr_task_log (id, status,  create_time,update_time, domain)" +
                    " VALUES (?, ?, now(),now(), ?);";
            update = jdbcIGA.update(sql, taskLog.getId(), "skip",  domain);
        }
        else if ("save".equals(type)) {
            String sql = "INSERT INTO t_mgr_task_log (id, status,  create_time, domain)" +
                    " VALUES ( ?, ?, now(), ?);";
            update = jdbcIGA.update(sql, taskLog.getId(), "doing",  domain);
        } else {
            String sql = "UPDATE t_mgr_task_log " +
                    "SET status = ?, dept_no = ?, post_no = ?, person_no = ?, occupy_no = ?, update_time = now()  WHERE id = ?;";

            update = jdbcIGA.update(sql, taskLog.getStatus(), taskLog.getDeptNo(), taskLog.getPostNo(),
                    taskLog.getPersonNo(), taskLog.getOccupyNo(), taskLog.getId());
        }

        return update;
    }

    @Override
    public TaskLogConnection taskLogs(Map<String, Object> arguments, String domainId) {
        Integer integer = this.allCount(arguments, domainId);
        TaskLogConnection taskLogConnection = new TaskLogConnection();
        taskLogConnection.setTotalCount(integer);
        List<TaskLogEdge> taskLogEdges = new ArrayList<>();
        try {
            String sql = "select id,status,dept_no as deptNo,post_no as postNo,person_no as personNo" +
                    ",occupy_no occupyNo ,create_time as createTime , update_time as updateTime from t_mgr_task_log where domain=? order by create_time desc";
            //拼接sql
            StringBuffer stb = new StringBuffer(sql);
            //存入参数
            List<Object> param = new ArrayList<>();
            param.add(domainId);
            dealData(arguments, stb, param);

            stb.append(" order by create_time desc limit ?,?");
            param.add(null == arguments.get("offset") ? 0 : arguments.get("offset"));
            param.add(null == arguments.get("first") ? 0 : arguments.get("first"));
            System.out.println(stb.toString());
            List<Map<String, Object>> taskLogMap = jdbcIGA.queryForList(stb.toString(), param.toArray());
            for (Map<String, Object> map : taskLogMap) {
                TaskLogEdge taskLogEdge = new TaskLogEdge();
                TaskLog taskLog = new TaskLog();
                MyBeanUtils.populate(taskLog, map);
                taskLogEdge.setNode(taskLog);
                taskLogEdges.add(taskLogEdge);
            }
            taskLogConnection.setEdges(taskLogEdges);
            return taskLogConnection;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public Integer allCount(Map<String, Object> arguments, String domain) {

        String sql = "select count(1) from t_mgr_task_log where domain=? ";
        //拼接sql
        StringBuffer stb = new StringBuffer(sql);
        //存入参数
        List<Object> param = new ArrayList<>();
        param.add(domain);
        dealData(arguments, stb, param);
        stb.append(" order by create_time desc");
        return jdbcIGA.queryForObject(stb.toString(), param.toArray(), Integer.class);
    }


    private void dealData(Map<String, Object> arguments, StringBuffer stb, List<Object> param) {
        Iterator<Map.Entry<String, Object>> it = arguments.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Object> entry = it.next();
//            if ("id".equals(entry.getKey())) {
//                stb.append("and id= ? ");
//                param.add(entry.getValue());
//            }

            if ("filter".equals(entry.getKey())) {
                HashMap<String, Object> map = (HashMap<String, Object>) entry.getValue();
                for (Map.Entry<String, Object> str : map.entrySet()) {
                    if ("status".equals(str.getKey())) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            stb.append("and status ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                            param.add(soe.getValue());
                        }
                    }
                    if ("startTime".equals(str.getKey())) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            //判断是否是区间
                            if (null != soe.getValue() && (!"".equals(soe.getValue()))) {
                                if ("gt".equals(soe.getKey()) || "lt".equals(soe.getKey())
                                        || "gte".equals(soe.getKey()) || "lte".equals(soe.getKey()) || "eq".equals(soe.getKey())) {
                                    stb.append("and create_time ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                    param.add(soe.getValue());

                                }
                            }
                        }
                    }
                    if ("endTime".equals(str.getKey())) {
                        HashMap<String, Object> value = (HashMap<String, Object>) str.getValue();
                        for (Map.Entry<String, Object> soe : value.entrySet()) {
                            //判断是否是区间
                            if (null != soe.getValue() && (!"".equals(soe.getValue()))) {
                                if ("gt".equals(soe.getKey()) || "lt".equals(soe.getKey())
                                        || "gte".equals(soe.getKey()) || "lte".equals(soe.getKey())
                                        || "eq".equals(soe.getKey())) {
                                    stb.append("and create_time ").append(FilterCodeEnum.getDescByCode(soe.getKey())).append(" ? ");
                                    param.add(soe.getValue());

                                }
                            }
                        }
                    }
                }

            }
        }
    }
}
