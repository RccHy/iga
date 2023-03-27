package com.qtgl.iga.dao;

import com.qtgl.iga.bean.MergeAttrRule;

import java.util.List;

public interface MergeAttrRuleDao {
    List<MergeAttrRule> findMergeAttrRules(String userId, String id);

    List<MergeAttrRule> findMergeAttrRulesByTenantId(String tenantId);

    List<MergeAttrRule> saveMergeAttrRule(List<MergeAttrRule> mergeAttrRules, String id);

    Integer deleteMergeAttrRuleByEntityIds(List<String> entityIds, String tenantId);
}
