package com.qtgl.iga.bo;


import lombok.Data;



@Data
public class TaskLog {

    private String id;
    private Integer status;
    private Integer deptNo;
    private Integer postNo;
    private Integer personNo;
    private Integer occupyNo;
    private Long createTime;
    private Long updateTime;
}
