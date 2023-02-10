package com.qtgl.iga.service.impl;

import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.bo.PreViewTask;
import com.qtgl.iga.dao.PreViewTaskDao;
import com.qtgl.iga.service.PreViewTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PreViewTaskServiceImpl implements PreViewTaskService {
    @Autowired
    PreViewTaskDao preViewTaskDao;

    @Override
    public Integer findByTypeAndStatus(String type, String doing, DomainInfo domain) {
        return preViewTaskDao.findByTypeAndStatus(type, doing, domain);
    }

    @Override
    public PreViewTask saveTask(PreViewTask viewTask) {
        return preViewTaskDao.saveTask(viewTask);
    }

    @Override
    public PreViewTask findByTaskId(Object id, DomainInfo domain) {
        return preViewTaskDao.findByTaskId(id, domain);
    }

    @Override
    public Integer makeTaskDone() {
        return preViewTaskDao.makeTaskDone();
    }

    @Override
    public PreViewTask findByTypeAndUpdateTime(String type, String domain) {
        return preViewTaskDao.findByTypeAndUpdateTime(type, domain);
    }

    @Override
    public PreViewTask findLastPreViewTask(String type, String domain) {
        return preViewTaskDao.findLastPreViewTask(type, domain);
    }
}
