package com.qtgl.iga.dao;


import com.qtgl.iga.bo.Dept;

import java.util.List;

public interface DeptDao {

    List<Dept> getAllDepts();

    Dept findById(String id);

}
