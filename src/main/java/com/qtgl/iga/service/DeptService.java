package com.qtgl.iga.service;

import com.qtgl.iga.bean.DeptBean;
import com.qtgl.iga.bo.Dept;
import com.qtgl.iga.bo.DomainInfo;

import java.util.List;
import java.util.Map;

public interface DeptService {


//    List<DeptBean> buildDeptByDomain(DomainInfo domainInfo);

    List<DeptBean> findDept(Map<String, Object> arguments, DomainInfo domain) throws Exception;

    List<DeptBean> findDeptByDomainName(String domainName, String treeType,Integer delMark);

     Map<DeptBean, String> buildDeptUpdateResult(DomainInfo domain);
}
