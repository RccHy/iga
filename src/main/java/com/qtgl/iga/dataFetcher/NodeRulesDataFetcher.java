package com.qtgl.iga.dataFetcher;


import com.alibaba.fastjson.JSON;
import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.bo.NodeRules;
import com.qtgl.iga.service.NodeRulesService;
import com.qtgl.iga.utils.CertifiedConnector;
import com.qtgl.iga.utils.exception.GraphqlExceptionUtils;
import com.qtgl.iga.utils.enumerate.ResultCode;
import com.qtgl.iga.utils.exception.CustomException;
import com.qtgl.iga.vo.NodeRulesVo;
import graphql.schema.DataFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class NodeRulesDataFetcher {

    public static Logger logger = LoggerFactory.getLogger(NodeRulesDataFetcher.class);


    @Autowired
    NodeRulesService nodeRulesService;

    /**
     * 查询节点规则
     *
     * @return
     */
    public DataFetcher findNodeRules() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            //2。解析查询参数+租户进行  进行查询
            try {
                List<NodeRules> nodeRules = nodeRulesService.findNodeRules(arguments, domain.getId());
                return nodeRules;
            } catch (CustomException e) {
                e.printStackTrace();
                logger.error(domain.getDomainName() + e.getMessage());

                return GraphqlExceptionUtils.getObject("查询节点规则失败", e);
            }
        };
    }

    /**
     * 删除节点规则
     *
     * @return
     */
    public DataFetcher deleteRules() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            try {
                NodeRules nodeRules = nodeRulesService.deleteRules(arguments, domain.getId());
                return nodeRules;
            } catch (CustomException e) {
                e.printStackTrace();
                logger.error(domain.getDomainName() + e.getMessage());

                return GraphqlExceptionUtils.getObject("删除节点规则失败", e);
            }
        };
    }

    /**
     * 添加节点规则
     *
     * @return
     */
    public DataFetcher saveRules() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            NodeRulesVo nodeRules = JSON.parseObject(JSON.toJSONString(arguments.get("entity")), NodeRulesVo.class);
            try {
                NodeRulesVo data = nodeRulesService.saveRules(nodeRules);
                if (null != data) {
                    return data;
                }
                throw new CustomException(ResultCode.FAILED, "添加节点规则失败");
            } catch (CustomException e) {
                e.printStackTrace();
                logger.error(domain.getDomainName() + e.getMessage());

                return GraphqlExceptionUtils.getObject("添加节点规则失败", e);
            }
        };
    }

    /**
     * 修改节点规则
     *
     * @return
     */
    public DataFetcher updateRules() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            NodeRulesVo nodeRules = JSON.parseObject(JSON.toJSONString(arguments.get("entity")), NodeRulesVo.class);
            try {
                NodeRulesVo data = nodeRulesService.updateRules(nodeRules);
                if (null != data) {
                    return data;
                }
                throw new CustomException(ResultCode.FAILED, "修改节点规则失败");
            } catch (CustomException e) {
                e.printStackTrace();

                logger.error(domain.getDomainName() + e.getMessage());

                return GraphqlExceptionUtils.getObject("修改节点规则失败", e);
            }
        };
    }

    /**
     * 批量删除规则
     *
     * @return
     */
    public DataFetcher deleteBatchRules() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            try {
                List<NodeRulesVo> nodeRulesVos = nodeRulesService.deleteBatchRules(arguments, domain.getId());
                return nodeRulesVos;
            } catch (CustomException e) {
                e.printStackTrace();
                logger.error(domain.getDomainName() + e.getMessage());

                return GraphqlExceptionUtils.getObject("删除节点规则失败", e);
            }
        };
    }
}
