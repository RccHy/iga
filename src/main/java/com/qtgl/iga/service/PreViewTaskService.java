package com.qtgl.iga.service;


import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.bo.PreViewTask;

public interface PreViewTaskService {

    Integer findByTypeAndStatus(String type, String doing, DomainInfo domain);

    PreViewTask saveTask(PreViewTask viewTask);

    PreViewTask findByTaskId(Object id, DomainInfo domain);

    Integer makeTaskDone();

    PreViewTask findByTypeAndUpdateTime(String type, String domain);
    PreViewTask findLastPreViewTask(String type, String domain);

}
