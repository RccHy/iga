package com.qtgl.iga.dataFetcher;


import com.alibaba.fastjson.JSON;
import com.qtgl.iga.bean.NodeDto;
import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.service.NodeService;
import com.qtgl.iga.utils.CertifiedConnector;
import graphql.schema.DataFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class NodeDataFetcher {

    public static Logger logger = LoggerFactory.getLogger(NodeDataFetcher.class);


    @Autowired
    NodeService nodeService;


    public DataFetcher findNodes() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            //2。解析查询参数+租户进行  进行查询
            return nodeService.findNodes(arguments, domain.getId());
        };
    }


    public DataFetcher deleteNode() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            return nodeService.deleteNode(arguments, domain.getId());
        };
    }

    public DataFetcher saveNode() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            NodeDto nodeDto = JSON.parseObject(JSON.toJSONString(arguments.get("entity")), NodeDto.class);
            NodeDto data = nodeService.saveNode(nodeDto, domain.getId());
            if (null != data) {
                return data;
            }
            throw new Exception("添加失败");
        };
    }

    public DataFetcher updateNode() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            NodeDto nodeDto = JSON.parseObject(JSON.toJSONString(arguments.get("entity")), NodeDto.class);
            nodeDto.setDomain(domain.getId());
            NodeDto data = nodeService.updateNode(nodeDto);
            if (null != data) {
                return data;
            }
            throw new Exception("修改失败");
        };
    }

    public DataFetcher findNodesPlus() {

        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            //2。解析查询参数+租户进行  进行查询
            return nodeService.findNodesPlus(arguments, domain.getId());
        };
    }

    public DataFetcher applyNode() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            return nodeService.applyNode(arguments, domain.getId());
        };
    }

    public DataFetcher rollbackNode() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            return nodeService.rollbackNode(arguments, domain.getId());
        };
    }

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
