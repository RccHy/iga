package com.qtgl.iga.service;

import com.qtgl.iga.bean.DeptBean;
import com.qtgl.iga.bo.DomainInfo;

import java.util.List;
import java.util.Map;

/**
 * <FileName> PostService
 * <Desc>
 *
 * @author HP
 */
public interface PostService {
    List<DeptBean> findPosts(Map<String, Object> arguments,DomainInfo domain) throws Exception;

    List<DeptBean> findDeptByDomainName(String domainName) throws Exception;
}
