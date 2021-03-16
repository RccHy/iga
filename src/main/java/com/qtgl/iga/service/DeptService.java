package com.qtgl.iga.service;

import com.qtgl.iga.bo.Dept;

import java.util.List;

public interface DeptService {


    List<Dept> getAllDepts();

    void buildDept() throws Exception;
}
