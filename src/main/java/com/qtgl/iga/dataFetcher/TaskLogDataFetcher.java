package com.qtgl.iga.dataFetcher;


import com.qtgl.iga.bean.TaskLogConnection;
import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.bo.TaskLog;
import com.qtgl.iga.service.TaskLogService;
import com.qtgl.iga.utils.CertifiedConnector;
import com.qtgl.iga.utils.exception.GraphqlExceptionUtils;
import com.qtgl.iga.utils.exception.CustomException;
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

    /**
     * 查询定时任务日志
     *
     * @return
     */
    public DataFetcher taskLogs() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            //2。解析查询参数+租户进行  进行查询
            try {
                TaskLogConnection taskLogConnection = taskLogService.taskLogs(arguments, domain.getId());
                return taskLogConnection;
            } catch (CustomException e) {
                e.printStackTrace();
                logger.error(domain.getDomainName() + e.getMessage());

                return GraphqlExceptionUtils.getObject("查询定时任务日志失败", e);
            }
        };
    }

    /**
     * 修改定时任务日志状态
     *
     * @return
     */
    public DataFetcher markLogs() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            //2。解析查询参数+租户进行  进行查询
            try {
                TaskLog taskLog = taskLogService.markLogs(arguments, domain.getId());
                return taskLog;
            } catch (CustomException e) {
                e.printStackTrace();
                logger.error(domain.getDomainName() + e.getMessage());

                return GraphqlExceptionUtils.getObject("修改定时任务日志失败", e);
            }
        };
    }

}
