package com.qtgl.iga.service;

import com.qtgl.iga.bo.DeptRelationType;

import java.util.List;

public interface DeptRelationTypeService {
     List<DeptRelationType> findAll(String domain);
}
