package com.qtgl.iga.dao;


import com.qtgl.iga.bo.Upstream;

import java.util.List;
import java.util.Map;

public interface UpstreamDao {

    List<Upstream> findAll(Map<String, Object> arguments, String domain);

    Upstream saveUpstream(Upstream upstream, String domain);

    Upstream deleteUpstream(Map<String, Object> arguments, String domain) throws Exception;

    Upstream updateUpstream(Upstream upstream);
}
