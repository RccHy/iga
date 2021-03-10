package com.qtgl.iga.bo;


import lombok.Data;

import java.sql.Timestamp;
//组织机构
@Data
public class Dept {

    private String id;

    private String name;

    private String code;

    private String typeId;

    private Timestamp createTime;


}
