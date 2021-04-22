package com.qtgl.iga.dao;

import com.qtgl.iga.bean.OccupyDto;
import com.qtgl.iga.bo.Occupy;

import java.util.List;
import java.util.Map;

public interface OccupyDao {

    List<Occupy> findAll(String tenantId);

    Integer saveToSso(Map<String, List<OccupyDto>> occupyMa, String tenantId);
}
