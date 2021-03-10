package com.qtgl.iga.bo;

import lombok.Data;

import java.sql.Timestamp;

//注册用户表
@Data
public class DomainInfo {

    private String id;

    private String domainId;

    private String domainName;

    private String clientId;

    private String clientSecret;

    private Integer status;

    private Timestamp createTime;

    private Timestamp updateTime;

    private String createUser;


}
