package com.qtgl.iga.service;


import com.qtgl.iga.bo.DeptTreeType;
import com.qtgl.iga.bo.UpStream;

import java.util.List;
import java.util.Map;

public interface DeptTreeTypeService {


    List<DeptTreeType> findAll(Map<String, Object> arguments, String domain);

    DeptTreeType deleteDeptTreeType(Map<String, Object> arguments, String domain) throws Exception;

    DeptTreeType saveDeptTreeType(DeptTreeType deptTreeType, String domain);

    DeptTreeType updateDeptTreeType(DeptTreeType deptTreeType);
}
