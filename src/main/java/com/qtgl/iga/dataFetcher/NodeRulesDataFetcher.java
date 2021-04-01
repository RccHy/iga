package com.qtgl.iga.dataFetcher;


import com.alibaba.fastjson.JSON;
import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.bo.NodeRules;
import com.qtgl.iga.service.NodeRulesService;
import com.qtgl.iga.utils.CertifiedConnector;
import com.qtgl.iga.vo.NodeRulesVo;
import graphql.schema.DataFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class NodeRulesDataFetcher {

    public static Logger logger = LoggerFactory.getLogger(NodeRulesDataFetcher.class);


    @Autowired
    NodeRulesService nodeRulesService;


    public DataFetcher findNodeRules() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            //2。解析查询参数+租户进行  进行查询
            return nodeRulesService.findNodeRules(arguments, domain.getId());
        };
    }


    public DataFetcher deleteRules() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            return nodeRulesService.deleteRules(arguments, domain.getId());
        };
    }

    public DataFetcher saveRules() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            NodeRulesVo nodeRules = JSON.parseObject(JSON.toJSONString(arguments.get("entity")), NodeRulesVo.class);
            NodeRulesVo data = nodeRulesService.saveRules(nodeRules);
            if (null != data) {
                return data;
            }
            throw new Exception("添加失败");
        };
    }

    public DataFetcher updateRules() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            NodeRulesVo nodeRules = JSON.parseObject(JSON.toJSONString(arguments.get("entity")), NodeRulesVo.class);
            NodeRulesVo data = nodeRulesService.updateRules(nodeRules);
            if (null != data) {
                return data;
            }
            throw new Exception("修改失败");
        };
    }

    public DataFetcher deleteBatchRules() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            return nodeRulesService.deleteBatchRules(arguments, domain.getId());
        };
    }
}
