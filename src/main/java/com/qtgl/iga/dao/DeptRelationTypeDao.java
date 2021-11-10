package com.qtgl.iga.dao;

import com.qtgl.iga.bo.DeptRelationType;

import java.util.List;

public interface DeptRelationTypeDao {
     List<DeptRelationType> findAll(String domain);
}
