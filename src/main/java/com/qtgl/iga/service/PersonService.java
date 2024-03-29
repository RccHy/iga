package com.qtgl.iga.service;

import com.alibaba.fastjson.JSONObject;
import com.qtgl.iga.bean.PersonConnection;
import com.qtgl.iga.bo.*;

import java.util.List;
import java.util.Map;

public interface PersonService {


    Map<String, List<Person>> buildPerson(DomainInfo domain, TaskLog lastTaskLog, TaskLog currentTask, List<NodeRules> nodeRules) throws Exception;

    PersonConnection findPersons(Map<String, Object> arguments, DomainInfo domain) throws Exception;

    PersonConnection preViewPersons(Map<String, Object> arguments, DomainInfo domain) throws Exception;

    PreViewTask reFreshPersons(Map<String, Object> arguments, DomainInfo domain, PreViewTask preViewTask) throws Exception;

    PreViewTask reFreshTaskStatus(Map<String, Object> arguments, DomainInfo domain);

    PreViewTask testUserTask(DomainInfo domain, PreViewTask preViewTask);

    JSONObject dealWithPerson(DomainInfo domainInfo);

    PersonConnection igaUser(Map<String, Object> arguments, DomainInfo domain);
}
