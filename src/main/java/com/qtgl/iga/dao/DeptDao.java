package com.qtgl.iga.dao;


import com.qtgl.iga.bean.DeptBean;
import com.qtgl.iga.bo.Dept;

import java.util.ArrayList;
import java.util.List;

public interface DeptDao {

    List<Dept> getAllDepts();

    Dept findById(String id);

    List<DeptBean> findByTenantId(String id, String treeTypeId);

    ArrayList<DeptBean> updateDept(ArrayList<DeptBean> list, String tenantId);

    ArrayList<DeptBean> saveDept(ArrayList<DeptBean> list, String tenantId);

    ArrayList<DeptBean> deleteDept(ArrayList<DeptBean> list);
}
