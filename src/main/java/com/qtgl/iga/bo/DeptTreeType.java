package com.qtgl.iga.bo;

import lombok.Data;

import java.sql.Timestamp;

/**
 * 组织机构树类别
 */
@Data
public class DeptTreeType {
    /**
     * 主键
     */
    private String id;

    //机构树类型代码
    private String name;

    //机构树类型代码
    private String code;

    //描述
    private String description;

    //是否允许多个根节点
    private Boolean multipleRootNode;

    //创建时间
    private Timestamp createTime;

    //修改时间
    private Timestamp updateTime;

    //创建人工号
    private String createUser;

    //租户外键
    private String domain;
    //排序字段
    private Integer treeIndex;
}
