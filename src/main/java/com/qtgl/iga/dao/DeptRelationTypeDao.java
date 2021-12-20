package com.qtgl.iga.dao;

import com.qtgl.iga.bo.DeptRelationType;
import com.qtgl.iga.bo.DomainInfo;

import java.util.List;

public interface DeptRelationTypeDao {
     List<DeptRelationType> findAll(String domain);
     void initialization(String domainInfo);
}
