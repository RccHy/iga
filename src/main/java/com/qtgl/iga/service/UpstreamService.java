package com.qtgl.iga.service;


import com.qtgl.iga.bean.UpstreamDto;
import com.qtgl.iga.bo.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface UpstreamService {


    List<UpstreamDto> findAll(Map<String, Object> arguments, String domain);

    Upstream deleteUpstream(Map<String, Object> arguments, String domain) throws Exception;

    Upstream saveUpstream(Upstream upstream, String domain) throws Exception;

    Upstream updateUpstream(Upstream upstream) throws Exception;

    UpstreamDto saveUpstreamAndTypes(UpstreamDto upstream, String domain) throws Exception;

    List<UpstreamDto> upstreamsAndTypes(Map<String, Object> arguments, String id);

    UpstreamDto updateUpstreamAndTypes(UpstreamDto upstream) throws Exception;

    void saveUpstreamAndTypesAndRoleBing(Upstream upstream, List<UpstreamType> upstreamTypes, List<Node> nodes, List<NodeRules> nodeRulesList,List<NodeRulesRange> nodeRulesRanges, DomainInfo domainInfo) throws Exception;

    ArrayList<Upstream> findByDomainAndActiveIsFalse(String domainId);

    ArrayList<Upstream> getUpstreams(String upstreamId, String domainId);

    ArrayList<Upstream> findByOtherUpstream(List<String> ids,String domain);

    List<Upstream> findByUpstreamTypeIds(ArrayList<String> upstreamTypeIds,String domainId);
}
