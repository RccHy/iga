package com.qtgl.iga.dataFetcher;


import com.alibaba.fastjson.JSON;
import com.qtgl.iga.bo.DomainIgnore;
import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.service.DomainIgnoreService;
import com.qtgl.iga.utils.CertifiedConnector;
import com.qtgl.iga.utils.exception.CustomException;
import com.qtgl.iga.utils.exception.GraphqlExceptionUtils;
import graphql.schema.DataFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;

@Component
public class DomainIgnoreFetcher {
    public static Logger logger = LoggerFactory.getLogger(DomainIgnoreFetcher.class);

    @Resource
    DomainIgnoreService ignoreService;


    public DataFetcher recoverUpstreamOrRule() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            DomainIgnore domainIgnore = JSON.parseObject(JSON.toJSONString(arguments), DomainIgnore.class);

            try {
                return ignoreService.recoverUpstreamOrRule(domainIgnore, domain.getId());

            } catch (CustomException e) {
                e.printStackTrace();
                logger.error(domain.getDomainName() + e.getErrorMsg());

                return GraphqlExceptionUtils.getObject("恢复失败", e);


            }
        };
    }
}
