package com.qtgl.iga.dataFetcher;

import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.service.UpstreamDataStateService;
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
public class UpstreamDataStateFetcher {

    public static Logger logger = LoggerFactory.getLogger(UpstreamDataStateFetcher.class);


    @Resource
    UpstreamDataStateService upstreamDataStatusService;

    public DataFetcher personUpstreamDataState() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            //2。解析查询参数+租户进行  进行查询
            try {
                return upstreamDataStatusService.personUpstreamDataState(arguments, domain.getId());
            } catch (CustomException e) {
                e.printStackTrace();
                logger.error(domain.getDomainName() + e.getMessage());

                return GraphqlExceptionUtils.getObject("查询上游人员同步数据状态失败", e);
            }
        };
    }


    public DataFetcher occupyUpstreamDataState() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            //2。解析查询参数+租户进行  进行查询
            try {
                return upstreamDataStatusService.occupyUpstreamDataState(arguments, domain.getId());
            } catch (CustomException e) {
                e.printStackTrace();
                logger.error(domain.getDomainName() + e.getMessage());

                return GraphqlExceptionUtils.getObject("查询上游身份同步数据状态失败", e);
            }
        };
    }
}


