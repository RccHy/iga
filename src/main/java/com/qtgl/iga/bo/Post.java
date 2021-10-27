package com.qtgl.iga.bo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * <FileName> Post
 * <Desc> sso岗位表
 *
 * @author 1
 */
@Data
public class Post {
    /**
     * 主键
     */
    private String id;
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
     * 是否能登录
     */
    private Boolean canLogin;
    /**
     * 延迟时间
     */
    private Integer delayTime;
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
     * 删除标记
     */
    private Boolean delMark;
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    /**
     * 修改时间
     */
    private LocalDateTime updateTime;
    /**
     * 数据来源（）
     */
    private String dataSource;
    /**
     * 是否孤儿节点
     */
    private Boolean orphan;
    /**
     * 是否有效
     */
    private Boolean active;
    /**
     * 是否有效更新时间
     */
    private LocalDateTime activeTime;
    /**
     * 排序
     */
    private String index;
    /**
     * 来源
     */
    private String source;
    /**
     * 类型
     */
    private Integer postType;

    /**
     * 应用标识
     */
    private String clientId;


}
