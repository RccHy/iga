package com.qtgl.iga.bo;

import lombok.Data;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;

/**
 * <FileName> UpStreamType
 * <Desc> 权威源类型表
 *
 * @author 1
 */
@Data
public class UpstreamType implements Serializable {
    /**
     * 主键
     */
    private String id;

    /**
     * 应用集成注册外建
     */
    private String upstreamId;

    /**
     * 描述
     */
    private String description;

    /**
     * 同步类型  部门/岗位/人员
     */
    private String synType;

    /**
     * 属组织机构类别外建
     */
    private String deptTypeId;

    /**
     * 属组织机构类别树外建
     */
    private String deptTreeTypeId;

    /**
     * 是否启用前缀 【规则】
     */
    private Boolean enablePrefix;

    /**
     * 是否启用
     */
    private Boolean active;

    /**
     * 是否启用时间
     */
    private Timestamp activeTime;

    /**
     * 是否为根数据源【抽到新表】
     */
    private Boolean root;

    /**
     * 注册时间
     */
    private Timestamp createTime;

    /**
     * 修改时间
     */
    private Timestamp updateTime;

    /**
     * 网关数据服务id
     */
    private String serviceCode;

    /**
     * ://服务/类型/方法
     */
    private String graphqlUrl;

    /**
     * 租户外键
     */
    private String domain;

    /**
     * 映射字段
     */
    private List<UpstreamTypeField> upstreamTypeFields;

    /**
     * 是否分页
     */
    private Boolean isPage;

    /**
     * 方式
     */
    private Integer synWay;
    /**
     * 时间戳查询字段
     */
    private Timestamp time;

}
