package com.qtgl.iga.service;

import com.qtgl.iga.bean.DeptBean;
import com.qtgl.iga.bo.DomainInfo;

import java.util.List;
import java.util.Map;

/**
 * <FileName> PostTypeService
 * <Desc>
 *
 * @author HP*/
public interface PostTypeService {
    List<DeptBean> findUserType( DomainInfo domain) throws Exception;

    List<DeptBean> findDeptByDomainName(String domainName);
}
