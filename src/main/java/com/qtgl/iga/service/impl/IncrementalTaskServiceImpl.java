package com.qtgl.iga.service.impl;

import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.bo.IncrementalTask;
import com.qtgl.iga.dao.IncrementalTaskDao;
import com.qtgl.iga.service.IncrementalTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class IncrementalTaskServiceImpl implements IncrementalTaskService {
    @Autowired
    IncrementalTaskDao incrementalTaskDao;

    @Override
    public List<IncrementalTask> findByDomainAndType(String domain, String synType, String upstreamId) {
        return incrementalTaskDao.findByDomainAndType(domain, synType, upstreamId);
    }

    @Override
    public void save(IncrementalTask incrementalTask, DomainInfo domain) {
        incrementalTaskDao.save(incrementalTask, domain);
    }

    @Override
    public void update(IncrementalTask incrementalTask) {
        incrementalTaskDao.update(incrementalTask);
    }

    @Override
    public void saveAll(List<IncrementalTask> incrementalTasks, DomainInfo domain) {
        incrementalTaskDao.saveAll(incrementalTasks, domain);
    }

    @Override
    public void updateBatch(ArrayList<IncrementalTask> incrementalTasks) {
        incrementalTaskDao.updateBatch(incrementalTasks);
    }
}
