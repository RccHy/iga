package com.qtgl.iga.dataFetcher;


import com.alibaba.fastjson.JSON;
import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.bo.MonitorRules;
import com.qtgl.iga.service.MonitorRulesService;
import com.qtgl.iga.utils.CertifiedConnector;
import graphql.schema.DataFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class MonitorRulesDataFetcher {

    public static Logger logger = LoggerFactory.getLogger(MonitorRulesDataFetcher.class);


    @Autowired
    MonitorRulesService monitorRulesService;


    public DataFetcher monitorRules() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            //2。解析查询参数+租户进行  进行查询
            return monitorRulesService.monitorRules(arguments, domain.getId());
        };
    }


    public DataFetcher deleteMonitorRules() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            return monitorRulesService.deleteMonitorRules(arguments, domain.getId());
        };
    }

    public DataFetcher saveMonitorRules() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            MonitorRules monitorRules = JSON.parseObject(JSON.toJSONString(arguments.get("entity")), MonitorRules.class);
            monitorRules.setDomain(domain.getId());
            MonitorRules data = monitorRulesService.saveMonitorRules(monitorRules);
            if (null != data) {
                return data;
            }
            throw new Exception("添加失败");
        };
    }

    public DataFetcher updateMonitorRules() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            MonitorRules monitorRules = JSON.parseObject(JSON.toJSONString(arguments.get("entity")), MonitorRules.class);
            monitorRules.setDomain(domain.getId());
            MonitorRules data = monitorRulesService.updateMonitorRules(monitorRules);
            if (null != data) {
                return data;
            }
            throw new Exception("修改失败");
        };
    }
}
