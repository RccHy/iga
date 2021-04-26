package com.qtgl.iga.dao;

import com.qtgl.iga.bo.UpstreamDept;


/**
 * <FileName> UpstreamDeptDao
 * <Desc>
 **/
public interface UpstreamDeptDao {

    UpstreamDept saveUpstreamDepts(UpstreamDept upstreamDept);

    UpstreamDept findUpstreamDeptByUpstreamId(String id);

    UpstreamDept updateUpstreamDepts(UpstreamDept upstreamDept);
}
