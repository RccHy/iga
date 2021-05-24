package com.qtgl.iga.bo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * <FileName> Tenant
 * <Desc> sso库tenant表
 *
 * @author 1
 */
@Data
public class Tenant {
    /**
     * 主键
     */
    private String id;
    /**
     * 租户
     */
    private String domain;

    private String tenantMatch;
    /**
     * 租户名称
     */
    private String tenantName;
    /**
     * 删除标记
     */
    private Boolean delMark;
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    /**
     * 修改时间
     */
    private LocalDateTime updateTime;
}
