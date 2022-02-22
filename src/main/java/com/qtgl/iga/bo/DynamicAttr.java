package com.qtgl.iga.bo;


import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class DynamicAttr implements Serializable {



    private String id;
    private String name;
    private String code;
    private Boolean required;
    private String description;
    private String tenantId;
    private Date createTime = new Date();
    private Date updateTime = new Date();
    private String type;
    private String fieldType;
    private Boolean isSearch;
    private String format;
    private Integer attrIndex;



}
