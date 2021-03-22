package com.qtgl.iga.bean;


import lombok.Data;


import java.sql.Timestamp;

@Data
public class DeptBean {

    private String code;

    private String name;

    private String parentCode;

    private String source;

    private Timestamp createTime;
}
