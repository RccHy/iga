package com.qtgl.iga.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.qtgl.iga.bo.Config;
import com.qtgl.iga.dao.ConfigDao;
import com.qtgl.iga.service.ConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Map;

@Service
@Slf4j
public class ConfigServiceImpl implements ConfigService {

    @Autowired
    ConfigDao configDao;


    @Override
    public String getPasswordConfigByTenantIdAndStatusAndPluginNameAndDelMarkIsFalse(String tenantId, String status, String commonPlugin) {
        Config config = configDao.findConfigByTenantIdAndStatusAndPluginNameAndDelMarkIsFalse(tenantId, status, commonPlugin);
        if (null == config || StringUtils.isEmpty(config.getConfig())) {
            return "MD5";
        }



        Map parse = (Map) JSONObject.parseObject(config.getConfig(), Feature.OrderedField);
        Object object = parse.get("ENCRYPTION_TEMPLATE");
        if(null==object){
            return "MD5";
        }
        Map<String,String> pwdConfig = (Map<String,String>)object;
        return new ArrayList<>(pwdConfig.keySet()).get(0);
    }
}
