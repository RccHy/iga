package com.qtgl.iga.service.impl;

import com.qtgl.iga.bo.TaskLog;
import com.qtgl.iga.dao.TaskLogDao;
import com.qtgl.iga.service.TaskLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
@Transactional
public class TaskLogServiceImpl implements TaskLogService {


    @Autowired
    TaskLogDao taskLogDao;

    @Override
    public List<TaskLog> findAll(String domain) {
        return taskLogDao.findAll(domain);
    }

    @Override
    public Integer save(TaskLog taskLog, String domain, String type) {
        return taskLogDao.save(taskLog,domain,type);
    }
}
