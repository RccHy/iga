package com.qtgl.iga.service;


import com.qtgl.iga.bo.UpstreamType;
import com.qtgl.iga.bo.UpstreamTypeField;
import com.qtgl.iga.vo.UpstreamTypeVo;

import java.util.List;
import java.util.Map;

public interface UpstreamTypeService {


    List<UpstreamTypeVo> findAll(Map<String, Object> arguments, String domain);

    UpstreamType deleteUpstreamType(Map<String, Object> arguments, String domain) throws Exception;

    UpstreamType saveUpstreamType(UpstreamType upstreamType, String domain);

    UpstreamType updateUpstreamType(UpstreamType upstreamType) throws Exception;

    List<UpstreamTypeField> findFields(String url);
}
