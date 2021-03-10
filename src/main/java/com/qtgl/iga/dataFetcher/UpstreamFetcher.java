package com.qtgl.iga.dataFetcher;


import com.alibaba.fastjson.JSON;
import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.bo.Upstream;
import com.qtgl.iga.service.UpstreamService;
import com.qtgl.iga.utils.CertifiedConnector;
import graphql.schema.DataFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class UpstreamFetcher {

    public static Logger logger = LoggerFactory.getLogger(UpstreamFetcher.class);


    @Autowired
    UpstreamService upstreamService;


    public DataFetcher upstreams() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            //2。解析查询参数  进行查询
            return upstreamService.findAll(arguments, domain.getId());
        };
    }

    public DataFetcher deleteUpstream() throws Exception {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            return upstreamService.deleteUpstream(arguments, domain.getId());
        };
    }

    public DataFetcher saveUpstream() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            Upstream upstream = JSON.parseObject(JSON.toJSONString(arguments.get("entity")), Upstream.class);
            Upstream data = upstreamService.saveUpstream(upstream, domain.getId());
            if (null != data) {
                return data;
            }
            throw new Exception("添加失败");
        };
    }

    public DataFetcher updateUpstream() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            Upstream upstream = JSON.parseObject(JSON.toJSONString(arguments.get("entity")), Upstream.class);
            upstream.setDomain(domain.getId());
            Upstream data = upstreamService.updateUpstream(upstream);
            if (null != data) {
                return data;
            }
            throw new Exception("修改失败");
        };
    }
}
