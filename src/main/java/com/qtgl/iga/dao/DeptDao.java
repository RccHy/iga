package com.qtgl.iga.dao;


import com.qtgl.iga.bean.TreeBean;
import com.qtgl.iga.bo.Dept;

import java.util.ArrayList;
import java.util.List;

public interface DeptDao {

    List<Dept> getAllDepts();

    Dept findById(String id);

    List<TreeBean> findByTenantId(String id, String treeTypeId, Integer delMark);

    ArrayList<TreeBean> updateDept(ArrayList<TreeBean> list, String tenantId);

    ArrayList<TreeBean> saveDept(ArrayList<TreeBean> list, String tenantId);

    ArrayList<TreeBean> deleteDept(ArrayList<TreeBean> list);

    Integer renewData(ArrayList<TreeBean> insertList, ArrayList<TreeBean> updateList, ArrayList<TreeBean> deleteList, String tenantId);
}
