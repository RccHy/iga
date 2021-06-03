package com.qtgl.iga.dao;

import com.qtgl.iga.bo.MonitorRules;

import java.util.List;
import java.util.Map;

public interface MonitorRulesDao {


    List<MonitorRules> findAll(String domain, String type);

    List<MonitorRules> monitorRules(Map<String, Object> arguments, String domainId);

    MonitorRules deleteMonitorRules(Map<String, Object> arguments, String domainId);

    MonitorRules saveMonitorRules(MonitorRules monitorRules);

    MonitorRules updateMonitorRules(MonitorRules monitorRules);
}
