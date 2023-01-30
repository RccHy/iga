package com.qtgl.iga.service;

import com.alibaba.fastjson.JSONObject;
import com.qtgl.iga.bean.PersonConnection;
import com.qtgl.iga.bean.TaskResult;
import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.bo.Person;
import com.qtgl.iga.bo.PreViewTask;
import com.qtgl.iga.bo.TaskLog;

import java.util.List;
import java.util.Map;

public interface PersonService {


    Map<String, List<Person>> buildPerson(DomainInfo domain, TaskLog lastTaskLog, TaskLog currentTask) throws Exception;

    PersonConnection findPersons(Map<String, Object> arguments, DomainInfo domain) throws Exception;

    PersonConnection preViewPersons(Map<String, Object> arguments, DomainInfo domain) throws Exception;

    PreViewTask reFreshPersons(Map<String, Object> arguments, DomainInfo domain, PreViewTask preViewTask) throws Exception;

    PreViewTask reFreshTaskStatus(Map<String, Object> arguments, DomainInfo domain);

    TaskResult testPersonTask(DomainInfo domain);

    JSONObject dealWithPerson(DomainInfo domainInfo);

    PersonConnection findTestPersons(Map<String, Object> arguments, DomainInfo domain);
}
