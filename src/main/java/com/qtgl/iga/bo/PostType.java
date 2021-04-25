package com.qtgl.iga.bo;

import lombok.Data;

import java.sql.Timestamp;

//岗位类别
@Data
public class PostType {
    //主键
    private String id;

    //机构类型名称
    private String name;

    //机构类型代码
    private String code;

    //描述
    private String description;

    //创建时间
    private Timestamp createTime;

    //修改时间
    private Timestamp updateTime;

    //创建人工号
    private String createUser;

    //租户外键
    private String domain;
}
