package com.qtgl.iga.bo;

import lombok.Data;

/**
 * sso中 ds_config表
 *
 * @author 1
 */
@Data
public class DsConfig {

    /**
     * 主键
     */
    private String id;
    /**
     * 配置
     */
    private String config;
    /**
     * 租户id
     */
    private String tenantId;
}
