package com.qtgl.iga.utils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.qtgl.iga.bo.Token;
import com.qtgl.iga.bo.UpstreamTypeField;
import com.qtgl.iga.service.UpstreamTypeService;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.apache.oltu.oauth2.client.OAuthClient;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.response.OAuthJSONAccessTokenResponse;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.apache.oltu.oauth2.common.utils.OAuthUtils;
import org.mountcloud.graphql.GraphqlClient;
import org.mountcloud.graphql.request.query.DefaultGraphqlQuery;
import org.mountcloud.graphql.request.query.GraphqlQuery;
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DataBusUtil {

    public static Logger logger = LoggerFactory.getLogger(DataBusUtil.class);

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
    private String busUrl;
    @Value("${graphql.url}")
    private String graphqlUrl;

    private ConcurrentHashMap<String, Token> tokenMap = new ConcurrentHashMap<>();


    public static String getService(String busUrl, String name) {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String token = OAuthUtils.getAuthHeaderField(request.getHeader("Authorization"));
        if (StringUtils.isBlank(token))
            token = request.getParameter("access_token");

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

        if (result.contains("data")) {
            logger.info("services:{}", result);
            return JSONObject.parseObject(result).getJSONObject("data").getJSONArray("services").getJSONObject(0).getString("id");
        } else {
            logger.error("services[error] name{}:", name);
            logger.error("services[error]:{}", result);
        }
        return null;
    }

    public static String sendPostRequest(String url, JSONObject params) {
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


    public Object getDataByBus(String url) throws Exception {
        //获取token
        String key = getToken();
        String[] split = url.split("/");

        //根据url 获取请求地址
        String substring = new StringBuffer(ssoUrl).replace(ssoUrl.length() - 4, ssoUrl.length(), busUrl).
                append(graphqlUrl).append("/").append(split[2]).append("/").append("?access_token=").append(key).toString();

        //工具类过滤处理url
        String dealUrl = UrlUtil.getUrl(substring);


        //调用获取资源url
        String dataUrl = invokeUrl(dealUrl, split);
        //请求获取资源
        String u = new StringBuffer(dataUrl).append("/").append("?access_token=").append(key).toString();

        return invokeForData(UrlUtil.getUrl(u), url);
    }

    private String getToken() throws Exception {
        //判断是否已有未过期token
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        StringBuffer url = request.getRequestURL();
        String tempContextUrl = url.delete(url.length() - request.getRequestURI().length(), url.length()).append("/").toString();

        Token token = tokenMap.get(tempContextUrl);
        if (null != token) {
            int i = token.getExpireIn().compareTo(System.currentTimeMillis());
            if (i > 0) {
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
        OAuthJSONAccessTokenResponse oAuthClientResponse = oAuthClient.accessToken(oAuthClientRequest, "POST", OAuthJSONAccessTokenResponse.class);
        String accessToken = oAuthClientResponse.getAccessToken();
        long exp = System.currentTimeMillis() + (oAuthClientResponse.getExpiresIn() - (10 * 60 * 1000));
        tokenMap.put(tempContextUrl, new Token(oAuthClientResponse.getAccessToken(), exp, System.currentTimeMillis()));
        return accessToken;
    }

    private String invokeUrl(String url, String[] split) throws Exception {
//        url = "https://cloud.ketanyun.cn/bus/graphql/builtin?access_token=a5021c7222995a42f54afca1f9c9f637";
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

        assert s != null;
        if (s.contains("errors")) {
            throw new Exception("获取url失败" + s);
        }
        JSONObject jsonObject = JSONArray.parseObject(s);

        JSONObject data = jsonObject.getJSONObject("data");

        JSONArray services = data.getJSONArray("services");

        JSONObject endpoints = services.getJSONObject(0);

        JSONArray endPoint = endpoints.getJSONArray("endpoints");

        JSONObject jo = endPoint.getJSONObject(0);

        String endPoint1 = jo.getString("endPoint");

        return endPoint1;

    }

    private Map invokeForData(String dataUrl, String url) throws IOException {
        logger.info("source url " + dataUrl);
        //获取字段映射
        List<UpstreamTypeField> fields = upstreamTypeService.findFields(url);
        String[] type = url.split("/");
        GraphqlClient graphqlClient = GraphqlClient.buildGraphqlClient(dataUrl);
        String methodName = type[5];
        Map<String, Object> result = null;
        if ("query".equals(type[4])) {
            GraphqlQuery query = new DefaultGraphqlQuery(methodName);

            for (UpstreamTypeField field : fields) {
                query.addResultAttributes(field.getTargetField() + ":" + field.getSourceField());
            }

//            query.addResultAttributes("A:id");

            logger.info("body " + query);
            GraphqlResponse response = graphqlClient.doQuery(query);

            //获取数据，数据为map类型
            result = response.getData();
            for (Map.Entry<String, Object> entry : result.entrySet()) {
                logger.info("result  data --" + entry.getKey() + "------" + entry.getValue());
            }
        }
        if ("mutation".equals(methodName)) {

        }

        return result;
    }

}
