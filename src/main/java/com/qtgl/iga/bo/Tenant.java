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

    private String id;

    private String domain;

    private String tenantMatch;

    private String tenantName;

    private Boolean delMark;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
