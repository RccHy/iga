package com.qtgl.iga.service.impl;

import com.qtgl.iga.bo.MonitorRules;
import com.qtgl.iga.dao.MonitorRulesDao;
import com.qtgl.iga.service.MonitorRulesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(rollbackFor = Exception.class)
public class MonitorRulesServiceImpl implements MonitorRulesService {

    @Autowired
    MonitorRulesDao monitorRulesDao;
    @Override
    public List<MonitorRules> findAll(String domain, String type) {
        return monitorRulesDao.findAll(domain,type);
    }
}
