package com.qtgl.iga.service.impl;


import com.qtgl.iga.bo.MonitorRules;
import com.qtgl.iga.dao.MonitorRulesDao;
import com.qtgl.iga.service.MonitorRulesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@Transactional(rollbackFor = Exception.class)

public class MonitorRulesServiceImpl implements MonitorRulesService {

    @Autowired
    MonitorRulesDao monitorRulesDao;

    @Override
    public List<MonitorRules> monitorRules(Map<String, Object> arguments, String domainId) {
        return monitorRulesDao.monitorRules(arguments, domainId);
    }

    @Override
    public MonitorRules deleteMonitorRules(Map<String, Object> arguments, String domainId) {
        return monitorRulesDao.deleteMonitorRules(arguments, domainId);
    }

    @Override
    public MonitorRules saveMonitorRules(MonitorRules monitorRules) {
        return monitorRulesDao.saveMonitorRules(monitorRules);
    }

    @Override
    public MonitorRules updateMonitorRules(MonitorRules monitorRules) {
        return monitorRulesDao.updateMonitorRules(monitorRules);
    }

    @Override
    public List<MonitorRules> findAll(String domain, String type) {
        return monitorRulesDao.findAll(domain, type);
    }

    @Override
    public void initialization(String domain) {
          monitorRulesDao.initialization(domain);

    }


}
