package com.qtgl.iga.dataFetcher;


import com.alibaba.druid.util.StringUtils;
import com.qtgl.iga.bean.TreeBean;
import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.utils.CertifiedConnector;
import com.qtgl.iga.utils.exception.CustomException;
import com.qtgl.iga.utils.exception.GraphqlExceptionUtils;
import graphql.schema.DataFetcher;

import lombok.extern.log4j.Log4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Log4j
public class CheckFetcher {


    @Value("${sso.token.url}")
    private String tokenUrl;
    @Value("${sso.introspect.url}")
    private String introspectUrl;
    @Value("${bus.url}")
    private String  busUrl;
    @Value("${server.origin}")
    private String  serverOrigin;
    @Value("${file.url}")
    private String  fileUrl;

    private String DOMAIN_CHECK = "租户";
    private String TOKEN_URL_CHECK = "OAUTH_TOKEN_URL";
    private String INTROSPECT_URL_CHECK = "OAUTH_INTROSPECT_URL";
    private String BUS_URL_CHECK = "BUS_URL";

    public DataFetcher checkList() {
        return dataFetchingEvn -> {
            List<Map> checkList = new ArrayList<>();
            // 环境变量
            //-------------------------------
            Map<String, Object>  tokenUrlCheck= new HashMap<>();
            // 如果tokenUrl为空 或者不包含 /sso/oauth2/token 则报错
            tokenUrlCheck.put("name", TOKEN_URL_CHECK);
            tokenUrlCheck.put("desc", tokenUrl);
            if (StringUtils.isEmpty(tokenUrl) || !tokenUrl.contains("/sso/oauth2/token")) {
                tokenUrlCheck.put("state", "error");
            } else {
                tokenUrlCheck.put("state", "success");
            }
            //-------------------------------
            Map<String, Object>  introspectUrlCheck= new HashMap<>();
            // 如果introspectUrl为空 或者不包含 /sso/oauth2/introspect 则报错
            introspectUrlCheck.put("name", INTROSPECT_URL_CHECK);
            introspectUrlCheck.put("desc", introspectUrl);
            if (StringUtils.isEmpty(introspectUrl) || !introspectUrl.contains("/sso/oauth2/introspect")) {
                introspectUrlCheck.put("state", "error");
            } else {
                introspectUrlCheck.put("state", "success");
            }
            //-------------------------------
            Map<String, Object>  busUrlCheck= new HashMap<>();
            // 如果busUrl为空 或者不包含 /bus/graphql/builtin 则报错
            busUrlCheck.put("name", BUS_URL_CHECK);
            busUrlCheck.put("desc", busUrl);
            if (StringUtils.isEmpty(busUrl) || !busUrl.contains("/bus/graphql/builtin")) {
                busUrlCheck.put("state",  "error");
            } else {
                busUrlCheck.put("state", "success");
            }

            //-------------------------------




            ///
            Map<String, Object> domainCheck = new HashMap<>();
            try {
                //1。更具token信息验证是否合法，并判断其租户
                DomainInfo domain = CertifiedConnector.getDomain();
                domainCheck.put("name", DOMAIN_CHECK);
                domainCheck.put("desc", domain.getDomainName());
            } catch (Exception e) {
                domainCheck.put("desc", e.getMessage());
                domainCheck.put("state", "success");
            }
            domainCheck.put("state",  "error");


            return checkList;

        };
    }
}
