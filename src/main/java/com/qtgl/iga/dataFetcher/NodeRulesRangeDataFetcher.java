package com.qtgl.iga.dataFetcher;


import com.alibaba.fastjson.JSON;
import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.bo.NodeRulesRange;
import com.qtgl.iga.service.NodeRulesRangeService;
import com.qtgl.iga.utils.CertifiedConnector;
import com.qtgl.iga.utils.exception.GraphqlExceptionUtils;
import com.qtgl.iga.utils.enumerate.ResultCode;
import com.qtgl.iga.utils.exception.CustomException;
import graphql.schema.DataFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class NodeRulesRangeDataFetcher {

    public static Logger logger = LoggerFactory.getLogger(NodeRulesRangeDataFetcher.class);


    @Autowired
    NodeRulesRangeService nodeRulesRangeService;

    /**
     * 查询节点规则作用域
     *
     * @return
     */
    public DataFetcher findNodeRulesRange() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            //2。解析查询参数+租户进行  进行查询
            try {
                List<NodeRulesRange> nodeRulesRange = nodeRulesRangeService.findNodeRulesRange(arguments, domain.getId());
                return nodeRulesRange;
            } catch (CustomException e) {
                e.printStackTrace();
                logger.error(domain.getDomainName() + e.getMessage());

                return GraphqlExceptionUtils.getObject("查询节点规则作用域失败", e);
            }
        };
    }

    /**
     * 删除节点规则作用域
     *
     * @return
     */
    public DataFetcher deleteNodeRulesRange() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            try {
                NodeRulesRange nodeRulesRange = nodeRulesRangeService.deleteNodeRulesRange(arguments, domain.getId());
                return nodeRulesRange;
            } catch (CustomException e) {
                e.printStackTrace();
                logger.error(domain.getDomainName() + e.getMessage());

                return GraphqlExceptionUtils.getObject("删除节点规则作用域失败", e);
            }
        };
    }

    /**
     * 添加节点规则作用域
     *
     * @return
     */
    public DataFetcher saveNodeRulesRange() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            NodeRulesRange nodeRulesRange = JSON.parseObject(JSON.toJSONString(arguments.get("entity")), NodeRulesRange.class);
            try {
                NodeRulesRange data = nodeRulesRangeService.saveNodeRulesRange(nodeRulesRange, domain.getId());
                if (null != data) {
                    return data;
                }
                throw new CustomException(ResultCode.FAILED, "添加节点规则作用域失败");
            } catch (CustomException e) {
                e.printStackTrace();
                logger.error(domain.getDomainName() + e.getMessage());

                return GraphqlExceptionUtils.getObject("添加节点规则作用域失败", e);
            }
        };
    }

    /**
     * 修改节点规则作用域
     *
     * @return
     */
    public DataFetcher updateNodeRulesRange() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            NodeRulesRange nodeRulesRange = JSON.parseObject(JSON.toJSONString(arguments.get("entity")), NodeRulesRange.class);
            try {
                NodeRulesRange data = nodeRulesRangeService.updateNodeRulesRange(nodeRulesRange);
                if (null != data) {
                    return data;
                }
                throw new CustomException(ResultCode.FAILED, "修改节点规则作用域失败");
            } catch (CustomException e) {
                e.printStackTrace();
                logger.error(domain.getDomainName() + e.getMessage());

                return GraphqlExceptionUtils.getObject("修改节点规则作用域失败", e);
            }
        };
    }
}
