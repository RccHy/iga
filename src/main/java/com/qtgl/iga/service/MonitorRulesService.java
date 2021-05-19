package com.qtgl.iga.service;

import com.qtgl.iga.bo.MonitorRules;

import java.util.List;

public interface MonitorRulesService {


     List<MonitorRules> findAll(String domain,String type);
}
