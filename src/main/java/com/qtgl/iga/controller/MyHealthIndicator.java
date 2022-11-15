package com.qtgl.iga.controller;

import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.service.DomainInfoService;
import com.qtgl.iga.service.TaskLogService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component("synchronize task")
public class MyHealthIndicator implements HealthIndicator {

    @Autowired
    DomainInfoService domainInfoService;
    @Autowired
    TaskLogService taskLogService;


    @Override
    public Health health() {
        Map<String,Boolean> details=new HashMap<>();
        List<DomainInfo> all = domainInfoService.findAll();
        for (DomainInfo domainInfo : all) {
            Boolean flag = taskLogService.checkTaskStatus(domainInfo.getId());
            details.put(domainInfo.getDomainName(),flag);
        }
        return Health.up().withDetails(details).build();


    }

}