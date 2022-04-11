package com.qtgl.iga.dao;

import com.qtgl.iga.bo.Config;

public interface ConfigDao {
    Config findConfigByTenantIdAndStatusAndPluginNameAndDelMarkIsFalse(String tenantId,String status, String commonPlugin);
}
