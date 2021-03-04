package com.qtgl.iga.bo;


import lombok.Data;

import java.util.Date;

@Data
public class Dept {

    private String id;
    private String name;
    private String code;
    private String typeId;
    private Date createTime;

}
