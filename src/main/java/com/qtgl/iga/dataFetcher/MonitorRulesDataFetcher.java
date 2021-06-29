package com.qtgl.iga.dataFetcher;


import com.alibaba.fastjson.JSON;
import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.bo.MonitorRules;
import com.qtgl.iga.service.MonitorRulesService;
import com.qtgl.iga.utils.CertifiedConnector;
import com.qtgl.iga.utils.exception.GraphqlExceptionUtils;
import com.qtgl.iga.utils.enumerate.ResultCode;
import com.qtgl.iga.utils.exception.CustomException;
import graphql.schema.DataFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class MonitorRulesDataFetcher {

    public static Logger logger = LoggerFactory.getLogger(MonitorRulesDataFetcher.class);


    @Autowired
    MonitorRulesService monitorRulesService;

    /**
     * 查询监控规则
     *
     * @return
     */
    public DataFetcher monitorRules() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            //2。解析查询参数+租户进行  进行查询
            try {
                List<MonitorRules> monitorRules = monitorRulesService.monitorRules(arguments, domain.getId());
                return monitorRules;
            } catch (CustomException e) {
                e.printStackTrace();
                logger.error(domain.getDomainName() + e.getMessage());

                return GraphqlExceptionUtils.getObject("查询监控规则失败", e);
            }
        };
    }

    /**
     * 删除监控规则
     *
     * @return
     */
    public DataFetcher deleteMonitorRules() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            try {
                MonitorRules monitorRules = monitorRulesService.deleteMonitorRules(arguments, domain.getId());
                return monitorRules;
            } catch (CustomException e) {
                e.printStackTrace();
                logger.error(domain.getDomainName() + e.getMessage());

                return GraphqlExceptionUtils.getObject("删除监控规则失败", e);
            }
        };
    }

    /**
     * 添加监控规则
     *
     * @return
     */
    public DataFetcher saveMonitorRules() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            MonitorRules monitorRules = JSON.parseObject(JSON.toJSONString(arguments.get("entity")), MonitorRules.class);
            monitorRules.setDomain(domain.getId());
            try {
                MonitorRules data = monitorRulesService.saveMonitorRules(monitorRules);
                if (null != data) {
                    return data;
                }
                throw new CustomException(ResultCode.FAILED, "添加监控规则失败");
            } catch (CustomException e) {
                e.printStackTrace();
                logger.error(domain.getDomainName() + e.getMessage());

                return GraphqlExceptionUtils.getObject("添加监控规则失败", e);
            }
        };
    }

    /**
     * 修改监控规则
     *
     * @return
     */
    public DataFetcher updateMonitorRules() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            MonitorRules monitorRules = JSON.parseObject(JSON.toJSONString(arguments.get("entity")), MonitorRules.class);
            monitorRules.setDomain(domain.getId());
            try {
                MonitorRules data = monitorRulesService.updateMonitorRules(monitorRules);
                if (null != data) {
                    return data;
                }
                throw new CustomException(ResultCode.FAILED, "修改监控规则失败");
            } catch (CustomException e) {
                e.printStackTrace();
                logger.error(domain.getDomainName() + e.getMessage());

                return GraphqlExceptionUtils.getObject("修改监控规则失败", e);
            }
        };
    }
}
