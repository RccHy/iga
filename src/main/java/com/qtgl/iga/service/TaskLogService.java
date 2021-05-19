package com.qtgl.iga.service;

import com.qtgl.iga.bean.TaskLogConnection;
import com.qtgl.iga.bo.TaskLog;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
@Component
public interface TaskLogService {


    List<TaskLog> findAll(String domain);

    Integer save(TaskLog taskLog, String domain, String type);

    TaskLogConnection taskLogs(Map<String, Object> arguments, String id);

    TaskLog markLogs(Map<String, Object> arguments, String id);

    TaskLog last(String domain);
}
