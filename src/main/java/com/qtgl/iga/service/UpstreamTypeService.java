package com.qtgl.iga.service;


import com.qtgl.iga.bo.UpstreamType;

import java.util.List;
import java.util.Map;

public interface UpstreamTypeService {


    List<UpstreamType> findAll(Map<String, Object> arguments, String domain);

    UpstreamType deleteUpstreamType(Map<String, Object> arguments, String domain) throws Exception;

    UpstreamType saveUpstreamType(UpstreamType upstreamType, String domain);

    UpstreamType updateUpstreamType(UpstreamType upstreamType);
}
