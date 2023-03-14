package com.qtgl.iga.service;


import com.qtgl.iga.bo.DeptTreeType;

import java.util.List;
import java.util.Map;

public interface DeptTreeTypeService {


    List<DeptTreeType> findAll(Map<String, Object> arguments, String domain);

    DeptTreeType deleteDeptTreeType(Map<String, Object> arguments, String domain) throws Exception;

    DeptTreeType saveDeptTreeType(DeptTreeType deptTreeType, String domain) throws Exception;

    DeptTreeType updateDeptTreeType(DeptTreeType deptTreeType) throws Exception;

    DeptTreeType findById(String deptTreeTypeId);
}
