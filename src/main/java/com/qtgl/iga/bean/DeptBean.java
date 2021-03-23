package com.qtgl.iga.bean;


import lombok.Data;


import java.sql.Timestamp;

@Data
public class DeptBean {

    private String code;

    private String name;

    private String parentCode;

    private Boolean independent;

    private String tags;

    private String description;

    private String meta;

    private String source;

    // push  pull  builtin
    private String dataSource;

    private Timestamp createTime;


}
