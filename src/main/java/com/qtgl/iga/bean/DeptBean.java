package com.qtgl.iga.bean;


import lombok.Data;


import java.sql.Timestamp;

@Data
public class DeptBean {

    private String code;

    private String name;

    private String parentCode;

    private Integer independent;

    private String tags;

    private String description;

    private String meta;

    private String source;

    // push  pull  builtin
    private String dataSource;

    private Timestamp createTime;

    //  todo 部门排序 暂不考虑
    private Integer deptIndex;

    private String treeType;


}
