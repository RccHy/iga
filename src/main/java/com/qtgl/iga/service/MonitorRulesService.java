package com.qtgl.iga.service;


import com.qtgl.iga.bo.MonitorRules;

import java.util.List;
import java.util.Map;

public interface MonitorRulesService {

    List<MonitorRules> monitorRules(Map<String, Object> arguments, String id);

    MonitorRules deleteMonitorRules(Map<String, Object> arguments, String id);

    MonitorRules saveMonitorRules(MonitorRules monitorRules);

    MonitorRules updateMonitorRules(MonitorRules monitorRules);

    List<MonitorRules> findAll(String domain, String type);

    void initialization(String domain);
}
