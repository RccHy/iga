package com.qtgl.iga.service.impl;

import com.qtgl.iga.bean.TaskLogConnection;
import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.bo.TaskLog;
import com.qtgl.iga.dao.DomainInfoDao;
import com.qtgl.iga.dao.TaskLogDao;
import com.qtgl.iga.service.TaskLogService;
import com.qtgl.iga.utils.FileUtil;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;


@Service
@Transactional
@Slf4j
public class TaskLogServiceImpl implements TaskLogService {


    @Autowired
    TaskLogDao taskLogDao;
    @Autowired
    DomainInfoDao domainInfoDao;
    @Autowired
    FileUtil fileUtil;

    @Autowired
    private MeterRegistry meterRegistry;

    @Override
    public List<TaskLog> findAll(String domain) {
        return taskLogDao.findAll(domain);
    }

    @Override
    public Integer save(TaskLog taskLog, String domain, String type) {

        Integer count = taskLogDao.save(taskLog, domain, type);
        DomainInfo domainInfo = domainInfoDao.findById(domain);
        String domainName = domainInfo.getDomainName();
        switch (type) {
            case "update":
                if(taskLog.getStatus().equals("failed")){
                    // 如果相同容器存在 则+1 否则创建
                    if(meterRegistry.find("triple_tasks_total").tags("result", "error").tags("tenant",domainName).counter() == null){
                        Counter.builder("triple_tasks_total").tags(Tags.of("result", "error").and("tenant",domainName)).register(meterRegistry).increment();
                    }else {
                        meterRegistry.get("triple_tasks_total").tags("result", "error").tags("tenant",domainName).counter().increment();
                    }
                    break;
                }
                if(taskLog.getStatus().equals("done")){

                    if(meterRegistry.find("triple_tasks_total").tags("result", "success").tags("tenant",domainName).counter() == null){
                        Counter.builder("triple_tasks_total").tags(Tags.of("result", "success").and("tenant",domainName)).register(meterRegistry).increment();
                    }else {
                        meterRegistry.get("triple_tasks_total").tags("result", "success").tags("tenant",domainName).counter().increment();
                    }
                    break;
                }
                break;
            case "skip":

                if(meterRegistry.find("triple_tasks_total").tags("result", "skip").tags("tenant",domainName).counter() == null){
                    Counter.builder("triple_tasks_total").tags(Tags.of("result", "skip").and("tenant",domainName)).register(meterRegistry).increment();
                }else {
                    meterRegistry.get("triple_tasks_total").tags("result", "skip").tags("tenant",domainName).counter().increment();
                }

                break;
            default:
                break;
        }
        return count;
    }

    @Override
    public TaskLogConnection taskLogs(Map<String, Object> arguments, String domainId) {
        return taskLogDao.taskLogs(arguments, domainId);
    }

    @Override
    public TaskLog markLogs(Map<String, Object> arguments, String domainId) {

        return taskLogDao.markLogs(arguments, domainId);
    }

    @Override
    public TaskLog last(String domain) {
        return taskLogDao.last(domain);
    }

    @Override
    public Map<String, String> downLog(String id, String domainId) {

        //下载
        Map<String, String> map = taskLogDao.downLog(id, domainId);

        return map;
    }

    @Override
    public Boolean checkTaskStatus(String domain) {
        List<TaskLog> taskLogs = taskLogDao.findByDomainAndLimit(domain);
        if (!CollectionUtils.isEmpty(taskLogs)) {
            if (taskLogs.size() != 3) {
                return true;
            } else {
                if (taskLogs.get(0).getStatus().equals("failed") && taskLogs.get(1).getStatus().equals("failed") && taskLogs.get(2).getStatus().equals("failed")) {
                    return false;
                } else {
                    return true;
                }
            }
        }
        return true;
    }

    @Override
    public List<TaskLog> findByStatus(String domain) {
        return taskLogDao.findByStatus(domain);
    }
}
