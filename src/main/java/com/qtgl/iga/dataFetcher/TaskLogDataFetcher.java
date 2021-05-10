package com.qtgl.iga.dataFetcher;


import com.alibaba.fastjson.JSON;
import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.bo.NodeRulesRange;
import com.qtgl.iga.service.NodeRulesRangeService;
import com.qtgl.iga.service.TaskLogService;
import com.qtgl.iga.utils.CertifiedConnector;
import graphql.schema.DataFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class TaskLogDataFetcher {

    public static Logger logger = LoggerFactory.getLogger(TaskLogDataFetcher.class);


    @Autowired
    TaskLogService taskLogService;


    public DataFetcher taskLogs() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            //2。解析查询参数+租户进行  进行查询
            return taskLogService.taskLogs(arguments, domain.getId());
        };
    }
}
