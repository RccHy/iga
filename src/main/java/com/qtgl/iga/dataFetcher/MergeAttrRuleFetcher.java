package com.qtgl.iga.dataFetcher;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.qtgl.iga.bean.MergeAttrRule;
import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.service.MergeAttrRuleService;
import com.qtgl.iga.utils.CertifiedConnector;
import com.qtgl.iga.utils.exception.CustomException;
import com.qtgl.iga.utils.exception.GraphqlExceptionUtils;
import graphql.schema.DataFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class MergeAttrRuleFetcher {
    public static Logger logger = LoggerFactory.getLogger(MergeAttrRuleFetcher.class);

    @Resource
    MergeAttrRuleService mergeAttrRuleService;

    public DataFetcher mergeAttrRules() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            String userId = null == arguments.get("userId") ? null : (String) arguments.get("userId");
            try {
                return mergeAttrRuleService.mergeAttrRules(userId, domain);
            } catch (CustomException e) {
                e.printStackTrace();
                logger.error(domain.getDomainName() + e.getErrorMsg());
                return GraphqlExceptionUtils.getObject("查询手工合重数据失败", e);

            }
        };
    }

    public DataFetcher saveMergeAttrRule() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            List<MergeAttrRule> mergeAttrRules = getMergeAttrRules(arguments);

            try {
                return mergeAttrRuleService.saveMergeAttrRule(mergeAttrRules, domain);
            } catch (CustomException e) {
                e.printStackTrace();
                logger.error(domain.getDomainName() + e.getErrorMsg());
                return GraphqlExceptionUtils.getObject("添加手工合重数据失败", e);

            }
        };
    }


    public DataFetcher deleteMergeAttrRule() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            String userId = null == arguments.get("userId") ? null : (String) arguments.get("userId");
            try {
                return mergeAttrRuleService.deleteMergeAttrRuleByEntityId(userId, domain);
            } catch (CustomException e) {
                e.printStackTrace();
                logger.error(domain.getDomainName() + e.getErrorMsg());
                return GraphqlExceptionUtils.getObject("删除手工合重数据失败", e);

            }
        };
    }

    private List<MergeAttrRule> getMergeAttrRules(Map<String, Object> arguments) {
        JSONArray jsonArray = JSON.parseArray(JSON.toJSONString(arguments.get("entity")));
        List<MergeAttrRule> mergeAttrRules = new ArrayList<>();
        for (Object o : jsonArray) {
            MergeAttrRule mergeAttrRule = JSON.parseObject(JSON.toJSONString(o), MergeAttrRule.class);
            mergeAttrRules.add(mergeAttrRule);
        }
        return mergeAttrRules;
    }
}
