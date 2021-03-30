package com.qtgl.iga.bean;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * <FileName> UserBean
 * <Desc> 岗位
 *
 * @author HP
 */
@Data
public class UserBean {

    /**
     * 岗位代码（唯一）
     */
    private String userType;
    /**
     * 岗位名称
     */
    private String name;
    /**
     * 父级code
     */
    private String parentCode;

    /**
     * 所属租户id
     */
    private String tenantId;
    /**
     * 是否身份岗
     */
    private Boolean formal;
    /**
     * 标签
     */
    private String tags;
    /**
     * 描述
     */
    private String description;
    /**
     *
     */
    private String meta;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 数据来源（）
     */
    private String dataSource;
    /**
     * 是否孤儿节点
     */
    private Boolean orphan;

    /**
     * 排序
     */
    private String index;


}
