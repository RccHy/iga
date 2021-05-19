package com.qtgl.iga.dao.impl;

import com.qtgl.iga.bo.MonitorRules;
import com.qtgl.iga.dao.MonitorRulesDao;
import com.qtgl.iga.utils.MyBeanUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Repository
@Component
public class MonitorRulesDaoImpl implements MonitorRulesDao {


    @Resource(name = "jdbcIGA")
    JdbcTemplate jdbcIGA;

    @Override
    public List<MonitorRules> findAll(String domain, String type) {
        List<MonitorRules> monitorRulesList = new ArrayList<>();
        try {
            String sql = "select * from  t_mgr_monitor_rules where domain=? and type=?";
            List<Map<String, Object>> taskLogMap = jdbcIGA.queryForList(sql, domain, type);
            if (taskLogMap.size() > 0) {
                for (Map<String, Object> map : taskLogMap) {
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


}
