package com.qtgl.iga.dao.impl;

import com.qtgl.iga.bo.Config;
import com.qtgl.iga.dao.ConfigDao;
import org.springframework.cglib.beans.BeanMap;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;


@Repository
@Component
public class ConfigDaoImpl implements ConfigDao {

    @Resource(name = "jdbcSSO")
    JdbcTemplate jdbcSSO;

    @Override
    public Config findConfigByTenantIdAndStatusAndPluginNameAndDelMarkIsFalse(String tenantId, String status, String commonPlugin) {
        String sql = "select id, tenant_id as tenantId, name, status,plugin_name as pluginName,config,del_mark as delMark, create_time as createTime,update_time as updateTime from config " +
                "where tenant_id=? and status=? and plugin_name =? and del_mark=0";
        List<Map<String, Object>> mapList = jdbcSSO.queryForList(sql, tenantId,status,commonPlugin);
        Config config = new Config();
        if (null != mapList && mapList.size() > 0) {
            for (Map<String, Object> map : mapList) {

                BeanMap beanMap = BeanMap.create(config);
                beanMap.putAll(map);
            }
            return config;
        }
        return config;
    }
}
