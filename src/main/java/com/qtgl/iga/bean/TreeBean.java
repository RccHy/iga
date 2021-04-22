package com.qtgl.iga.bean;


import lombok.Data;


import java.time.LocalDateTime;

@Data
public class TreeBean {

    private String code;

    private String name;

    private String parentCode;

    private Integer independent;

    private String tags;

    private String description;

    private String meta;

    private String source;

    /**
     * push  PULL  builtin
     */
    private String dataSource;

    private LocalDateTime createTime;


    private LocalDateTime updateTime;

    private String upstreamTypeId;

    private Integer deptIndex;

    private String treeType;

    private String abbreviation;

    private Integer delMark;

    private String type;
//    public DeptBean(Dept dept) {
//        this.setCode(dept.getDeptCode());
//        this.setParentCode(dept.getParentCode());
//        this.setName(dept.getDeptName());
//        this.setSource(dept.getSource());
//    }

    public TreeBean() {
    }
}
