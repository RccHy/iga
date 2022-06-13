package com.qtgl.iga.service;

import com.qtgl.iga.bean.PersonConnection;
import com.qtgl.iga.bean.PreViewResult;
import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.bo.Person;
import com.qtgl.iga.bo.TaskLog;

import java.util.List;
import java.util.Map;

public interface PersonService {


    Map<String, List<Person>> buildPerson(DomainInfo domain, TaskLog lastTaskLog) throws Exception;

    PersonConnection findPersons(Map<String, Object> arguments, DomainInfo domain) throws Exception;

    PersonConnection preViewPersons(Map<String, Object> arguments, DomainInfo domain) throws Exception;

    PreViewResult reFreshPersons(Map<String, Object> arguments, DomainInfo domain) throws Exception;

    PreViewResult reFreshTaskStatus(Map<String, Object> arguments, DomainInfo domain);
}
