package com.qtgl.iga.service;

import com.qtgl.iga.bean.MergeAttrRule;
import com.qtgl.iga.bo.DomainInfo;

import java.util.List;

public interface MergeAttrRuleService {

    List<MergeAttrRule> mergeAttrRules(String userId, DomainInfo domainInfo);

    List<MergeAttrRule> findMergeAttrRulesByTenantId(String tenantId);

    List<MergeAttrRule> saveMergeAttrRule(List<MergeAttrRule> mergeAttrRules, DomainInfo domain);

    Integer deleteMergeAttrRuleByEntityIds(List<String> entityIds,String tenantId);
}
