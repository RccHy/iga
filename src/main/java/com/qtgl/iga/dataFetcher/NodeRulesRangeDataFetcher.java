package com.qtgl.iga.dataFetcher;


import com.alibaba.fastjson.JSON;
import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.bo.NodeRulesRange;
import com.qtgl.iga.service.NodeRulesRangeService;
import com.qtgl.iga.utils.CertifiedConnector;
import graphql.schema.DataFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class NodeRulesRangeDataFetcher {

    public static Logger logger = LoggerFactory.getLogger(NodeRulesRangeDataFetcher.class);


    @Autowired
    NodeRulesRangeService nodeRulesRangeService;


    public DataFetcher findNodeRulesRange() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            //2。解析查询参数+租户进行  进行查询
            return nodeRulesRangeService.findNodeRulesRange(arguments, domain.getId());
        };
    }


    public DataFetcher deleteNodeRulesRange() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            return nodeRulesRangeService.deleteNodeRulesRange(arguments, domain.getId());
        };
    }

    public DataFetcher saveNodeRulesRange() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            NodeRulesRange nodeRulesRange = JSON.parseObject(JSON.toJSONString(arguments.get("entity")), NodeRulesRange.class);
            NodeRulesRange data = nodeRulesRangeService.saveNodeRulesRange(nodeRulesRange, domain.getId());
            if (null != data) {
                return data;
            }
            throw new Exception("添加失败");
        };
    }

    public DataFetcher updateNodeRulesRange() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            NodeRulesRange nodeRulesRange = JSON.parseObject(JSON.toJSONString(arguments.get("entity")), NodeRulesRange.class);
            NodeRulesRange data = nodeRulesRangeService.updateNodeRulesRange(nodeRulesRange);
            if (null != data) {
                return data;
            }
            throw new Exception("修改失败");
        };
    }
}
