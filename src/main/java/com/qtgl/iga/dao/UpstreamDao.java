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

    Upstream findByCodeAndDomain(String code, String domain);

    ArrayList<Upstream> getUpstreams(String id, String domain);

    Integer saveUpstreamTypesAndFields(List<UpstreamType> upstreamTypes, List<UpstreamType> updateUpstreamTypes, List<UpstreamTypeField> upstreamTypeFields, DomainInfo domainInfo);

    Integer saveUpstreamAbountNodes(List<Node> nodes, List<NodeRules> nodeRulesList, List<NodeRulesRange> nodeRulesRangeList, DomainInfo domainInfo);

    Integer delAboutNode(Upstream upstream, DomainInfo domainInfo) throws Exception;

    List<Map<String, Object>> findByAppNameAndAppCode(String appName, String appCode, String domain);

    ArrayList<Upstream> findByDomainAndActiveIsFalse(String id);

    ArrayList<Upstream> findByOtherUpstream(List<String> ids, String domain);

    List<Upstream> findByUpstreamTypeIds(ArrayList<String> ids, String domainId);

    List<Upstream> findByRecover(String domain);

    List<Upstream> findByDomainAndIgnore(String domainId);
}
