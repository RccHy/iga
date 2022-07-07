package com.qtgl.iga.dao;

import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.bo.IncrementalTask;

import java.util.List;

public interface IncrementalTaskDao {
    List<IncrementalTask> findByDomainAndType(String domainId, String synType,String upstreamId);

    void saveAll(List<IncrementalTask> incrementalTasks, DomainInfo domainInfo);
}
