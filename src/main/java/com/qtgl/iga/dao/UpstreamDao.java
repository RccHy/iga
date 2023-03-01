package com.qtgl.iga.dao;


import com.qtgl.iga.bo.*;

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

    Integer saveUpstreamAndTypesAndNode(Upstream upstream, List<UpstreamType> upstreamTypes, List<Node> nodes, List<NodeRules> nodeRulesList, DomainInfo domainInfo);

    void findByAppNameAndAppCode(String appName, String appCode, String domain);

    ArrayList<Upstream> findByDomainAndActiveIsFalse(String id);

    ArrayList<Upstream> findByOtherDomain(String id);
}
