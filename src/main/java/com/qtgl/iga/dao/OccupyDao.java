package com.qtgl.iga.dao;

import com.qtgl.iga.bean.OccupyDto;

import java.util.List;
import java.util.Map;

public interface OccupyDao {

    List<OccupyDto> findAll(String tenantId, String deptCode, String postCode);

    Integer saveToSso(Map<String, List<OccupyDto>> occupyMa, String tenantId);
}
