package com.qtgl.iga.dao;

import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.bo.DynamicValue;
import com.qtgl.iga.bo.Person;

import java.util.List;
import java.util.Map;

public interface PersonDao {


    List<Person> getAll(String tenantId);


    Integer saveToSso(Map<String, List<Person>> personMap, String tenantId, List<DynamicValue> valueUpdate, List<DynamicValue> valueInsert);

    Integer saveToTemp(List<Person> personList, DomainInfo domainInfo);

    List<Person> findPersonTemp(Map<String, Object> arguments, DomainInfo domain);

    void removeData(DomainInfo domain);
}
