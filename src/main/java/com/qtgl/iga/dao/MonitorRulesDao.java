package com.qtgl.iga.dao;

import com.qtgl.iga.bo.MonitorRules;

import java.util.List;

public interface MonitorRulesDao {


    public List<MonitorRules> findAll(String domain,String type);
}
