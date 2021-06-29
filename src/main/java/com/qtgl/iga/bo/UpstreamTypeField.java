package com.qtgl.iga.bo;

import lombok.Data;

import java.sql.Timestamp;

/**
 * <FileName> UpstreamTypeField
 * <Desc> 权威源类型字段映射表
 *
 * @author 1
 */
@Data
public class UpstreamTypeField {
    /**
     * 主键
     */
    private String id;

    /**
     * 权威源数据类型注册外键
     */
    private String upstreamTypeId;

    /**
     * 源字段名称
     */
    private String sourceField;

    /**
     * 转换后字段名称
     */
    private String targetField;

    /**
     * 创建时间
     */
    private Timestamp createTime;

    /**
     * 修改时间
     */
    private Timestamp updateTime;

    /**
     * 租户外键
     */
    private String domain;

}
