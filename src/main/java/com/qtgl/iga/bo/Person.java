package com.qtgl.iga.bo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Person {


    private String id;
    private String name;
    private String accountNo;
    private String cardType;
    private String cardNo;
    private String tags;
    private String tenantId;
    private String cellphone;
    private String email;
    private String source;
    private String dataSource;
    private String active;
    private String activeTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Integer delMark;

    private String upstreamType;

    private String openId;



}
