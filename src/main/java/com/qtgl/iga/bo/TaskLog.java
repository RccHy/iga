package com.qtgl.iga.bo;


import lombok.Data;


/**
 * 定时任务日志
 *
 * @author 1
 */
@Data
public class TaskLog {
    /**
     * 主键
     */
    private String id;
    /**
     * 状态
     */
    private String status;
    /**
     * 部门变更数量
     */
    private String deptNo;
    /**
     * 岗位变更数量
     */
    private String postNo;
    /**
     * 人员变更数量
     */
    private String personNo;
    /**
     * 人员身份变更数量
     */
    private String occupyNo;
    /**
     * 创建时间
     */
    private Long createTime;
    /**
     * 修改时间
     */
    private Long updateTime;
    /**
     * 租户
     */
    private String domain;
    /**
     * 原因
     */
    private String reason;
    /**
     * 数据
     */
    private String data;
    /**
     * 同步方式
     */
    private Integer synWay;
}
