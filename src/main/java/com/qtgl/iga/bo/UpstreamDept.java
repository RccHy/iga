package com.qtgl.iga.bo;

import lombok.Data;

import java.sql.Timestamp;

/**
 * <FileName> UpstreamDept
 * <Desc> 上游源部门数据
 **/
@Data
public class UpstreamDept {

    private String id;

    private String upstreamTypeId;

    private String dept;

    private Timestamp createTime;
}
