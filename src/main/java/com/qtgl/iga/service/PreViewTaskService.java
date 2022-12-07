package com.qtgl.iga.service;


import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.bo.PreViewTask;

public interface PreViewTaskService {

    Integer findByTypeAndStatus(String occupy, String doing, DomainInfo domain);

    PreViewTask saveTask(PreViewTask viewTask);

    PreViewTask findByTaskId(Object id, DomainInfo domain);

    Integer makeTaskDone();
}
