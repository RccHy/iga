package com.qtgl.iga.bo;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * <FileName> UpStreamType
 * <Desc> 上游源类型表
 **/
@Data
public class UpStreamType {
    //主键
    private String id;

    //应用集成注册外建
    private String upstreamId;

    //描述
    private String description;

    //同步类型  部门/岗位/人员
    private String synType;

    //属组织机构类别外建
    private String deptTypeId;

    //是否启用前缀 【规则】
    private Boolean enablePrefix;

    //是否启用
    private Integer active;

    //是否启用时间
    private Date activeTime;

    //是否为根数据源【抽到新表】
    private Boolean root;

    //注册时间
    private Date createTime;

    //修改时间
    private Date updateTime;

    //网关数据服务id
    private String serviceCode;

    //://服务/类型/方法
    private String graphqlUrl;

    //租户外键
    private String domain;

    //映射字段
    private List<UpStreamTypeField> upStreamTypeFields;

}
