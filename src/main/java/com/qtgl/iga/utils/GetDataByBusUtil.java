package com.qtgl.iga.utils;


import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.bo.UpstreamTypeField;
import com.qtgl.iga.service.UpstreamTypeService;
import com.qtgl.iga.service.impl.UpstreamTypeServiceImpl;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.oltu.oauth2.client.OAuthClient;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.response.OAuthJSONAccessTokenResponse;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.mountcloud.graphql.GraphqlClient;
import org.mountcloud.graphql.request.query.DefaultGraphqlQuery;
import org.mountcloud.graphql.request.query.GraphqlQuery;
import org.mountcloud.graphql.request.result.ResultAttributtes;
import org.mountcloud.graphql.response.GraphqlResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <FileName> GetDataByBusUtil
 * <Desc> 通过url获取数据
 **/
@Component
public class GetDataByBusUtil {

    @Autowired
    UpstreamTypeService upstreamTypeService;

    @Value("${app.scope}")
    private String appScope;
    @Value("${sso.url}")
    private String ssoUrl;
    @Value("${app.client}")
    private String appKey;
    @Value("${app.secret}")
    private String appSecret;
    @Value("${bus.url}")
    private String busUrl ;
    @Value("${graphql.url}")
    private String graphqlUrl;

    private static ConcurrentHashMap<String, Token> tokenMap = new ConcurrentHashMap<>();


    public Object getDataByBus(String url) throws Exception {
//        //获取token
        String key = getToken();
       // String key = "8a5ebcef3ace2ec4b93b0bf34bcdd567";
        String[] split = url.split("/");
        for (String s : split) {
            System.out.println(s);
        }
        //根据url 获取请求地址
        String substring = new StringBuffer(ssoUrl).replace(ssoUrl.length() - 4, ssoUrl.length(), busUrl).
                append(graphqlUrl).append("/").append(split[2]).append("/").append("?access_token=").append(key).toString();

        //工具类过滤处理url
        String dealUrl = UrlUtil.getUrl(substring);


        //调用获取资源url
        String dataUrl = invokeUrl(dealUrl);
//        String dataUrl="http://localhost:8080/iga/graphql?access_token="+key;
        //请求获取资源

        return invokeForData(dataUrl, url);
    }

    private String getToken() throws Exception {
        //判断是否已有未过期token
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        StringBuffer url = request.getRequestURL();
        String tempContextUrl = url.delete(url.length() - request.getRequestURI().length(), url.length()).append("/").toString();

        Token token = tokenMap.get(tempContextUrl);
        if (null != token) {
            int i = token.getExpireIn().compareTo(System.currentTimeMillis() + 3000);
            if (i == 1) {
                return token.getToken();
            }
        }
        Object[] objects = Arrays.stream(appScope.replace("+", " ").split(" ")).filter(s -> s.contains("sys_")).toArray();
        String scope = ArrayUtils.toString(objects, ",").replace("{", "").replace("}", "");
        OAuthClientRequest oAuthClientRequest = OAuthClientRequest
                .tokenLocation(ssoUrl + "/oauth2/token").setGrantType(GrantType.CLIENT_CREDENTIALS)
                .setClientId(appKey).setClientSecret(appSecret)
                .setScope(scope.replace(",", " ")).buildBodyMessage();
        OAuthClient oAuthClient = new OAuthClient(new SSLConnectionClient());
        OAuthJSONAccessTokenResponse oAuthClientResponse = (OAuthJSONAccessTokenResponse) oAuthClient.accessToken(oAuthClientRequest, "POST", OAuthJSONAccessTokenResponse.class);
        String accessToken = oAuthClientResponse.getAccessToken();
        long exp =System.currentTimeMillis() +( oAuthClientResponse.getExpiresIn() - (10 * 60 * 1000));
        tokenMap.put(tempContextUrl, new Token(oAuthClientResponse.getAccessToken(), exp, System.currentTimeMillis()));
        return accessToken;
    }

    private String invokeUrl(String url) throws IOException {
        GraphqlClient graphqlClient = GraphqlClient.buildGraphqlClient(url);
        String queryMethodName = "services";
        GraphqlQuery query = new DefaultGraphqlQuery(queryMethodName);
        HashMap<String, HashMap<String, Object>> map = new HashMap<>();
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("like", "canvas-hub");
        map.put("name", hashMap);
        query.addParameter("filter", map);
        query.addResultAttributes("id", "name", "graph");
        ResultAttributtes endpoints = new ResultAttributtes("endpoints");
        endpoints.addResultAttributes("endPoint");
        query.addResultAttributes(endpoints);
        GraphqlResponse response = graphqlClient.doQuery(query);
        //获取数据，数据为map类型
        Map<String, Object> result = response.getData();
        for (Map.Entry<String, Object> entry : result.entrySet()) {
            System.out.println(entry.getKey() + "------" + entry.getValue());
        }

        return "";
    }

    private Map invokeForData(String dataUrl, String url) throws IOException {
        //获取字段映射
//        List<UpstreamTypeField> fields = upstreamTypeService.findFields(url);
        String[] type = url.split("/");
        GraphqlClient graphqlClient = GraphqlClient.buildGraphqlClient(dataUrl);
        String methodName = type[5];
        Map<String, Object> result = null;
        if ("query".equals(type[4])) {
            GraphqlQuery query = new DefaultGraphqlQuery(methodName);

//            for (UpstreamTypeField field : fields) {
//                query.addResultAttributes(field.getTargetField() + ":" + field.getSourceField());
//            }

            query.addResultAttributes("A:id");



            GraphqlResponse response = graphqlClient.doQuery(query);
            //获取数据，数据为map类型
            result = response.getData();
            for (Map.Entry<String, Object> entry : result.entrySet()) {
                System.out.println(entry.getKey() + "------" + entry.getValue());
            }
        }
        if ("mutation".equals(methodName)) {

        }

        return result;
    }


}
