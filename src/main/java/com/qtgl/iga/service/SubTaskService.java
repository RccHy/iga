package com.qtgl.iga.service;

import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.bo.NodeRules;

import java.util.List;

public interface SubTaskService {

    public void subTask(String type, DomainInfo domain, List<NodeRules> nodeRules);
}
