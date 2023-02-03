package com.qtgl.iga.dao;

import com.qtgl.iga.bean.OccupyDto;
import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.bo.DynamicAttr;
import com.qtgl.iga.bo.DynamicValue;
import com.qtgl.iga.bo.Tenant;

import java.util.List;
import java.util.Map;

public interface OccupyDao {

    List<OccupyDto> findAll(String tenantId, String deptCode, String postCode);

    Integer saveToSso(Map<String, List<OccupyDto>> occupyMa, String tenantId, List<DynamicValue> valueUpdate, List<DynamicValue> valueInsert);

    Integer saveToSsoTest(Map<String, List<OccupyDto>> occupyMap, String tenantId, List<DynamicValue> valueUpdate, List<DynamicValue> valueInsert, List<DynamicAttr> attrList);

    void removeData(DomainInfo domain);

    Integer saveToTemp(List<OccupyDto> occupyDtos, DomainInfo domain);

    List<OccupyDto> findOccupyTemp(Map<String, Object> arguments, DomainInfo domain);

    Integer findOccupyTempCount(Map<String, Object> arguments, DomainInfo domain);

    Map<String, Object> findTestUsers(Map<String, Object> arguments, Tenant tenant);
}
