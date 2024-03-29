package com.qtgl.iga.dataFetcher;


import com.qtgl.iga.bean.TaskLogConnection;
import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.bo.TaskLog;
import com.qtgl.iga.service.TaskLogService;
import com.qtgl.iga.task.TaskConfig;
import com.qtgl.iga.task.bo.TaskResult;
import com.qtgl.iga.utils.CertifiedConnector;
import com.qtgl.iga.utils.exception.CustomException;
import com.qtgl.iga.utils.exception.GraphqlExceptionUtils;
import graphql.schema.DataFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

@Component
public class TaskLogDataFetcher {

    public static Logger logger = LoggerFactory.getLogger(TaskLogDataFetcher.class);


    @Resource
    TaskLogService taskLogService;

    @Resource
    TaskConfig taskConfig;

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

    /**
     * 修改定时任务日志状态
     *
     * @return
     */
    public DataFetcher invokeTask() {
        return dataFetchingEvn -> {
            TaskResult taskResult = new TaskResult();

            try {
                DomainInfo domainInfo = CertifiedConnector.getDomain();

                Boolean flag = taskLogService.checkTaskStatus(domainInfo.getId());

                if (!flag) {
                    taskResult.setCode("FAILED");
                    taskResult.setMessage("最近三次同步状态均为失败,请处理后再进行同步");
                    return taskResult;
                }
                taskConfig.executeTask(domainInfo);

            } catch (RejectedExecutionException e) {
                e.printStackTrace();
                taskResult.setCode("FAILED");
                taskResult.setMessage("当前线程正在进行数据同步,请稍后再试");
                return taskResult;
            } catch (Exception e) {
                e.printStackTrace();
                taskResult.setCode("FAILED");
                taskResult.setMessage(e.getMessage());
                return taskResult;
            }
            taskResult.setCode("SUCCESS");
            taskResult.setMessage("触发成功");
            return taskResult;
        };
    }

}
