package com.qtgl.iga.bo;


import lombok.Data;


@Data
public class TaskLog {

    private String id;
    private String status;
    private String deptNo;
    private String postNo;
    private String personNo;
    private String occupyNo;
    private Long createTime;
    private Long updateTime;
    private String domain;

    private String reason;
    //  忽略ignore    解决fix
    private String mark;
}
