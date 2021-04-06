package com.qtgl.iga.utils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.bo.Token;
import com.qtgl.iga.bo.UpstreamType;
import com.qtgl.iga.bo.UpstreamTypeField;
import com.qtgl.iga.service.DomainInfoService;
import com.qtgl.iga.service.UpstreamTypeService;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.apache.oltu.oauth2.client.OAuthClient;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.response.OAuthJSONAccessTokenResponse;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.apache.oltu.oauth2.common.utils.OAuthUtils;
import org.mountcloud.graphql.GraphqlClient;
import org.mountcloud.graphql.request.query.DefaultGraphqlQuery;
import org.mountcloud.graphql.request.query.GraphqlQuery;
import org.mountcloud.graphql.request.result.ResultAttributtes;
import org.mountcloud.graphql.response.GraphqlResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DataBusUtil {

    public static Logger logger = LoggerFactory.getLogger(DataBusUtil.class);

    @Autowired
    UpstreamTypeService upstreamTypeService;

    @Autowired
    DomainInfoService domainInfoService;

    @Value("${app.scope}")
    private String appScope;
    @Value("${sso.url}")
    private String ssoUrl;
    @Value("${app.client}")
    private String appKey;
    @Value("${app.secret}")
    private String appSecret;
    @Value("${bus.url}")
    private String busUrl;
    @Value("${graphql.url}")
    private String graphqlUrl;

    private ConcurrentHashMap<String, Token> tokenMap = new ConcurrentHashMap<>();


    public static String getService(String busUrl, String name) {
        HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();
        String token = OAuthUtils.getAuthHeaderField(request.getHeader("Authorization"));
        if (StringUtils.isBlank(token)) {
            token = request.getParameter("access_token");
        }

        if (StringUtils.isBlank(token)) {
            logger.error("getService[error]:Missing required args '[access_token]'");
        }

        logger.info("Service name:{}", name);

        JSONObject params = new JSONObject();
        String graphql = "query {" +
                "services(filter:{name:{like:\"${name}\"}}){" +
                "      id" +
                "    }" +
                "}";
        params.put("query", graphql.replace("${name}", name));
        logger.info("Service query:{}", graphql);
        busUrl = UrlUtil.getUrl(busUrl);
        logger.info("busUrl=>" + busUrl);
        String result = sendPostRequest(busUrl + "?access_token=" + token, params);

        assert result != null;
        if (result.contains("data")) {
            logger.info("services:{}", result);
            return JSONObject.parseObject(result).getJSONObject("data").getJSONArray("services").getJSONObject(0).getString("id");
        } else {
            logger.error("services[error] name{}:", name);
            logger.error("services[error]:{}", result);
        }
        return null;
    }

    private static String sendPostRequest(String url, JSONObject params) {
        url = UrlUtil.getUrl(url);
        logger.info("pub post url:" + url);
        try {
            String s = Request.Post(url).bodyString(params.toJSONString(), ContentType.APPLICATION_JSON).execute().returnContent().asString();
            logger.info("pub post response:" + s);
            return s;
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("pub post error:" + e);
        }
        return null;


    }


    public Object getDataByBus(UpstreamType upstreamType) {
        //获取token
        String key = getToken();
        String[] split = upstreamType.getGraphqlUrl().split("/");

        //根据url 获取请求地址
        String substring = new StringBuffer(ssoUrl).replace(ssoUrl.length() - 4, ssoUrl.length(), busUrl).
                append(graphqlUrl).append("/").append("builtin").append("?access_token=").append(key).toString();

        //工具类过滤处理url
        String dealUrl = UrlUtil.getUrl(substring);


        //调用获取资源url
        String dataUrl = invokeUrl(dealUrl, split);
        //请求获取资源
        String u = dataUrl + "/" + "?access_token=" + key;

        return invokeForData(UrlUtil.getUrl(u), upstreamType);
    }

    private String getToken() {
        String sso = UrlUtil.getUrl(ssoUrl);
        //判断是否已有未过期token
        HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();
        String serverName = request.getServerName();

        //获取domain 信息
        DomainInfo byDomainName = domainInfoService.getByDomainName(serverName);
        Token token = tokenMap.get(serverName);
        if (null != token) {
            int i = token.getExpireIn().compareTo(System.currentTimeMillis());
            if (i > 0) {
                return token.getToken();
            }
        }

        Object[] objects = Arrays.stream(appScope.replace("+", " ").split(" ")).filter(s -> s.contains("sys_")).toArray();
        String scope = ArrayUtils.toString(objects, ",").replace("{", "").replace("}", "");
        OAuthClientRequest oAuthClientRequest = null;
        try {
            oAuthClientRequest = OAuthClientRequest
                    .tokenLocation(sso + "/oauth2/token").setGrantType(GrantType.CLIENT_CREDENTIALS)
                    .setClientId(byDomainName.getClientId()).setClientSecret(byDomainName.getClientSecret())
                    .setScope(scope.replace(",", " ")).buildBodyMessage();
        } catch (OAuthSystemException e) {
            logger.error("token 获取 : ->" + e.getMessage());
            e.printStackTrace();
        }
        OAuthClient oAuthClient = new OAuthClient(new SSLConnectionClient());
        OAuthJSONAccessTokenResponse oAuthClientResponse = null;
        try {
            oAuthClientResponse = oAuthClient.accessToken(oAuthClientRequest, "POST", OAuthJSONAccessTokenResponse.class);
        } catch (OAuthSystemException | OAuthProblemException e) {
            logger.error("token 获取" + e.getMessage());
            e.printStackTrace();
        }
        assert oAuthClientResponse != null;
        String accessToken = oAuthClientResponse.getAccessToken();
        long exp = System.currentTimeMillis() + (oAuthClientResponse.getExpiresIn() * 1000 - (10 * 60 * 1000));
        tokenMap.put(serverName, new Token(oAuthClientResponse.getAccessToken(), exp, System.currentTimeMillis()));
        return accessToken;
    }

    private String invokeUrl(String url, String[] split) {
        JSONObject params = new JSONObject();
        String graphql = "query  services($filter :Filter){   " +
                "  services(filter:$filter){" +
                "    endpoints{" +
                "    endPoint" +
                "    }" +
                "  }" +
                "}";
        JSONObject variables = new JSONObject();
        JSONObject name = new JSONObject();
        JSONObject like = new JSONObject();
        like.put("like", split[2]);
        name.put("name", like);
        variables.put("filter", name);
        params.put("query", graphql);
        params.put("variables", variables);

        url = UrlUtil.getUrl(url);

        String s = sendPostRequest(url, params);

        if (null == s) {
            try {
                throw new Exception("请求数据失败");
            } catch (Exception e) {
                logger.error("请求url 失败" + e.getMessage());
                e.printStackTrace();
            }
        }
        assert s != null;
        if (s.contains("errors")) {
            try {
                throw new Exception("获取url失败" + s);
            } catch (Exception e) {
                logger.error("获取url 失败" + e.getMessage());
                e.printStackTrace();
            }
        }
        JSONObject jsonObject = JSONArray.parseObject(s);

        JSONObject data = jsonObject.getJSONObject("data");

        JSONArray services = data.getJSONArray("services");

        JSONObject endpoints = services.getJSONObject(0);

        JSONArray endPoint = endpoints.getJSONArray("endpoints");

        JSONObject jo = endPoint.getJSONObject(0);

        return jo.getString("endPoint");

    }

    private Map invokeForData(String dataUrl, UpstreamType upstreamType) {
        logger.info("source url " + dataUrl);
        //获取字段映射
        List<UpstreamTypeField> fields = upstreamTypeService.findFields(upstreamType.getId());
        String[] type = upstreamType.getGraphqlUrl().split("/");
        GraphqlClient graphqlClient = GraphqlClient.buildGraphqlClient(dataUrl);
        String methodName = type[5];
        Map<String, Object> result = null;

        if (upstreamType.getIsPage()) {
            if ("query".equals(type[4])) {
                GraphqlQuery query = new DefaultGraphqlQuery(upstreamType.getSynType() + ":" + methodName);
                ResultAttributtes edges = new ResultAttributtes("edges");
                ResultAttributtes node = new ResultAttributtes("node");
                ArrayList<UpstreamTypeField> upstreamTypeFields = new ArrayList<>();
                for (UpstreamTypeField field : fields) {
                    // todo 修改常量
                    if (field.getTargetField().contains("$")) {
                        query.addResultAttributes(field.getSourceField() + ":" + field.getTargetField().substring(2, field.getTargetField().length() - 1));
                    } else {
                        upstreamTypeFields.add(field);
                    }
                }
                edges.addResultAttributes(node);
                query.addResultAttributes(edges);


                logger.info("body " + query);
                GraphqlResponse response = null;
                try {
                    response = graphqlClient.doQuery(query);
                } catch (IOException e) {
                    logger.info("response :  ->" + e.getMessage());
                    e.printStackTrace();
                }

                //获取数据，数据为map类型
                assert response != null;
                result = response.getData();
                for (Map.Entry<String, Object> entry : result.entrySet()) {
                    logger.info("result  data --" + entry.getKey() + "------" + entry.getValue());
                }
                if (null != upstreamTypeFields && upstreamTypeFields.size() > 0) {
                    for (UpstreamTypeField upstreamTypeField : upstreamTypeFields) {
                        result.put(upstreamTypeField.getSourceField(), upstreamTypeField.getTargetField());
                    }
                }
                return result;
            }
        }
        if ("query".equals(type[4])) {
            GraphqlQuery query = new DefaultGraphqlQuery(methodName);
            ArrayList<UpstreamTypeField> upstreamTypeFields = new ArrayList<>();
            for (UpstreamTypeField field : fields) {
                if (field.getTargetField().contains("$")) {
                    query.addResultAttributes(field.getSourceField() + ":" + field.getTargetField().substring(2, field.getTargetField().length() - 1));
                } else {
                    upstreamTypeFields.add(field);
                }
            }


            logger.info("body " + query);
            GraphqlResponse response = null;
            try {
                response = graphqlClient.doQuery(query);
            } catch (IOException e) {
                logger.info("response :  ->" + e.getMessage());
                e.printStackTrace();
            }

            //获取数据，数据为map类型
            result = response.getData();
            for (Map.Entry<String, Object> entry : result.entrySet()) {
                logger.info("result  data --" + entry.getKey() + "------" + entry.getValue());
            }
            if (null != upstreamTypeFields && upstreamTypeFields.size() > 0) {
                for (UpstreamTypeField upstreamTypeField : upstreamTypeFields) {
                    result.put(upstreamTypeField.getSourceField(), upstreamTypeField.getTargetField());
                }
            }
        }


        return result;
    }

}
