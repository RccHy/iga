package com.qtgl.iga.dataFetcher;


import com.alibaba.fastjson.JSON;
import com.qtgl.iga.bean.NodeDto;
import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.bo.Node;
import com.qtgl.iga.service.NodeService;
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
public class NodeDataFetcher {

    public static Logger logger = LoggerFactory.getLogger(NodeDataFetcher.class);


    @Autowired
    NodeService nodeService;

    /**
     * 查询节点
     *
     * @return
     */
    public DataFetcher findNodes() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            //2。解析查询参数+租户进行  进行查询
            try {
                List<NodeDto> nodes = nodeService.findNodes(arguments, domain.getId(), true);
                return nodes;
            } catch (CustomException e) {
                e.printStackTrace();
                logger.error(domain.getDomainName() + e.getMessage());

                return GraphqlExceptionUtils.getObject("查询节点失败", e);
            }
        };
    }

    /**
     * 删除节点
     *
     * @return
     */
    public DataFetcher deleteNode() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            try {
                NodeDto nodeDto = nodeService.deleteNode(arguments, domain.getId());
                return nodeDto;
            } catch (CustomException e) {
                e.printStackTrace();
                logger.error(domain.getDomainName() + e.getMessage());

                return GraphqlExceptionUtils.getObject("删除节点失败", e);
            }
        };
    }

    /**
     * 添加节点
     *
     * @return
     */
    public DataFetcher saveNode() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            NodeDto nodeDto = JSON.parseObject(JSON.toJSONString(arguments.get("entity")), NodeDto.class);
            try {
                NodeDto data = nodeService.saveNode(nodeDto, domain.getId());
                if (null != data) {
                    return data;
                }
                throw new CustomException(ResultCode.FAILED, "添加节点失败");
            } catch (CustomException e) {
                e.printStackTrace();
                logger.error(domain.getDomainName() + e.getMessage());

                return GraphqlExceptionUtils.getObject("添加节点失败", e);
            }
        };
    }

    /**
     * 修改节点
     *
     * @return
     */
    public DataFetcher updateNode() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            NodeDto nodeDto = JSON.parseObject(JSON.toJSONString(arguments.get("entity")), NodeDto.class);
            nodeDto.setDomain(domain.getId());
            try {
                NodeDto data = nodeService.updateNode(nodeDto);
                if (null != data) {
                    return data;
                }
                throw new CustomException(ResultCode.FAILED, "修改节点失败");
            } catch (CustomException e) {
                e.printStackTrace();
                logger.error(domain.getDomainName() + e.getMessage());

                return GraphqlExceptionUtils.getObject("修改节点失败", e);
            }
        };
    }

    /***
     * 查询多个节点规则
     * @return
     */
    public DataFetcher findNodesPlus() {

        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            //2。解析查询参数+租户进行  进行查询
            try {
                List<NodeDto> nodesPlus = nodeService.findNodesPlus(arguments, domain.getId());
                return nodesPlus;
            } catch (CustomException e) {
                e.printStackTrace();
                logger.error(domain.getDomainName() + e.getMessage());

                return GraphqlExceptionUtils.getObject("查询节点失败", e);
            }
        };
    }

    /**
     * 应用版本方法
     *
     * @return
     */
    public DataFetcher applyNode() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            try {
                Node node = nodeService.applyNode(arguments, domain.getId());
                if (null != node) {
                    return node;
                }
                Boolean mark = (Boolean) arguments.get("mark");
                //mark为true 则为回滚  false为应用
                if (mark) {
                    throw new CustomException(ResultCode.FAILED, "回滚失败");
                } else {
                    throw new CustomException(ResultCode.FAILED, "应用失败");
                }
            } catch (CustomException e) {
                e.printStackTrace();
                logger.error(domain.getDomainName() + e.getMessage());

                return GraphqlExceptionUtils.getObject("操作失败", e);
            }
        };
    }

    /**
     * 弃用方法
     *
     * @return
     */
    public DataFetcher rollbackNode() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            return nodeService.rollbackNode(arguments, domain.getId());
        };
    }

    /***
     * 根据状态类型查询是否有数据
     * @return
     */
    public DataFetcher nodeStatus() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            //2。解析查询参数+租户进行  进行查询
            return nodeService.nodeStatus(arguments, domain.getId());
        };
    }
}
