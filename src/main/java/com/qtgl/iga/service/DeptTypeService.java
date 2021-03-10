package com.qtgl.iga.service;

import com.qtgl.iga.bo.DeptType;

import java.util.List;
import java.util.Map;

public interface DeptTypeService {


    List<DeptType> getAllDeptTypes(Map<String, Object> arguments, String domain);

    DeptType deleteSchemaField(Map<String, Object> arguments, String domain) throws Exception;

    DeptType saveSchemaField(DeptType deptType, String domain) throws Exception;

    DeptType updateSchemaField(DeptType deptType) throws Exception;
}
