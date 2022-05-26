package com.qtgl.iga.service;

import com.qtgl.iga.bean.OccupyConnection;
import com.qtgl.iga.bean.OccupyDto;
import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.bo.TaskLog;

import java.util.List;
import java.util.Map;

public interface OccupyService {

    Map<String, List<OccupyDto>> buildOccupy(DomainInfo domain, TaskLog lastTaskLog) throws Exception;

    OccupyConnection findOccupies(Map<String, Object> arguments, DomainInfo domain) throws Exception;

    OccupyConnection preViewOccupies(Map<String, Object> arguments, DomainInfo domain) throws Exception;


}
