package com.qtgl.iga.dao;


import com.qtgl.iga.bo.UpstreamType;
import com.qtgl.iga.vo.UpstreamTypeVo;

import java.util.List;
import java.util.Map;

public interface UpstreamTypeDao {

    List<UpstreamTypeVo> findAll(Map<String, Object> arguments, String domain);

    UpstreamType saveUpstreamType(UpstreamType upstreamType, String domain);

    UpstreamType deleteUpstreamType(Map<String, Object> arguments, String domain) throws Exception;

    UpstreamType updateUpstreamType(UpstreamType upstreamType);

    List<UpstreamType> findByUpstreamId(String id);

    UpstreamType findById(String id);
}
