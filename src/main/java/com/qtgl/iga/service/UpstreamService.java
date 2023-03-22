package com.qtgl.iga.service;


import com.qtgl.iga.bean.UpstreamDto;
import com.qtgl.iga.bo.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface UpstreamService {

    Upstream findByCodeAndDomain(String code, String domain);

    List<UpstreamDto> findAll(Map<String, Object> arguments, String domain);

    Upstream deleteUpstream(Map<String, Object> arguments, String domain) throws Exception;


    Upstream saveUpstream(Upstream upstream, String domain) throws Exception;

    Upstream updateUpstream(Upstream upstream) throws Exception;

    Integer delAboutNode(Upstream upstream, DomainInfo domainInfo) throws Exception;

    UpstreamDto saveUpstreamAndTypes(UpstreamDto upstream, String domain) throws Exception;

    List<UpstreamDto> upstreamsAndTypes(Map<String, Object> arguments, String id);

    UpstreamDto updateUpstreamAndTypes(UpstreamDto upstream) throws Exception;


    List<UpstreamDto> findByDomainAndActiveIsFalse(String domainId);

    List<UpstreamDto> getUpstreams(String upstreamId, String domainId);

    List<UpstreamDto> findByOtherUpstream(List<String> ids,String domain);

    List<Upstream> findByUpstreamTypeIds(ArrayList<String> upstreamTypeIds,String domainId);

    Upstream findById(String upstreamId);

    Integer saveUpstreamTypesAndFields(List<UpstreamType> upstreamTypes, List<UpstreamType> updateUpstreamTypes, List<UpstreamTypeField> upstreamTypeFields, DomainInfo domainInfo);

    Integer saveUpstreamAboutNodes(List<Node> nodes, List<NodeRules> nodeRulesList, List<NodeRulesRange> nodeRulesRanges, DomainInfo domainInfo);

    void saveRoleBing(List<UpstreamType> upstreamTypes, List<Node> nodes, List<NodeRules> nodeRulesList, DomainInfo domainInfo);

    List<UpstreamDto> findByRecover(String domain);
}
