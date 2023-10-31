package com.qtgl.iga.dao;

import com.qtgl.iga.bean.OccupyDto;
import com.qtgl.iga.bean.OccupyEdge;
import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.bo.DynamicAttr;
import com.qtgl.iga.bo.DynamicValue;
import com.qtgl.iga.bo.Tenant;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface OccupyDao {

    List<OccupyDto> findAll(String tenantId, String deptCode, String postCode);

    Integer saveToSso(Map<String, List<OccupyDto>> occupyMa, String tenantId, List<DynamicValue> valueUpdate, List<DynamicValue> valueInsert);

    Integer saveToSsoTest(Map<String, List<OccupyDto>> occupyMap, String tenantId, List<DynamicValue> valueUpdate, List<DynamicValue> valueInsert, List<DynamicAttr> attrList, List<DynamicValue> dynamicValues);

    void removeData(DomainInfo domain);

    Integer saveToTemp(Map<String, OccupyDto> occupyDtoMap, DomainInfo domain);

    List<OccupyEdge> findUpstreamDataState(Map<String, Object> arguments, String domain);

    Integer findOccupyTempCount(Map<String, Object> arguments, String domain);

    Map<String, Object> igaOccupy(Map<String, Object> arguments, Tenant tenant);

    List<OccupyDto> findOccupyByIdentityId(Set<String> keySet, Map<String, Object> arguments,Tenant tenant);


}
