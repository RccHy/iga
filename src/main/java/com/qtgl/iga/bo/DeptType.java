package com.qtgl.iga.bo;

import lombok.Data;

import java.util.Date;

@Data
public class DeptType {

    private String id;
    private String name;
    private String code;
    private Boolean multipleRootNode;
    private Date createTime;
    private Date updateTime;
    private String createUser;
}
