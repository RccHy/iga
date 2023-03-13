package com.qtgl.iga.dao.impl;

import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.bo.PreViewTask;
import com.qtgl.iga.dao.PreViewTaskDao;
import org.springframework.cglib.beans.BeanMap;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;


@Repository
@Component
public class PreViewTaskDaoImpl implements PreViewTaskDao {
    //todo 预览任务(已弃用) 超级租户处理
    @Resource(name = "jdbcIGA")
    JdbcTemplate jdbcIGA;

    @Override
    public Integer findByTypeAndStatus(String type, String status, DomainInfo domain) {
        String sql = " SELECT count(1) from t_mgr_pre_view_task where domain=? and type=? and status=? ";
        //拼接sql
        StringBuffer stb = new StringBuffer(sql);
        //存入参数
        List<Object> param = new ArrayList<>();
        param.add(domain.getId());
        param.add(type);
        param.add(status);

        return jdbcIGA.queryForObject(stb.toString(), param.toArray(), Integer.class);
    }

    @Override
    public PreViewTask saveTask(PreViewTask viewTask) {
        //新增
        String sql;
        if (null == viewTask.getId()) {
            sql = "INSERT INTO `t_mgr_pre_view_task`(`id`, `task_id`, `status`, `create_time`, `type`, `domain`, `update_time`) VALUES (?, ?, ?, ?, ?, ?, ?)";
            //生成主键和时间
            String id = UUID.randomUUID().toString().replace("-", "");
            viewTask.setId(id);
            Timestamp date = new Timestamp(System.currentTimeMillis());
            viewTask.setCreateTime(date);
            viewTask.setUpdateTime(date);
            int update = jdbcIGA.update(sql, preparedStatement -> {
                preparedStatement.setObject(1, id);
                preparedStatement.setObject(2, viewTask.getTaskId());
                preparedStatement.setObject(3, viewTask.getStatus());
                preparedStatement.setObject(4, viewTask.getCreateTime());
                preparedStatement.setObject(5, viewTask.getType());
                preparedStatement.setObject(6, viewTask.getDomain());
                preparedStatement.setObject(7, viewTask.getUpdateTime());
            });
        } else {
            //修改
            sql = "UPDATE `t_mgr_pre_view_task` SET  `status` = ? , `update_time` = ?,statistics=?,reason=? WHERE `id` = ? and domain=? ";
            viewTask.setUpdateTime(new Timestamp(System.currentTimeMillis()));
            int update = jdbcIGA.update(sql, preparedStatement -> {
                preparedStatement.setObject(1, viewTask.getStatus());
                preparedStatement.setObject(2, viewTask.getUpdateTime());
                preparedStatement.setObject(3, viewTask.getStatistics());
                preparedStatement.setObject(4, viewTask.getReason());
                preparedStatement.setObject(5, viewTask.getId());
                preparedStatement.setObject(6, viewTask.getDomain());
            });
        }
        return viewTask;

    }

    @Override
    public PreViewTask findByTaskId(Object id, DomainInfo domain) {
        String sql = " SELECT id,task_id as taskId,status,create_time as createTime,type,domain,update_time as updateTime from t_mgr_pre_view_task where domain=? and task_id =?  ";
        //拼接sql
        StringBuffer stb = new StringBuffer(sql);
        //存入参数
        List<Object> param = new ArrayList<>();
        param.add(domain.getId());
        param.add(id);
        Map<String, Object> stringObjectMap = jdbcIGA.queryForMap(stb.toString(), param.toArray());
        PreViewTask preViewTask = new PreViewTask();
        if (null != stringObjectMap && stringObjectMap.size() > 0) {
            BeanMap beanMap = BeanMap.create(preViewTask);
            beanMap.putAll(stringObjectMap);

            return preViewTask;
        }

        return preViewTask;
    }

    @Override
    public Integer makeTaskDone() {
        //修改
        String sql = "UPDATE `t_mgr_pre_view_task` SET  `status` = 'done' , `update_time` = ? WHERE status!='done' ";
        return jdbcIGA.update(sql, preparedStatement -> {
            preparedStatement.setObject(1, new Timestamp(System.currentTimeMillis()));
        });
    }

    @Override
    public PreViewTask findByTypeAndUpdateTime(String type, String domain) {
        String sql = " SELECT id,task_id as taskId,status,create_time as createTime,type,domain,update_time as updateTime from t_mgr_pre_view_task where domain=? and type =? and status='done'  ORDER BY update_time desc limit 1 ";
        //拼接sql
        StringBuffer stb = new StringBuffer(sql);
        //存入参数
        List<Object> param = new ArrayList<>();
        param.add(domain);
        param.add(type);
        List<Map<String, Object>> maps = jdbcIGA.queryForList(stb.toString(), param.toArray());
        PreViewTask preViewTask = new PreViewTask();
        if (!CollectionUtils.isEmpty(maps)) {
            for (Map<String, Object> map : maps) {
                BeanMap beanMap = BeanMap.create(preViewTask);
                beanMap.putAll(map);

                return preViewTask;
            }

        }

        return preViewTask;
    }

    @Override
    public PreViewTask findLastPreViewTask(String type, String domain) {
        String sql = " SELECT id,task_id as taskId,status,create_time as createTime,type,domain,update_time as updateTime,statistics,reason from t_mgr_pre_view_task where domain=? and type =?  ORDER BY update_time desc limit 1 ";
        //拼接sql
        StringBuffer stb = new StringBuffer(sql);
        //存入参数
        List<Object> param = new ArrayList<>();
        param.add(domain);
        param.add(type);
        List<Map<String, Object>> maps = jdbcIGA.queryForList(stb.toString(), param.toArray());
        PreViewTask preViewTask = new PreViewTask();
        if (!CollectionUtils.isEmpty(maps)) {
            for (Map<String, Object> map : maps) {
                BeanMap beanMap = BeanMap.create(preViewTask);
                beanMap.putAll(map);

                return preViewTask;
            }

        }

        return preViewTask;
    }
}
