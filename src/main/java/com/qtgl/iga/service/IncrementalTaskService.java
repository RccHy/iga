package com.qtgl.iga.service;

import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.bo.IncrementalTask;

import java.util.ArrayList;
import java.util.List;

public interface IncrementalTaskService {
    List<IncrementalTask> findByDomainAndType(String domain, String synType, String id);

    void save(IncrementalTask incrementalTask, DomainInfo domain);

    void update(IncrementalTask incrementalTask);

    void saveAll(List<IncrementalTask> incrementalTasks, DomainInfo domain);

    void updateBatch(ArrayList<IncrementalTask> incrementalTasks);
}
