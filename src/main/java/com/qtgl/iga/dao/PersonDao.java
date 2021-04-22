package com.qtgl.iga.dao;

import com.qtgl.iga.bo.Person;

import java.util.List;
import java.util.Map;

public interface PersonDao {


    List<Person> getAll(String tenantId);


    List<Person> savePerson(List<Person> list, String tenantId);

    List<Person> updatePerson(List<Person> list, String tenantId);

    List<Person> deletePerson(List<Person> list, String tenantId);

    List<String> getAccountByIdentityId(List<String> ids);

    List<String> getOccupyByIdentityId(List<String> ids);

    Integer deleteAccount(List<String> ids);

    Integer deleteOccupy(List<String> ids);

         Integer saveToSso(Map<String, List<Person>> personMap, String tenantId) ;


}
