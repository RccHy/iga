package com.qtgl.iga.service;


import com.qtgl.iga.bean.UpstreamDto;
import com.qtgl.iga.bo.Upstream;

import java.util.List;
import java.util.Map;

public interface UpstreamService {


    List<Upstream> findAll(Map<String, Object> arguments, String domain);

    Upstream deleteUpstream(Map<String, Object> arguments, String domain) throws Exception;

    Upstream saveUpstream(Upstream upstream, String domain) throws Exception;

    Upstream updateUpstream(Upstream upstream) throws Exception;

    UpstreamDto saveUpstreamAndTypes(UpstreamDto upstream, String id) throws Exception;

    List<UpstreamDto> upstreamsAndTypes(Map<String, Object> arguments, String id);
}
