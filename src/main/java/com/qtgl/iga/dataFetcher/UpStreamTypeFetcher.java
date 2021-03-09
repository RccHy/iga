package com.qtgl.iga.dataFetcher;


import com.alibaba.fastjson.JSON;
import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.bo.UpStream;
import com.qtgl.iga.bo.UpStreamType;
import com.qtgl.iga.service.UpStreamService;
import com.qtgl.iga.service.UpStreamTypeService;
import com.qtgl.iga.utils.CertifiedConnector;
import graphql.schema.DataFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class UpStreamTypeFetcher {

    public static Logger logger = LoggerFactory.getLogger(UpStreamTypeFetcher.class);


    @Autowired
    UpStreamTypeService upStreamTypeService;


    public DataFetcher upStreamTypes() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            //2。解析查询参数  进行查询
            return upStreamTypeService.findAll(arguments, domain.getId());
        };
    }

    public DataFetcher deleteUpStreamType() throws Exception {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            return upStreamTypeService.deleteUpStreamType(arguments, domain.getId());
        };
    }

    public DataFetcher saveUpStreamType() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();

            UpStreamType upStreamType = JSON.parseObject(JSON.toJSONString(arguments.get("entity")), UpStreamType.class);

            UpStreamType data = upStreamTypeService.saveUpStreamType(upStreamType, domain.getId());
            if (null != data) {
                return data;
            }
            throw new Exception("添加失败");
        };
    }

    public DataFetcher updateUpStreamType() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            UpStreamType upStreamType = JSON.parseObject(JSON.toJSONString(arguments.get("entity")), UpStreamType.class);
            upStreamType.setDomain(domain.getId());
            UpStreamType data = upStreamTypeService.updateUpStreamType(upStreamType);
            if (null != data) {
                return data;
            }
            throw new Exception("修改失败");
        };
    }
}
