package com.qtgl.iga.service;

import com.qtgl.iga.bean.TreeBean;
import com.qtgl.iga.bo.DomainInfo;

import java.util.List;
import java.util.Map;

public interface DeptService {


//    List<DeptBean> buildDeptByDomain(DomainInfo domainInfo);

    List<TreeBean> findDept(Map<String, Object> arguments, DomainInfo domain) throws Exception;

    List<TreeBean> findDeptByDomainName(String domainName, String treeType, Integer delMark);

     Map<TreeBean, String> buildDeptUpdateResult(DomainInfo domain);
}
