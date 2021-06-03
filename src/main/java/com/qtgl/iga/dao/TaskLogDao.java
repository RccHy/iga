package com.qtgl.iga.dao;

import com.qtgl.iga.bean.TaskLogConnection;
import com.qtgl.iga.bo.TaskLog;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
@Component
public interface TaskLogDao {


    List<TaskLog> findAll(String domain);

    Integer save(TaskLog taskLog, String domain, String type);


    TaskLogConnection taskLogs(Map<String, Object> arguments, String domainId);

    List<TaskLog> findByStatus(String domain, String status);

    TaskLog markLogs(Map<String, Object> arguments, String domainId);

     TaskLog last(String domain);
}
