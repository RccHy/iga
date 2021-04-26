package com.qtgl.iga.dao;

import com.qtgl.iga.bo.TaskLog;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Component
public interface TaskLogDao {


  List<TaskLog> findAll(String domain);

   Integer save(TaskLog taskLog,String domain,String type);






}
