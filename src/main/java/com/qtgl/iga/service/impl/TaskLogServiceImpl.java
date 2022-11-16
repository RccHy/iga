package com.qtgl.iga.service.impl;

import com.qtgl.iga.bean.TaskLogConnection;
import com.qtgl.iga.bo.TaskLog;
import com.qtgl.iga.dao.TaskLogDao;
import com.qtgl.iga.service.TaskLogService;
import com.qtgl.iga.utils.FileUtil;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;
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
    FileUtil fileUtil;

    @Autowired
    private MeterRegistry meterRegistry;

    @Override
    public List<TaskLog> findAll(String domain) {
        return taskLogDao.findAll(domain);
    }

    @Override
    public Integer save(TaskLog taskLog, String domain, String type) {
        //if (StringUtils.isNotBlank(taskLog.getData())) {
        //    try {
        //        String utf8 = fileUtil.putFile(taskLog.getData().getBytes("UTF8"), LocalDateTime.now() + ".txt", domain);
        //        taskLog.setData(utf8);
        //    } catch (Exception e) {
        //        log.error("上传文件失败:{}", e);
        //        e.printStackTrace();
        //    }
        //}

        if(taskLog.getStatus().equals("failed")){
            meterRegistry.gauge("iga_sync_error_task", Tags.of("dept", taskLog.getDeptNo(), "post", taskLog.getPersonNo(),"user",taskLog.getPersonNo(),"occupy",taskLog.getOccupyNo()), -1);

        }

        return taskLogDao.save(taskLog, domain, type);
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
}
