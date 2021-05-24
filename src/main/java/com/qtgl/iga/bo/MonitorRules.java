package com.qtgl.iga.bo;


import lombok.Data;

/**
 * <FileName> MonitorRules
 * <Desc> 监控规则
 *
 * @author 1
 */

@Data
public class MonitorRules {

    /**
     * 主键
     */
    private String id;
    /**
     * 规则
     */
    private String rules;
    /**
     * 类型
     */
    private String type;
    /**
     * 租户
     */
    private String domain;
    /**
     * 启用
     */
    private Boolean active;
    /**
     * 启用时间
     */
    private Long activeTime;
    /**
     * 创建时间
     */
    private Long createTime;
    /**
     * 修改时间
     */
    private Long updateTime;


}
