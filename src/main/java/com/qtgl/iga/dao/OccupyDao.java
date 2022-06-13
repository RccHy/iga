package com.qtgl.iga.dao;

import com.qtgl.iga.bean.OccupyDto;
import com.qtgl.iga.bo.DomainInfo;

import java.util.List;
import java.util.Map;

public interface OccupyDao {

    List<OccupyDto> findAll(String tenantId, String deptCode, String postCode);

    Integer saveToSso(Map<String, List<OccupyDto>> occupyMa, String tenantId);

    void removeData(DomainInfo domain);

    Integer saveToTemp(List<OccupyDto> occupyDtos, DomainInfo domain);

    List<OccupyDto> findOccupyTemp(Map<String, Object> arguments, DomainInfo domain);

    Integer findOccupyTempCount(DomainInfo domain);
}
