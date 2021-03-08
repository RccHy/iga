package com.qtgl.iga.dataFetcher;


import com.alibaba.fastjson.JSON;
import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.bo.UpStream;
import com.qtgl.iga.service.UpStreamService;
import com.qtgl.iga.utils.CertifiedConnector;
import graphql.schema.DataFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class UpStreamFetcher {

    public static Logger logger = LoggerFactory.getLogger(UpStreamFetcher.class);


    @Autowired
    UpStreamService upStreamService;


    public DataFetcher upStreams() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            //2。解析查询参数  进行查询
            return upStreamService.findAll(arguments,domain.getId());
        };
    }

    public DataFetcher deleteUpStream() throws Exception {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            return upStreamService.deleteUpStream(arguments,domain.getId());
        };
    }

    public DataFetcher saveUpStream() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            UpStream upStream = JSON.parseObject(JSON.toJSONString(arguments.get("entity")), UpStream.class);
            if (null != upStreamService.saveUpStream(upStream,domain.getId())) {
                return upStreamService.saveUpStream(upStream,domain.getId());
            }
            throw new Exception("添加失败");
        };
    }

    public DataFetcher updateUpStream() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            UpStream upStream = JSON.parseObject(JSON.toJSONString(arguments.get("entity")), UpStream.class);
            upStream.setDomain(domain.getId());
            if (null != upStreamService.updateUpStream(upStream)) {
                return upStreamService.updateUpStream(upStream);
            }
            throw new Exception("修改失败");
        };
    }
}
