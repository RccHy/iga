package com.qtgl.iga.service;

import com.qtgl.iga.bean.IgaOccupyConnection;
import com.qtgl.iga.bean.OccupyConnection;
import com.qtgl.iga.bean.OccupyDto;
import com.qtgl.iga.bo.*;

import java.util.List;
import java.util.Map;

public interface OccupyService {

    Map<String, List<OccupyDto>> buildOccupy(DomainInfo domain, TaskLog lastTaskLog, TaskLog currentTask, List<NodeRules> userRules) throws Exception;

    OccupyConnection findOccupies(Map<String, Object> arguments, DomainInfo domain) throws Exception;

    OccupyConnection preViewOccupies(Map<String, Object> arguments, DomainInfo domain) throws Exception;


    PreViewTask reFreshOccupies(Map<String, Object> arguments, DomainInfo domain, PreViewTask preViewTask);

    PreViewTask testOccupyTask(DomainInfo domain, PreViewTask preViewTask);

    IgaOccupyConnection igaOccupy(Map<String, Object> arguments, DomainInfo domain);

    void saveToSso(Map<String, List<OccupyDto>> octResult, String tenantId , List<DynamicValue> valueUpdate, List<DynamicValue> valueInsert);

    List<OccupyDto> findAll(String tenantId, String deptCode, String postCode);
}
