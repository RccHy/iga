package com.qtgl.iga.dao.impl;

import com.alibaba.fastjson.JSONObject;
import com.qtgl.iga.bean.TaskLogConnection;
import com.qtgl.iga.bean.TaskLogEdge;
import com.qtgl.iga.bo.TaskLog;
import com.qtgl.iga.dao.TaskLogDao;
import com.qtgl.iga.utils.FilterCodeEnum;
import com.qtgl.iga.utils.MyBeanUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@Component
@Slf4j
public class TaskLogDaoImpl implements TaskLogDao {

    @Resource(name = "jdbcIGA")
    JdbcTemplate jdbcIGA;

    @Override
    public List<TaskLog> findAll(String domain) {
        try {
            List<TaskLog> taskLogs = new ArrayList<>();
            String sql = "select id,status,dept_no as deptNo,post_no as postNo,person_no as personNo" +
                    ",occupy_no occupyNo ,create_time as createTime , update_time as updateTime ,reason,data,syn_way as synWay  from t_mgr_task_log where domain=?  order by create_time desc";
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
        if ("skip".equals(type)) {
            String sql = "INSERT INTO t_mgr_task_log (id, status,  create_time,update_time, reason,domain,data,syn_way)" +
                    " VALUES (?, ?, now(),now(), ?,?,?,?);";
            update = jdbcIGA.update(sql, taskLog.getId(), "skip", taskLog.getReason(), domain, taskLog.getData(), taskLog.getSynWay());
        } else if ("skip-error".equals(type)) {
            String sql = "INSERT INTO t_mgr_task_log (id, status,  create_time,update_time, reason,domain,data,syn_way)" +
                    " VALUES (?, ?, now(),now(), ?,?,?,?);";
            update = jdbcIGA.update(sql, taskLog.getId(), "failed", taskLog.getReason(), domain, taskLog.getData(), taskLog.getSynWay());
        } else if ("save".equals(type)) {
            String sql = "INSERT INTO t_mgr_task_log (id, status,  create_time, domain,data,syn_way )" +
                    " VALUES ( ?, ?, now(), ? ,?,? );";
            update = jdbcIGA.update(sql, taskLog.getId(), "doing", domain, taskLog.getData(), taskLog.getSynWay());
        } else {
            String sql = "UPDATE t_mgr_task_log " +
                    "SET reason=?,status = ?, dept_no = ?, post_no = ?, person_no = ?, occupy_no = ?, update_time = now(),data =?,syn_way=?  WHERE id = ?;";

            update = jdbcIGA.update(sql, taskLog.getReason(), taskLog.getStatus(), taskLog.getDeptNo(), taskLog.getPostNo(),
                    taskLog.getPersonNo(), taskLog.getOccupyNo(), taskLog.getData(), taskLog.getSynWay(), taskLog.getId());
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
                    ",occupy_no occupyNo ,create_time as createTime , update_time as updateTime,reason,data,syn_way as synWay from t_mgr_task_log where domain=? ";
            //拼接sql
            StringBuffer stb = new StringBuffer(sql);
            //存入参数
            List<Object> param = new ArrayList<>();
            param.add(domainId);
            dealData(arguments, stb, param);

            stb.append(" order by create_time desc ");
            //查询所有
            if (null != arguments.get("offset") && null != arguments.get("first")) {

                stb.append(" limit ?,? ");
                param.add(null == arguments.get("offset") ? 0 : arguments.get("offset"));
                param.add(null == arguments.get("first") ? 0 : arguments.get("first"));
            }

            System.out.println(stb.toString());
            List<Map<String, Object>> taskLogMap = jdbcIGA.queryForList(stb.toString(), param.toArray());
            for (Map<String, Object> map : taskLogMap) {
                TaskLogEdge taskLogEdge = new TaskLogEdge();
                TaskLog taskLog = new TaskLog();
                MyBeanUtils.populate(taskLog, map);
                String data = taskLog.getData();
                try {
                    if (StringUtils.isNotBlank(data) && 0 == JSONObject.parseObject(data).getInteger("errno")) {
                        JSONObject object = JSONObject.parseObject(data);
                        String uri = object.getJSONArray("entities").getJSONObject(0).getString("name");
                        taskLog.setData(uri);
                    }
                } catch (Exception e) {

                }

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

    @Override
    public List<TaskLog> findByStatus(String domain) {
        try {
            List<TaskLog> taskLogs = new ArrayList<>();
            String sql = "select id,status,dept_no as deptNo,post_no as postNo,person_no as personNo" +
                    ",occupy_no occupyNo ,create_time as createTime , update_time as updateTime,reason,data,syn_way as synWay  from t_mgr_task_log where domain=?  order by create_time desc limit 1 ";
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
    public TaskLog markLogs(Map<String, Object> arguments, String domainId) {
        String sql = "update t_mgr_task_log  set status = ? where id=? and domain=?";
        int update = jdbcIGA.update(sql, preparedStatement -> {
            preparedStatement.setObject(1, arguments.get("status"));
            preparedStatement.setObject(2, arguments.get("id"));
            preparedStatement.setObject(3, domainId);
        });
        return update > 0 ? new TaskLog() : null;
    }

    @Override
    public TaskLog last(String domain) {
        String sql = "select id,reason,status,data,syn_way as synWay from t_mgr_task_log where domain=? order by create_time desc limit 1";
        List<TaskLog> taskLogs = new ArrayList<>();
        try {
            List<Map<String, Object>> taskLogMap = jdbcIGA.queryForList(sql, domain);
            if (taskLogMap.size() > 0) {
                for (Map<String, Object> map : taskLogMap) {
                    TaskLog taskLog = new TaskLog();
                    MyBeanUtils.populate(taskLog, map);
                    taskLogs.add(taskLog);
                }
                return taskLogs.get(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public Map<String, String> downLog(String id, String domainId) {
        String sql = "select data from t_mgr_task_log where domain=? and id =? ";
        Map<String, Object> url = jdbcIGA.queryForMap(sql, domainId, id);
        String data = null == url.get("data") ? null : String.valueOf(url.get("data"));
        ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>();
        if (StringUtils.isNotBlank(data) && 0 == JSONObject.parseObject(data).getInteger("errno")) {
            JSONObject object = JSONObject.parseObject(data);
            String uri = object.getJSONArray("entities").getJSONObject(0).getString("uri");
            String fileName = object.getJSONArray("entities").getJSONObject(0).getString("name");
            map.put("uri", uri);
            map.put("fileName", fileName);
            return map;
        } else {
            return null;
        }

    }

    @Override
    public List<TaskLog> findByDomainAndLimit(String domain) {
        try {
            List<TaskLog> taskLogs = new ArrayList<>();
            String sql = "select id,status,dept_no as deptNo,post_no as postNo,person_no as personNo" +
                    ",occupy_no occupyNo ,create_time as createTime , update_time as updateTime ,reason,data,syn_way as synWay  from t_mgr_task_log where domain=?  order by create_time desc limit 3 ";
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
