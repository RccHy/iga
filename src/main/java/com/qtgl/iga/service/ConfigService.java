package com.qtgl.iga.service;

public interface ConfigService {

    public String getPasswordConfigByTenantIdAndStatusAndPluginNameAndDelMarkIsFalse(String tenantId,String status, String commonPlugin);
}
