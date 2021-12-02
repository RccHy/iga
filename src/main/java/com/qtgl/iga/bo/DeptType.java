package com.qtgl.iga.bo;

import lombok.Data;

import java.sql.Timestamp;

/**
 * 组织机构类别
 *
 * @author 1
 */
@Data
public class DeptType {
    /**
     * 主键
     */
    private String id;

    /**
     * 机构类型名称
     */
    private String name;

    /**
     * 机构类型代码
     */
    private String code;
    /**
     * 监控规则
     */
    private String rule;

    /**
     * 描述
     */
    private String description;

    /**
     * 创建时间
     */
    private Timestamp createTime;

    /**
     * 修改时间
     */
    private Timestamp updateTime;

    /**
     * 创建人工号
     */
    private String createUser;

    /**
     * 租户外键
     */
    private String domain;
    /**
     * 排序
     */
    private Integer typeIndex;
}
