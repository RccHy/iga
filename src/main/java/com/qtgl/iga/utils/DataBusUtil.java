package com.qtgl.iga.utils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.qtgl.iga.bean.OccupyDto;
import com.qtgl.iga.bean.TreeBean;
import com.qtgl.iga.bo.*;
import com.qtgl.iga.service.DomainInfoService;
import com.qtgl.iga.service.UpstreamTypeService;
import com.qtgl.iga.utils.enumerate.ResultCode;
import com.qtgl.iga.utils.exception.CustomException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.BeanMap;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
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
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Component
public class DataBusUtil {


    @Autowired
    UpstreamTypeService upstreamTypeService;

    @Autowired
    DomainInfoService domainInfoService;

    @Value("${app.scope}")
    private String appScope;
    @Value("${sso.token.url}")
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

    public static ConcurrentHashMap<String, List<UpstreamTypeField>> typeFields = new ConcurrentHashMap<>();


    public static String sendPostRequest(String url, JSONObject params) throws Exception {
        url = UrlUtil.getUrl(url);
        log.info(" post url:" + url);
        String s = null;
        try {
            s = Request.Post(url).bodyString(params.toJSONString(), ContentType.APPLICATION_JSON).execute().returnContent().asString();
        } catch (IOException e) {
            e.printStackTrace();
            throw new CustomException(ResultCode.FAILED, "post请求失败， url:" + url + "; message:" + e.getMessage() + "; params:" + params);
        }
        log.info(" post response:" + s);
        return s;
    }


    public JSONArray getDataByBus(UpstreamType upstreamType, String serverName) throws Exception {
        //获取token
        String key = getToken(serverName);
        String[] split = upstreamType.getGraphqlUrl().split("/");

        //根据url 获取请求地址
        String substring = new StringBuffer(UrlUtil.getUrl(busUrl)).append("?access_token=").append(key)
                .append("&domain=").append(serverName).toString();
        //工具类过滤处理url
        String dealUrl = UrlUtil.getUrl(substring);
        //调用获取资源url
        String dataUrl = invokeUrl(dealUrl, split);
        //请求获取资源
        String u = dataUrl + "/" + "?access_token=" + key;

        return invokeForData(UrlUtil.getUrl(u), upstreamType);
    }

    public String getToken(String serverName) {
        String sso = UrlUtil.getUrl(ssoUrl);
        //判断是否已有未过期token
        if (StringUtils.isBlank(serverName)) {
            HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();
            serverName = request.getServerName();
        }

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
                    .tokenLocation(sso ).setGrantType(GrantType.CLIENT_CREDENTIALS)
                    .setClientId(byDomainName.getClientId()).setClientSecret(byDomainName.getClientSecret())
                    .setScope(scope.replace(",", " ")).buildBodyMessage();
        } catch (OAuthSystemException e) {
            log.error("token 获取 : ->" + e.getMessage());
            e.printStackTrace();
        }
        OAuthClient oAuthClient = new OAuthClient(new SSLConnectionClient());
        OAuthJSONAccessTokenResponse oAuthClientResponse = null;
        try {
            oAuthClientResponse = oAuthClient.accessToken(oAuthClientRequest, "POST", OAuthJSONAccessTokenResponse.class);
        } catch (OAuthSystemException | OAuthProblemException e) {
            log.error("token 获取" + e.getMessage());
            e.printStackTrace();
        }
        assert oAuthClientResponse != null;
        String accessToken = oAuthClientResponse.getAccessToken();
        long exp = System.currentTimeMillis() + (oAuthClientResponse.getExpiresIn() * 1000 - (10 * 60 * 1000));
        tokenMap.put(serverName, new Token(oAuthClientResponse.getAccessToken(), exp, System.currentTimeMillis()));
        return accessToken;
    }

    private String invokeUrl(String url, String[] split) throws Exception {
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
        if (null == s || s.contains("errors")) {
            throw new CustomException(ResultCode.GET_DATA_ERROR, null, null, s);
        }
        JSONObject jsonObject = JSONArray.parseObject(s);

        JSONObject data = jsonObject.getJSONObject("data");

        JSONArray services = data.getJSONArray("services");

        if (services.size() <= 0) {
            throw new CustomException(ResultCode.FAILED, "请求资源地址失败,请检查权威源类型");
        }
        JSONObject endpoints = services.getJSONObject(0);
        JSONArray endPoint = endpoints.getJSONArray("endpoints");
        if (endpoints.size() <= 0) {
            throw new CustomException(ResultCode.FAILED, "请求资源地址失败,请检查权威源类型");
        }
        JSONObject jo = endPoint.getJSONObject(0);

        return jo.getString("endPoint");

    }

    private JSONArray invokeForData(String dataUrl, UpstreamType upstreamType) throws Exception {
        log.info("source url " + dataUrl);
        //获取字段映射
        List<UpstreamTypeField> fields = upstreamTypeService.findFields(upstreamType.getId());
        typeFields.put(upstreamType.getId(), fields);
        String[] type = upstreamType.getGraphqlUrl().split("/");
        GraphqlClient graphqlClient = GraphqlClient.buildGraphqlClient(dataUrl);
        String methodName = type[5];
        Map<String, Object> result = null;
        JSONArray objects = new JSONArray();
        if (upstreamType.getIsPage()) {
            if ("query".equals(type[4])) {
                GraphqlQuery query = new DefaultGraphqlQuery(upstreamType.getSynType() + ":" + methodName);
                ResultAttributtes edges = new ResultAttributtes("edges");
                ResultAttributtes node = new ResultAttributtes("node");
                //不需要处理的映射字段(常量)
                ArrayList<UpstreamTypeField> upstreamTypeFields = new ArrayList<>();
                //存储映射字段(表达式需处理的)
                HashMap<String, List<String>> map = new HashMap<>();
                //query拼接所需查询的字段(简单映射的重命名以及表达式的处理(源字段查询))
                Map<String, String> nodeMap = new ConcurrentHashMap<>();
                for (UpstreamTypeField field : fields) {
                    //名称校验
                    String pattern = "\\$[a-zA-Z0-9_]+";
                    Pattern r = Pattern.compile(pattern);
                    Matcher m = r.matcher(field.getTargetField());
                    if (field.getTargetField().contains("=")) {
                        List<String> groupList = new ArrayList<>();
                        while (m.find()) {
                            groupList.add(m.group(0).substring(1));
                            nodeMap.put(m.group(0).substring(1), m.group(0).substring(1));
                        }
                        map.put(field.getSourceField(), groupList);
                        // 表达式 最终需要进行运算
                    } else if (!field.getTargetField().contains("=") && field.getTargetField().contains("$")) {
                        // 简单映射，直接查询时候进行重命名
                        if (m.find()) {
                            System.out.println("Found value: " + m.group(0));
                            // node.addResultAttributes(field.getSourceField() + ":" + m.group(0).substring(1));
                            nodeMap.put(field.getSourceField() + ":" + m.group(0).substring(1), m.group(0).substring(1));

                        }
                    } else {
                        upstreamTypeFields.add(field);
                    }
                }
                Set<String> nodeSet = nodeMap.keySet();
                String[] nodeString = nodeSet.toArray(new String[nodeSet.size()]);
                node.addResultAttributes(nodeString);
                edges.addResultAttributes(node);
                query.addResultAttributes(edges);


                log.info("body " + query);
                GraphqlResponse response = null;
                try {
                    response = graphqlClient.doQuery(query);
                } catch (IOException e) {
                    log.info("response :  ->" + e.getMessage());
                    e.printStackTrace();
                }

                //获取数据，数据为map类型
                result = response.getData();
                for (Map.Entry<String, Object> entry : result.entrySet()) {
                    log.debug("result  data --" + entry.getKey() + "------" + entry.getValue());
                }

                if (null == result || null == result.get("data")) {

                    throw new CustomException(ResultCode.GET_DATA_ERROR, null, null, upstreamType.getDescription(), result.get("errors").toString());

                }
                Map dataMap = (Map) result.get("data");
                Map deptMap = (Map) dataMap.get(upstreamType.getSynType());
                JSONArray deptArray = (JSONArray) JSONArray.toJSON(deptMap.get("edges"));
                if (null != deptArray) {
                    for (Object deptOb : deptArray) {
                        JSONObject nodeJson = (JSONObject) deptOb;
                        JSONObject node1 = nodeJson.getJSONObject("node");

                        objects.add(node1);
                    }
                }

                JSONArray resultJson = new JSONArray();
                Map<String, String> collect = fields.stream().collect(Collectors.toMap(UpstreamTypeField::getSourceField, UpstreamTypeField::getTargetField));
                for (Object object : objects) {
                    BeanMap beanMap = new BeanMap(object);
                    //以查询结果为结果集装入处理表达式后的结果
                    HashMap<String, Object> innerMap = (HashMap<String, Object>) beanMap.get("innerMap");

                    for (Map.Entry<String, List<String>> entry : map.entrySet()) {
                        try {
                            //处理前缀
                            SimpleBindings bindings = new SimpleBindings();
                            for (int i = 0; i < entry.getValue().size(); i++) {
                                bindings.put("$" + entry.getValue().get(i), null == innerMap.get(entry.getValue().get(i)) ? "" : innerMap.get(entry.getValue().get(i)));
                            }

                            String reg = collect.get(entry.getKey()).substring(1);
                            ScriptEngineManager sem = new ScriptEngineManager();
                            ScriptEngine engine = sem.getEngineByName("js");
                            final Object eval = engine.eval(reg, bindings);
                            innerMap.put(entry.getKey(), eval);

                        } catch (ScriptException e) {
                            log.error("eval处理数据异常{}", collect.get(map.get(entry.getKey())));
                            throw new Exception("eval处理数据异常" + collect.get(map.get(entry.getKey())));
                        }
                    }
                    //处理parentCode字段
                    if (null == innerMap.get("parentCode")) {
                        innerMap.put("parentCode", "");
                    }
                    if (null != upstreamTypeFields && upstreamTypeFields.size() > 0) {
                        for (UpstreamTypeField field : upstreamTypeFields) {
                            innerMap.put(field.getSourceField(), field.getTargetField());
                        }
                    }
                    resultJson.add(innerMap);

                }


                return resultJson;
            }
        }
        return null;
    }

    public Map getDataByBus(UpstreamType upstreamType, Integer offset, Integer first,DomainInfo domain) throws Exception {
        //获取token
        String key = getToken(null);
        String[] split = upstreamType.getGraphqlUrl().split("/");

        //根据url 获取请求地址
        String substring = new StringBuffer(UrlUtil.getUrl(busUrl)).append("?access_token=").append(key)
                .append("&domain=").append(domain.getDomainName()).toString();
        //工具类过滤处理url
        String dealUrl = UrlUtil.getUrl(substring);


        //调用获取资源url
        String dataUrl = invokeUrl(dealUrl, split);
        //请求获取资源
        String u = dataUrl + "/" + "?access_token=" + key+"&domain="+domain.getDomainName();

        return invokeForMapData(UrlUtil.getUrl(u), upstreamType, offset, first);
    }


    private Map invokeForMapData(String dataUrl, UpstreamType upstreamType, Integer offset, Integer first) throws Exception {
        log.info("source url " + dataUrl);
        //获取字段映射
        List<UpstreamTypeField> fields = upstreamTypeService.findFields(upstreamType.getId());
        String[] type = upstreamType.getGraphqlUrl().split("/");
        GraphqlClient graphqlClient = GraphqlClient.buildGraphqlClient(dataUrl);
        String methodName = type[5];
        Map<String, Object> result = null;
        JSONArray objects = new JSONArray();
        ArrayList<HashMap<String, Object>> rs = null;
        if ("query".equals(type[4])) {
            GraphqlQuery query = new DefaultGraphqlQuery(upstreamType.getSynType() + ":" + methodName);
            if (null != offset) {
                query.addParameter("offset", offset);
            }
            if (null != first) {
                query.addParameter("first", first);
            }
            ResultAttributtes edges = new ResultAttributtes("edges");
            ResultAttributtes node = new ResultAttributtes("node");
            //不需要处理的映射字段(常量)
            ArrayList<UpstreamTypeField> upstreamTypeFields = new ArrayList<>();
            //存储映射字段(表达式需处理的)
            HashMap<String, List<String>> map = new HashMap<>();
            //query拼接所需查询的字段(简单映射的重命名以及表达式的处理(源字段查询))
            Map<String, String> nodeMap = new ConcurrentHashMap<>();
            for (UpstreamTypeField field : fields) {
                //名称校验
                String pattern = "\\$[a-zA-Z0-9_]+";
                Pattern r = Pattern.compile(pattern);
                Matcher m = r.matcher(field.getTargetField());
                // 表达式 最终需要进行运算
                if (field.getTargetField().contains("=")) {
                    List<String> goupList = new ArrayList<>();
                    while (m.find()) {
                        goupList.add(m.group(0).substring(1));
                        nodeMap.put(m.group(0).substring(1), m.group(0).substring(1));
                    }
                    map.put(field.getSourceField(), goupList);
                   /* if (m.find()) {
                        map.put( field.getSourceField(),m.group(0).substring(1));
                        nodeMap.put(m.group(0).substring(1), m.group(0).substring(1));
                    }*/
                } else if (!field.getTargetField().contains("=") && field.getTargetField().contains("$")) {
                    // 简单映射，直接查询时候进行重命名
                    if (m.find()) {
                        System.out.println("Found value: " + m.group(0));
                        // node.addResultAttributes(field.getSourceField() + ":" + m.group(0).substring(1));
                        nodeMap.put(field.getSourceField() + ":" + m.group(0).substring(1), m.group(0).substring(1));

                    }
                } else {
                    upstreamTypeFields.add(field);
                }
            }
            Set<String> nodeSet = nodeMap.keySet();
            String[] nodeString = nodeSet.toArray(new String[nodeSet.size()]);
            node.addResultAttributes(nodeString);
            edges.addResultAttributes(node);
            query.addResultAttributes(edges);
            query.addResultAttributes("totalCount");


            log.info("body " + query);
            GraphqlResponse response = null;
            try {
                response = graphqlClient.doQuery(query);
            } catch (IOException e) {
                log.info("response :  ->" + e.getMessage());
                e.printStackTrace();
            }

            //获取数据，数据为map类型
            result = response.getData();
            for (Map.Entry<String, Object> entry : result.entrySet()) {
                log.info("result  data --" + entry.getKey() + "------" + entry.getValue());
                if ("errors".equals(entry.getKey())) {
                    throw new CustomException(ResultCode.FAILED, "查询数据失败,请检查权威源配置");
                }
            }

            if (null == result || null == result.get("data")) {

                throw new CustomException(ResultCode.FAILED, "映射字段错误,数据获取失败");

            }

            Map dataMap = (Map) result.get("data");
            Map deptMap = (Map) dataMap.get(upstreamType.getSynType());
            JSONArray deptArray = (JSONArray) JSONArray.toJSON(deptMap.get("edges"));
            if (null != deptArray) {
                for (Object deptOb : deptArray) {
                    JSONObject nodeJson = (JSONObject) deptOb;
                    objects.add(nodeJson.getJSONObject("node"));
                }
            }

            rs = new ArrayList<>();
            Map<String, String> collect = fields.stream().collect(Collectors.toMap(UpstreamTypeField::getSourceField, UpstreamTypeField::getTargetField));
            System.out.println("start:" + System.currentTimeMillis());
            for (Object object : objects) {

                BeanMap beanMap = new BeanMap(object);
                //以查询结果为结果集装入处理表达式后的结果
                HashMap<String, Object> innerMap = (HashMap<String, Object>) beanMap.get("innerMap");
                for (Map.Entry<String, List<String>> entry : map.entrySet()) {
                    try {
                        //处理前缀
                        SimpleBindings bindings = new SimpleBindings();
                        for (int i = 0; i < entry.getValue().size(); i++) {
                            bindings.put("$" + entry.getValue().get(i), null == innerMap.get(entry.getValue().get(i)) ? "" : innerMap.get(entry.getValue().get(i)));
                        }

                        String reg = collect.get(entry.getKey()).substring(1);
                        ScriptEngineManager sem = new ScriptEngineManager();
                        ScriptEngine engine = sem.getEngineByName("js");
                        final Object eval = engine.eval(reg, bindings);
                        // jsonObject.put(entry.getValue(), eval);
                        innerMap.put(entry.getKey(), eval);

                    } catch (ScriptException e) {
                        log.error("eval处理数据异常{}", collect.get(map.get(entry.getKey())));
                        throw new Exception("eval处理数据异常" + collect.get(map.get(entry.getKey())));
                    }
                }
                //处理parentCode字段
                if (null == innerMap.get("parentCode")) {
                    innerMap.put("parentCode", "");
                }
                LinkedHashMap<String, Object> stringObjectLinkedHashMap = new LinkedHashMap<>();
                if (null != upstreamTypeFields && upstreamTypeFields.size() > 0) {
                    for (UpstreamTypeField field : upstreamTypeFields) {
                        //jsonObject.put(field.getSourceField(), field.getTargetField());
                        innerMap.put(field.getSourceField(), field.getTargetField());
                    }
                }
                stringObjectLinkedHashMap.put("node", innerMap);
                rs.add(stringObjectLinkedHashMap);
            }

            System.out.println("end:" + System.currentTimeMillis());
            deptMap.put("edges", rs);


        }

        return (Map) result.get("data");


    }


    public String pub(Map<TreeBean, String> treeBeanMap, Map<String, List<Person>> personMap, Map<String, List<OccupyDto>> occupyMap, String type, DomainInfo domain) throws Exception {
        log.info("start pub {}", type);
        JSONObject params = new JSONObject();
        StringBuffer graphql = new StringBuffer("mutation  {");
        if ("dept".equals(type) || "post".equals(type)) {
            for (Map.Entry<TreeBean, String> entry : treeBeanMap.entrySet()) {
                TreeBean k = entry.getKey();
                String v = entry.getValue();
                if (v.equals("obsolete")) {
                    break;
                }
                String pub = "{" +
                        "    type : \"%s\",\n" +
                        "    source : \"%s\", \n" +
                        "    subject : \"%s\", \n" +
                        "    id : \"%s\",         \n" +
                        "    time : \"%s\",  \n" +
                        "    datacontenttype : \"application/json\",        \n" +
                        "    data : \"%s\", " +
                        "}";
                if (v.equals("insert")) {
                    pub = String.format(pub, type + ".created", domain.getClientId(),
                            k.getCode(), k.getCode() + UUID.randomUUID(),
                            k.getCreateTime().toEpochSecond(ZoneOffset.of("+8")), JSONObject.toJSONString(k).replace("\"", "\\\""));
                }
                if (v.equals("update")) {
                    pub = String.format(pub, type + ".updated", domain.getClientId(),
                            k.getCode(), k.getCode() + UUID.randomUUID(),
                            k.getCreateTime().toEpochSecond(ZoneOffset.of("+8")), JSONObject.toJSONString(k).replace("\"", "\\\""));
                }
                if (v.equals("delete")) {
                    pub = String.format(pub, type + ".deleted", domain.getClientId(),
                            k.getCode(), k.getCode() + UUID.randomUUID(),
                            k.getCreateTime().toEpochSecond(ZoneOffset.of("+8")), JSONObject.toJSONString(k).replace("\"", "\\\""));
                }
                graphql.append(RandomStringUtils.randomAlphabetic(20) + ":pub(message:" + pub + "){id}\n");
            }

        } else if ("person".equals(type)) {
            List<Person> insertPersonList = personMap.get("insert");
            if (null != insertPersonList && insertPersonList.size() > 0) {
                for (Person person : insertPersonList) {
                    String pub = "{" +
                            "    type : \"%s\",\n" +
                            "    source : \"%s\", \n" +
                            "    subject : \"%s\", \n" +
                            "    id : \"%s\",         \n" +
                            "    time : \"%s\",  \n" +
                            "    datacontenttype : \"application/json\",        \n" +
                            "    data : \"%s\", " +
                            "}";
                    pub = String.format(pub, "person.created", domain.getClientId(),
                            person.getOpenId(), UUID.randomUUID(),
                            person.getCreateTime().toEpochSecond(ZoneOffset.of("+8")), JSONObject.toJSONString(person).replace("\"", "\\\""));
                    graphql.append(person.getOpenId() + ":pub(message:" + pub + "){id}\n");
                }
            }
            List<Person> updatePersonList = personMap.get("update");
            if (null != updatePersonList && updatePersonList.size() > 0) {
                for (Person person : updatePersonList) {
                    String pub = "{" +
                            "    type : \"%s\",\n" +
                            "    source : \"%s\", \n" +
                            "    subject : \"%s\", \n" +
                            "    id : \"%s\",         \n" +
                            "    time : \"%s\",  \n" +
                            "    datacontenttype : \"application/json\",        \n" +
                            "    data : \"%s\", " +
                            "}";
                    pub = String.format(pub, "person.updated", domain.getClientId(),
                            person.getOpenId(), UUID.randomUUID(),
                            person.getCreateTime().toEpochSecond(ZoneOffset.of("+8")), JSONObject.toJSONString(person).replace("\"", "\\\""));
                    graphql.append(person.getOpenId() + ":pub(message:" + pub + "){id}\n");
                }
            }
            List<Person> deletePersonList = personMap.get("delete");
            if (null != deletePersonList && deletePersonList.size() > 0) {
                for (Person person : deletePersonList) {
                    String pub = "{" +
                            "    type : \"%s\",\n" +
                            "    source : \"%s\", \n" +
                            "    subject : \"%s\", \n" +
                            "    id : \"%s\",         \n" +
                            "    time : \"%s\",  \n" +
                            "    datacontenttype : \"application/json\",        \n" +
                            "    data : \"%s\", " +
                            "}";
                    pub = String.format(pub, "person.deleted", domain.getClientId(),
                            person.getOpenId(), UUID.randomUUID(),
                            person.getCreateTime().toEpochSecond(ZoneOffset.of("+8")), JSONObject.toJSONString(person).replace("\"", "\\\""));
                    graphql.append(person.getOpenId() + ":pub(message:" + pub + "){id}\n");
                }
            }

        } else if ("occupy".equals(type)) {
            List<OccupyDto> insert = occupyMap.get("insert");
            if (null != insert && insert.size() > 0) {
                for (OccupyDto occupy : insert) {
                    String pub = "{" +
                            "    type : \"%s\",\n" +
                            "    source : \"%s\", \n" +
                            "    subject : \"%s\", \n" +
                            "    id : \"%s\",         \n" +
                            "    time : \"%s\",  \n" +
                            "    datacontenttype : \"application/json\",        \n" +
                            "    data : \"%s\", " +
                            "}";
                    pub = String.format(pub, "user.position.created", domain.getClientId(),
                            occupy.getOccupyId(), UUID.randomUUID(),
                            occupy.getCreateTime().toEpochSecond(ZoneOffset.of("+8")), JSONObject.toJSONString(occupy).replace("\"", "\\\""));
                    graphql.append(RandomStringUtils.randomAlphabetic(20) + ":pub(message:" + pub + "){id}\n");
                }
            }
            List<OccupyDto> update = occupyMap.get("update");
            if (null != update && update.size() > 0) {
                for (OccupyDto occupy : update) {
                    String pub = "{" +
                            "    type : \"%s\",\n" +
                            "    source : \"%s\", \n" +
                            "    subject : \"%s\", \n" +
                            "    id : \"%s\",         \n" +
                            "    time : \"%s\",  \n" +
                            "    datacontenttype : \"application/json\",        \n" +
                            "    data : \"%s\", " +
                            "}";
                    pub = String.format(pub, "user.position.updated", domain.getClientId(),
                            occupy.getOccupyId(), UUID.randomUUID(),
                            occupy.getCreateTime().toEpochSecond(ZoneOffset.of("+8")), JSONObject.toJSONString(occupy).replace("\"", "\\\""));
                    graphql.append(RandomStringUtils.randomAlphabetic(20) + ":pub(message:" + pub + "){id}\n");
                }
            }
            List<OccupyDto> delete = occupyMap.get("delete");
            if (null != delete && delete.size() > 0) {
                for (OccupyDto occupy : delete) {
                    String pub = "{" +
                            "    type : \"%s\",\n" +
                            "    source : \"%s\", \n" +
                            "    subject : \"%s\", \n" +
                            "    id : \"%s\",         \n" +
                            "    time : \"%s\",  \n" +
                            "    datacontenttype : \"application/json\",        \n" +
                            "    data : \"%s\", " +
                            "}";
                    pub = String.format(pub, "user.position.deleted", domain.getClientId(),
                            occupy.getOccupyId(), UUID.randomUUID(),
                            occupy.getCreateTime().toEpochSecond(ZoneOffset.of("+8")), JSONObject.toJSONString(occupy).replace("\"", "\\\""));
                    graphql.append(RandomStringUtils.randomAlphabetic(20) + ":pub(message:" + pub + "){id}\n");
                }
            }

        }
        if (graphql.toString().equals("mutation  {")) {
            return "";
        }
        graphql.append("}");
        params.put("query", graphql);

        log.debug("pub graphql: {}", graphql);

        String token = getToken(domain.getDomainName());
        return sendPostRequest(busUrl + "?access_token=" + token+"&domain="+domain.getDomainName(), params);

    }


    public String getApiToken(DomainInfo domainInfo) {
        String sso = UrlUtil.getUrl(ssoUrl);
        //判断是否已有未过期token
        if (StringUtils.isBlank(domainInfo.getDomainName())) {
            HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();
            domainInfo.setDomainName(request.getServerName());
        }

        //获取domain 信息
        Token token = tokenMap.get(domainInfo.getDomainName());
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
                    .setClientId(domainInfo.getClientId()).setClientSecret(domainInfo.getClientSecret())
                    .setScope(scope.replace(",", " ")).buildBodyMessage();
        } catch (OAuthSystemException e) {
            log.error("token 获取 : ->" + e.getMessage());
            e.printStackTrace();
        }
        OAuthClient oAuthClient = new OAuthClient(new SSLConnectionClient());
        OAuthJSONAccessTokenResponse oAuthClientResponse = null;
        try {
            oAuthClientResponse = oAuthClient.accessToken(oAuthClientRequest, "POST", OAuthJSONAccessTokenResponse.class);
        } catch (OAuthSystemException | OAuthProblemException e) {
            log.error("token 获取" + e.getMessage());
            e.printStackTrace();
        }
        assert oAuthClientResponse != null;
        String accessToken = oAuthClientResponse.getAccessToken();
        long exp = System.currentTimeMillis() + (oAuthClientResponse.getExpiresIn() * 1000 - (10 * 60 * 1000));
        tokenMap.put(domainInfo.getDomainName(), new Token(oAuthClientResponse.getAccessToken(), exp, System.currentTimeMillis()));
        return accessToken;
    }
}
