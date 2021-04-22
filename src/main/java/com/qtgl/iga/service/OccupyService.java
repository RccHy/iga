package com.qtgl.iga.service;

import com.qtgl.iga.bean.OccupyDto;
import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.bo.Person;

import java.util.List;
import java.util.Map;

public interface OccupyService {

    Map<String, List<OccupyDto>> buildPerson(DomainInfo domain);
}
