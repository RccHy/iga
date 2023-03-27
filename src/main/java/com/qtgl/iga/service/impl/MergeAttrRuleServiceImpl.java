package com.qtgl.iga.service.impl;

import com.qtgl.iga.bean.MergeAttrRule;
import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.bo.Tenant;
import com.qtgl.iga.dao.MergeAttrRuleDao;
import com.qtgl.iga.service.MergeAttrRuleService;
import com.qtgl.iga.service.TenantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class MergeAttrRuleServiceImpl implements MergeAttrRuleService {

    @Resource
    MergeAttrRuleDao mergeAttrRuleDao;
    @Resource
    TenantService tenantService;


    @Override
    public List<MergeAttrRule> mergeAttrRules(String userId, DomainInfo domain) {
        //根据domain获取tenantId
        Tenant tenant = tenantService.findByDomainName(domain.getDomainName());

        return mergeAttrRuleDao.findMergeAttrRules(userId, tenant.getId());
    }

    @Override
    public List<MergeAttrRule> findMergeAttrRulesByTenantId(String tenantId) {
        return mergeAttrRuleDao.findMergeAttrRulesByTenantId(tenantId);
    }

    @Override
    public List<MergeAttrRule> saveMergeAttrRule(List<MergeAttrRule> mergeAttrRules, DomainInfo domain) {
        if (!CollectionUtils.isEmpty(mergeAttrRules)) {
            //根据domain获取tenantId
            Tenant tenant = tenantService.findByDomainName(domain.getDomainName());
            //判断是否作用于同一人

            //根据entity_id 删除之前的数据
            Set<String> ids = mergeAttrRules.stream().collect(Collectors.groupingBy(MergeAttrRule::getEntityId)).keySet();
            deleteMergeAttrRuleByEntityIds(new ArrayList<>(ids), tenant.getId());
            //新增合重基础属性
            return mergeAttrRuleDao.saveMergeAttrRule(mergeAttrRules, tenant.getId());
        }
        return null;
    }

    @Override
    public Integer deleteMergeAttrRuleByEntityIds(List<String> entityIds, String tenantId) {
        return mergeAttrRuleDao.deleteMergeAttrRuleByEntityIds(entityIds, tenantId);
    }

    @Override
    public List<MergeAttrRule> deleteMergeAttrRuleByEntityId(String userId, DomainInfo domain) {
        //根据domain获取tenantId
        Tenant tenant = tenantService.findByDomainName(domain.getDomainName());
        List<MergeAttrRule> mergeAttrRules = mergeAttrRuleDao.findMergeAttrRules(userId, tenant.getId());
        if(!CollectionUtils.isEmpty(mergeAttrRules)){
            mergeAttrRuleDao.deleteMergeAttrRuleByEntityId(userId, tenant.getId());
        }
        return mergeAttrRules;
    }
}
