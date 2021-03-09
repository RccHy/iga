package com.qtgl.iga.bo;

import lombok.Data;

import java.util.Date;

/**
 * <FileName> UpStreamType
 * <Desc> 上游源类型字段映射表
 **/
@Data
public class UpStreamTypeField {
    //主键
    private String id;

    //上游源数据类型注册外键
    private String upstreamTypeId;

    //源字段名称
    private String sourceField;

    //转换后字段名称
    private String targetField;

    //创建时间
    private Date createTime;

    //修改时间
    private Date updateTime;

    //租户外键
    private String domain;

}
