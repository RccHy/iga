package com.qtgl.iga.dao;


import com.qtgl.iga.bo.Upstream;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface UpstreamDao {

    List<Upstream> findAll(Map<String, Object> arguments, String domain);

    Upstream saveUpstream(Upstream upstream, String domain) throws Exception;

    Integer deleteUpstream(String id) throws Exception;

    Upstream updateUpstream(Upstream upstream) throws Exception;

    Upstream findById(String id);

    ArrayList<Upstream> getUpstreams(String id, String domain);


}
