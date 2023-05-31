package com.qtgl.iga.dao;


import com.qtgl.iga.bean.TreeBean;
import com.qtgl.iga.bo.DynamicAttr;
import com.qtgl.iga.bo.DynamicValue;

import java.util.ArrayList;
import java.util.List;

public interface PostDao {


    List<TreeBean> findByTenantId(String id);

    List<TreeBean> updatePostBatch(List<TreeBean> list, String tenantId);

    ArrayList<TreeBean> saveDept(ArrayList<TreeBean> list, String tenantId);

    ArrayList<TreeBean> deleteDept(ArrayList<TreeBean> list);

    List<TreeBean> findRootData(String tenantId);

    List<TreeBean> findPostType(String id);

    Integer renewData(ArrayList<TreeBean> insertList, ArrayList<TreeBean> updateList, ArrayList<TreeBean> deleteList, ArrayList<TreeBean> invalidList, List<DynamicValue> valueUpdate, List<DynamicValue> valueInsert, String tenantId);

    Integer renewDataTest(ArrayList<TreeBean> keepList, ArrayList<TreeBean> insertList, ArrayList<TreeBean> updateList, ArrayList<TreeBean> deleteList,
                          ArrayList<TreeBean> invalidList, List<DynamicValue> valueUpdate, List<DynamicValue> valueInsert, List<DynamicAttr> attrList, String tenantId);

    List<TreeBean> findActiveDataByTenantId(String tenantId);

    List<TreeBean> findByTenantIdAndDelMarkIsFalse(String tenantId);
}
