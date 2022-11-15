package com.qtgl.iga;

import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.bo.MonitorRules;
import com.qtgl.iga.service.DomainInfoService;
import com.qtgl.iga.service.MonitorRulesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AutoUpRunner implements CommandLineRunner {


    @Autowired
    DomainInfoService domainInfoService;
    @Autowired
    MonitorRulesService monitorRulesService;
    @Override
    public void run(String... args) throws Exception {
        for (DomainInfo domainInfo : domainInfoService.findAll()) {
            // 【对 2022-11月前版本增加，自动增加对没有设置限制规则的环境，  默认限制 50%删除 上限】
            List<MonitorRules> monitorRules = monitorRulesService.findAll(domainInfo.getId(),null);
            if(null==monitorRules||monitorRules.size()<=0){
                monitorRulesService.initialization(domainInfo.getId());
            }
        }
    }
}
