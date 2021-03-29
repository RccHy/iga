package com.qtgl.iga.dao;


import com.qtgl.iga.bean.DeptBean;
import com.qtgl.iga.bo.Dept;
import com.qtgl.iga.bo.UserType;

import java.util.ArrayList;
import java.util.List;

public interface UserTypeDao {



    List<UserType> findByTenantId(String id);

    ArrayList<DeptBean> updateDept(ArrayList<DeptBean> list, String tenantId);

    ArrayList<DeptBean> saveDept(ArrayList<DeptBean> list, String tenantId);

    ArrayList<DeptBean> deleteDept(ArrayList<DeptBean> list);

    List<DeptBean> findRootData();
}
