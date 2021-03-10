package com.qtgl.iga.dataFetcher;


import com.alibaba.fastjson.JSON;
import com.qtgl.iga.bo.DomainInfo;

import com.qtgl.iga.bo.UpstreamType;
import com.qtgl.iga.service.UpstreamTypeService;
import com.qtgl.iga.utils.CertifiedConnector;
import graphql.schema.DataFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class UpstreamTypeFetcher {

    public static Logger logger = LoggerFactory.getLogger(UpstreamTypeFetcher.class);


    @Autowired
    UpstreamTypeService upStreamTypeService;


    public DataFetcher upstreamTypes() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            //2。解析查询参数  进行查询
            return upStreamTypeService.findAll(arguments, domain.getId());
        };
    }

    public DataFetcher deleteUpstreamType() throws Exception {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            return upStreamTypeService.deleteUpstreamType(arguments, domain.getId());
        };
    }

    public DataFetcher saveUpstreamType() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();

            UpstreamType upstreamType = JSON.parseObject(JSON.toJSONString(arguments.get("entity")), UpstreamType.class);

            UpstreamType data = upStreamTypeService.saveUpstreamType(upstreamType, domain.getId());
            if (null != data) {
                return data;
            }
            throw new Exception("添加失败");
        };
    }

    public DataFetcher updateUpstreamType() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            UpstreamType upstreamType = JSON.parseObject(JSON.toJSONString(arguments.get("entity")), UpstreamType.class);
            upstreamType.setDomain(domain.getId());
            UpstreamType data = upStreamTypeService.updateUpstreamType(upstreamType);
            if (null != data) {
                return data;
            }
            throw new Exception("修改失败");
        };
    }
}
