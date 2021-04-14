package com.qtgl.iga.service;

import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.bo.Person;

import java.util.List;
import java.util.Map;

public interface PersonService {


     Map<String, List<Person>> buildPerson(DomainInfo domain) throws Exception;
}
