package com.qtgl.iga.dao.impl;

import com.qtgl.iga.bo.DsConfig;
import com.qtgl.iga.dao.DsConfigDao;
import com.qtgl.iga.utils.MyBeanUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Repository
@Component
public class DsConfigDaoImpl implements DsConfigDao {


    @Resource(name = "jdbcSSOAPI")
    JdbcTemplate jdbcSSOAPI;

    @Resource(name = "api-txTemplate")
    TransactionTemplate txTemplate;


    @Override
    public List<DsConfig> findAll() {
        List<DsConfig> dsConfigs = new ArrayList<>();
        try {
            String sql = "select id , config,tenant_id as tenantId  from  ds_config ";
            List<Map<String, Object>> mapList = jdbcSSOAPI.queryForList(sql);
            if (null != mapList && mapList.size() > 0) {
                for (Map<String, Object> map : mapList) {
                    DsConfig dsConfig = new DsConfig();
                    MyBeanUtils.populate(dsConfig, map);
                    dsConfigs.add(dsConfig);
                }
                return dsConfigs;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return dsConfigs;
    }
}
