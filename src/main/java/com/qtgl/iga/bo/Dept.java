package com.qtgl.iga.bo;


import lombok.Data;

import java.time.LocalDateTime;

//组织机构
@Data
public class Dept {

    private String id;

    private String deptCode;

    private String deptName;

    private String parentCode;

    private Boolean delMark;

    private Boolean independent;

    private String tenantId;

    private LocalDateTime updateTime;

    private String source;

    private String tags;

    private String dataSource;

    private String description;

    private Boolean orphan;

    private String meta;

    private String treeType;

    private String type;

    private LocalDateTime createTime;

    private Boolean active;


}
