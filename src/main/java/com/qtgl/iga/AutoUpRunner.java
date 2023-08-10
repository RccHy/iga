package com.qtgl.iga;

import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.bo.MonitorRules;
import com.qtgl.iga.service.DomainInfoService;
import com.qtgl.iga.service.MonitorRulesService;
import com.qtgl.iga.utils.webSocket.SubWebSocket;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AutoUpRunner implements CommandLineRunner {

    public static String superDomainId= "";
    @Autowired
    DomainInfoService domainInfoService;
    @Autowired
    MonitorRulesService monitorRulesService;

    @Autowired
    SubWebSocket subWebSocket;


    @Override
    public void run(String... args)  {
        List<DomainInfo> all = domainInfoService.findAll();
        if (null != all&&all.size()>0) {
            for (DomainInfo domainInfo : all) {
                // 【对 2022-11月前版本增加，自动增加对没有设置限制规则的环境，  默认限制 50%删除 上限】
                List<MonitorRules> monitorRules = monitorRulesService.findAll(domainInfo.getId(), null);
                if (null == monitorRules || monitorRules.size() <= 0) {
                    monitorRulesService.initialization(domainInfo.getId());
                }
                try {
                    subWebSocket.listening(domainInfo);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        //获取超级租户id 放入全局变量
        DomainInfo localhost = domainInfoService.getLocalhost();
        superDomainId=localhost.getId();


    }
}
