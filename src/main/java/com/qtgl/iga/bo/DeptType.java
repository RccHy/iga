package com.qtgl.iga.bo;

import lombok.Data;

import java.util.Date;

//组织机构类别
@Data
public class DeptType {

    private String id;
    private String name;
    private String code;
    private String description;
    private Date createTime;
    private Date updateTime;
    private String createUser;
    private String domain;
}
