package com.qtgl.iga.dao;


import com.qtgl.iga.bean.TreeBean;

import java.util.ArrayList;
import java.util.List;

public interface PostDao {



    List<TreeBean> findByTenantId(String id);

    ArrayList<TreeBean> updateDept(ArrayList<TreeBean> list, String tenantId);

    ArrayList<TreeBean> saveDept(ArrayList<TreeBean> list, String tenantId);

    ArrayList<TreeBean> deleteDept(ArrayList<TreeBean> list);

    List<TreeBean> findRootData(String tenantId);

    List<TreeBean> findPostType(String id);
}
