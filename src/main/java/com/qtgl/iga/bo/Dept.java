package com.qtgl.iga.bo;


import lombok.Data;

import java.time.LocalDateTime;

/**
 * 组织机构
 * @author 1
 */
@Data
public class Dept {
    /**
     * 主键
     */
    private String id;
    /**
     * 部门代码（唯一）
     */
    private String deptCode;
    /**
     * 部门名称
     */
    private String deptName;
    /**
     * 父级code
     */
    private String parentCode;
    /**
     * 是否删除
     */
    private Boolean delMark;
    /**
     * 是否顶级部门
     */
    private Boolean independent;
    /**
     * 所属租户id
     */
    private String tenantId;
    /**
     * 修改时间
     */
    private LocalDateTime updateTime;
    /**
     * 来源（人读，人事/教务）
     */
    private String source;
    /**
     * 标签
     */
    private String tags;
    /**
     * 数据来源（机读，手工/同步）
     */
    private String dataSource;
    /**
     * 描述
     */
    private String description;
    /**
     * 是否孤儿节点
     */
    private Boolean orphan;
    /**
     * 元
     */
    private String meta;
    /**
     * 部门树类型
     */
    private String treeType;
    /**
     * 部门类型
     */
    private String type;
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    /**
     * 是否有效
     */
    private Boolean active;
    /**
     * 启用时间
     */
    private LocalDateTime activeTime;
    /**
     * 简称
     */
    private String abbreviation;
    /**
     * 排序
     */
    private Integer deptIndex;
}
