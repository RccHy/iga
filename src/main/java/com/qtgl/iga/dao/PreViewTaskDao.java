package com.qtgl.iga.dao;


import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.bo.PreViewTask;

public interface PreViewTaskDao {

    Integer findByTypeAndStatus(String type, String status, DomainInfo domainInfo);

    PreViewTask saveTask(PreViewTask viewTask);

    PreViewTask findByTaskId(Object id, DomainInfo domain);

    Integer makeTaskDone();

    PreViewTask findByTypeAndUpdateTime(String type, String domain);
}
