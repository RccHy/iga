package com.qtgl.iga.service.impl;


import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.qtgl.iga.bo.DsConfig;
import com.qtgl.iga.dao.DsConfigDao;
import com.qtgl.iga.service.DsConfigService;
import com.qtgl.iga.task.TaskConfig;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DsConfigServiceImpl implements DsConfigService {

    @Autowired
    DsConfigDao dsConfigDao;

    public static Logger logger = LoggerFactory.getLogger(DsConfigServiceImpl.class);

    private static final String SYNC_WAY = "SYNC_WAY";
    private static final String SETTINGS_DS = "SETTINGS_DS";
    private static final String SETTINGS = "SETTINGS";


    @Override
    public List<DsConfig> findAll() {
        return dsConfigDao.findAll();
    }

    @Override
    public String getSyncWay(DsConfig dsConfig) {
        if (null == dsConfig) {
            return null;
        }
        JSONObject settingDs = getSettingDs(dsConfig);
        if (null == settingDs) {
            return null;
        }
        String syncWay = settingDs.getString(SYNC_WAY);
        if (StringUtils.isBlank(syncWay)) {
            return TaskConfig.SYNC_WAY_BRIDGE;
        }

        return syncWay;
    }

    public JSONObject getSettingDs(DsConfig dsConfig) {
        JSONObject setting = getSetting(dsConfig);
        if (null == setting) {
            return null;
        }
        return setting.getJSONObject(SETTINGS_DS);

    }

    public JSONObject getSetting(DsConfig dsConfig) {
        String config = dsConfig.getConfig();
        if (config == null) {
            return null;
        }
        JSONObject jsonObject = JSONObject.parseObject(config);
        return getSetting(jsonObject);
    }

    public JSONObject getSetting(JSONObject jsonObject) {
        if (jsonObject == null) {
            return null;
        }
        JSONArray jsonArray = jsonObject.getJSONArray(SETTINGS);
        if (null == jsonArray) {
            return null;
        }
        Object o = jsonArray.get(0);

        if (!(o instanceof JSONObject)) {
            return null;
        }

        return (JSONObject) o;
    }
}
