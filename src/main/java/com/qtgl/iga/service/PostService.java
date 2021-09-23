package com.qtgl.iga.service;

import com.qtgl.iga.bean.TreeBean;
import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.bo.TaskLog;

import java.util.List;
import java.util.Map;

/**
 * <FileName> PostService
 * <Desc>
 *
 * @author HP
 */
public interface PostService {
    List<TreeBean> findPosts(Map<String, Object> arguments, DomainInfo domain) throws Exception;

    //List<TreeBean> findDeptByDomainName(String domainName) throws Exception;

    Map<TreeBean, String> buildPostUpdateResult(DomainInfo domain, TaskLog lastTaskLog) throws Exception;
}
