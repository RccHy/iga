package com.qtgl.iga.bo;

import lombok.Data;

import java.sql.Timestamp;

/**
 * <FileName> UpstreamDept
 * <Desc> 上游源部门数据
 *
 * @author 1
 */
@Data
public class UpstreamDept {
    /**
     * 主键
     */
    private String id;
    /**
     * 上游源类型id
     */
    private String upstreamTypeId;
    /**
     * 部门
     */
    private String dept;
    /**
     * 创建时间
     */
    private Timestamp createTime;
}
