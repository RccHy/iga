package com.qtgl.iga.service;


import com.qtgl.iga.bo.Upstream;

import java.util.List;
import java.util.Map;

public interface UpstreamService {


    List<Upstream> findAll(Map<String, Object> arguments, String domain);

    Upstream deleteUpstream(Map<String, Object> arguments, String domain) throws Exception;

    Upstream saveUpstream(Upstream upstream, String domain);

    Upstream updateUpstream(Upstream upstream);
}
