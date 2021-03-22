package com.qtgl.iga.bean;


import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

@Getter
@Setter
public class DeptBean {

    private String code;

    private String name;

    private String parentCode;

    private String source;

    private Timestamp createTime;
}
