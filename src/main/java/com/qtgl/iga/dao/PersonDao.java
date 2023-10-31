package com.qtgl.iga.dao;

import com.alibaba.fastjson.JSONObject;
import com.qtgl.iga.bean.MergeAttrRule;
import com.qtgl.iga.bean.PersonEdge;
import com.qtgl.iga.bo.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface PersonDao {


    List<Person> getAll(String tenantId);


    Integer saveToSso(Map<String, List<Person>> personMap, String tenantId, List<DynamicValue> valueUpdate, List<DynamicValue> valueInsert, List<Certificate> certificates, List<MergeAttrRule> mergeAttrRules);

    Integer saveToSsoTest(Map<String, List<Person>> personMap, String tenantId, List<DynamicValue> valueUpdate, List<DynamicValue> valueInsert, List<DynamicAttr> attrList, List<Certificate> certificates, List<DynamicValue> dynamicValues);

    Integer saveToTemp(Map<String, Person> tempUsers, DomainInfo domainInfo);

    void removeData(DomainInfo domain);

    List<Person> findDistinctPerson(String tenantId);

    List<Certificate> getAllCard(String tenantId);

    List<Person> mergeCharacteristicPerson(String tenantId);

    List<Person> getDelMarkPeople(String tenantId);

    List<Person> findRepeatPerson(String tenantId, String dataSource);

    List<Person> findPersonByDataSource(String tenantId, String dataSource);

    List<PersonEdge> findUpstreamDataState(Map<String, Object> arguments, String tenantId);

    JSONObject dealWithPeople(ArrayList<Person> resultPeople);

    Map<String, Object> findTestPersons(Map<String, Object> arguments, Tenant domain);
}
