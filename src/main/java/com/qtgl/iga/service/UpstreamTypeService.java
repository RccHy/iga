package com.qtgl.iga.service;


import com.alibaba.fastjson.JSONArray;
import com.google.gson.JsonArray;
import com.qtgl.iga.bo.UpstreamType;
import com.qtgl.iga.bo.UpstreamTypeField;
import com.qtgl.iga.vo.UpstreamTypeVo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface UpstreamTypeService {


    List<UpstreamTypeVo> findAll(Map<String, Object> arguments, String domain);

    UpstreamType deleteUpstreamType(Map<String, Object> arguments, String domain) throws Exception;

    UpstreamType saveUpstreamType(UpstreamType upstreamType, String domain) throws Exception;

    UpstreamType updateUpstreamType(UpstreamType upstreamType) throws Exception;

    List<UpstreamTypeField> findFields(String url);

    HashMap<Object, Object> upstreamTypesData(Map<String, Object> arguments, String domainName);
}
