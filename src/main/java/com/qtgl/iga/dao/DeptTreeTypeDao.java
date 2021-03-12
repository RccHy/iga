package com.qtgl.iga.dao;


import com.qtgl.iga.bo.DeptTreeType;

import java.util.List;
import java.util.Map;

public interface DeptTreeTypeDao {

    List<DeptTreeType> findAll(Map<String, Object> arguments, String domain);

    DeptTreeType saveDeptTreeType(DeptTreeType deptTreeType, String domain) throws Exception;

    DeptTreeType deleteDeptTreeType(Map<String, Object> arguments, String domain) throws Exception;

    DeptTreeType updateDeptTreeType(DeptTreeType deptTreeType) throws Exception;

    DeptTreeType findById(String id);

}