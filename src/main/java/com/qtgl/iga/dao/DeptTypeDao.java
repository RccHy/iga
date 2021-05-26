package com.qtgl.iga.dao;


import com.qtgl.iga.bo.DeptType;

import java.util.List;
import java.util.Map;

public interface DeptTypeDao {

    List<DeptType> getAllDeptTypes(Map<String, Object> arguments, String domain);

    DeptType deleteDeptTypes(Map<String, Object> arguments, String domain) throws Exception;

    DeptType saveDeptTypes(DeptType deptType, String domain) throws Exception;

    DeptType updateDeptTypes(DeptType deptType) throws Exception;

    DeptType findById(String id);
}
