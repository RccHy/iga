package com.qtgl.iga.dao.impl;

import com.qtgl.iga.bo.MonitorRules;
import com.qtgl.iga.dao.MonitorRulesDao;
import com.qtgl.iga.utils.MyBeanUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;


@Repository
@Component
public class MonitorRulesDaoImpl implements MonitorRulesDao {


    @Resource(name = "jdbcIGA")
    JdbcTemplate jdbcIGA;

    @Override
    public List<MonitorRules> findAll(String domain, String type) {
        List<MonitorRules> monitorRulesList = new ArrayList<>();
        try {
            String sql = "select id , rules ,type , domain,active,active_time as activeTime,create_time as createTime,update_time as updateTime  from  t_mgr_monitor_rules where domain=? and type=?";
            List<Map<String, Object>> mapList = jdbcIGA.queryForList(sql, domain, type);
            if (null != mapList && mapList.size() > 0) {
                for (Map<String, Object> map : mapList) {
                    MonitorRules montorRules = new MonitorRules();
                    MyBeanUtils.populate(montorRules, map);
                    monitorRulesList.add(montorRules);
                }
                return monitorRulesList;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return monitorRulesList;
    }

    @Override
    public List<MonitorRules> monitorRules(Map<String, Object> arguments, String domainId) {
        List<MonitorRules> monitorRulesList = new ArrayList<>();
        try {
            String sql = "select id , rules ,type , domain,active,active_time as activeTime,create_time as createTime,update_time as updateTime from  t_mgr_monitor_rules where domain=? and type=?";
            List<Map<String, Object>> mapList = jdbcIGA.queryForList(sql, domainId, arguments.get("type"));
            if (null != mapList && mapList.size() > 0) {
                for (Map<String, Object> map : mapList) {
                    MonitorRules montorRules = new MonitorRules();
                    MyBeanUtils.populate(montorRules, map);
                    monitorRulesList.add(montorRules);
                }
                return monitorRulesList;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return monitorRulesList;
    }

    @Override
    public MonitorRules deleteMonitorRules(Map<String, Object> arguments, String domainId) {
        //删除监控规则数据
        String sql = "delete from t_mgr_monitor_rules  where id =? and domain=? ";


        return jdbcIGA.update(sql, preparedStatement -> {
            preparedStatement.setObject(1, arguments.get("id"));
            preparedStatement.setObject(2, domainId);
        }) > 0 ? new MonitorRules() : null;
    }

    @Override
    public MonitorRules saveMonitorRules(MonitorRules monitorRules) {
        String sql = "insert into t_mgr_monitor_rules (id,rules,type,domain,active,active_time,update_time,create_time) values(?,?,?,?,true,now(),now(),now())";
        //生成主键
        String id = UUID.randomUUID().toString().replace("-", "");
        monitorRules.setId(id);

        int update = jdbcIGA.update(sql, preparedStatement -> {
            preparedStatement.setObject(1, id);
            preparedStatement.setObject(2, monitorRules.getRules());
            preparedStatement.setObject(3, monitorRules.getType());
            preparedStatement.setObject(4, monitorRules.getDomain());
        });
        return update > 0 ? monitorRules : null;
    }

    @Override
    public MonitorRules updateMonitorRules(MonitorRules monitorRules) {
        String sql = "update t_mgr_monitor_rules  set rules = ?,type = ?,active =?,update_time =?  ";
        if (monitorRules.getActive()) {
            sql = sql + " active_time = now() ";
        }
        sql = sql + " where id=? and domain=? ";
        int update = jdbcIGA.update(sql, preparedStatement -> {
            preparedStatement.setObject(1, monitorRules.getRules());
            preparedStatement.setObject(2, monitorRules.getType());
            preparedStatement.setObject(3, monitorRules.getActive());
            preparedStatement.setObject(4, System.currentTimeMillis());
            preparedStatement.setObject(5, monitorRules.getId());
            preparedStatement.setObject(6, monitorRules.getDomain());
        });
        return update > 0 ? monitorRules : null;
    }
}
