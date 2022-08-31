package com.qtgl.iga.dao;

import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.bo.IncrementalTask;

import java.util.ArrayList;
import java.util.List;

public interface IncrementalTaskDao {
    List<IncrementalTask> findByDomainAndType(String domainId, String synType, String upstreamId);

    void saveAll(List<IncrementalTask> incrementalTasks, DomainInfo domainInfo);

    void save(IncrementalTask incrementalTasks, DomainInfo domainInfo);

    void update(IncrementalTask incrementalTask);

    void updateBatch(ArrayList<IncrementalTask> incrementalTasks);
}
