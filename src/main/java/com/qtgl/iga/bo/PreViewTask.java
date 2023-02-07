package com.qtgl.iga.bo;

import lombok.Data;

import java.sql.Timestamp;

/**
 * 预览任务
 *
 * @author 1
 */
@Data
public class PreViewTask {
    /**
     * 主键标识
     */
    private String id;

    /**
     * 任务标识
     */
    private String taskId;
    /**
     * 任务状态
     */
    private String status;
    /**
     * 创建时间
     */
    private Timestamp createTime;
    /**
     * 租户外键
     */
    private String domain;
    /**
     * 预览类型
     */
    private String type;
    /**
     * 修改时间
     */
    private Timestamp updateTime;

    /**
     * 统计变更数量  没有变化/新增/删除/修改/无效
     */
    private String statistics;
    /**
     * 失败的原因
     */
    private String reason;


}
