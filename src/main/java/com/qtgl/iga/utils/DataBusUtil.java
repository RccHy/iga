package com.qtgl.iga.utils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.qtgl.iga.bean.DataMapField;
import com.qtgl.iga.bean.DataMapNode;
import com.qtgl.iga.bean.OccupyDto;
import com.qtgl.iga.bean.TreeBean;
import com.qtgl.iga.bo.*;
import com.qtgl.iga.dao.*;
import com.qtgl.iga.service.DomainInfoService;
import com.qtgl.iga.service.IncrementalTaskService;
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
import org.springframework.util.CollectionUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.sql.Timestamp;
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

    @Autowired
    CertifiedConnector certifiedConnector;
    @Autowired
    DeptDao deptDao;
    @Autowired
    TenantDao tenantDao;
    @Autowired
    PostDao postDao;
    @Autowired
    PersonDao personDao;
    @Autowired
    OccupyDao occupyDao;
    @Autowired
    IncrementalTaskService incrementalTaskService;

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
        String u = dataUrl + "/" + "?access_token=" + key + "&domain=" + serverName;

        //调用获取资源url
        String dataMapUrl = invokeUrl(dealUrl, new String[]{"", "", "catalog"});
        //
        Map<String, Map<String, String>> dataMap = null;
        if (StringUtils.isNotBlank(dataMapUrl)) {
            log.info("无法内省 catalog 服务地址");
            dataMap = getDataMap(dataMapUrl + "?access_token=" + key + "&domain=" + serverName);
        }

        return invokeForData(UrlUtil.getUrl(u), upstreamType, serverName, dataMap);
    }

    public String getToken(String serverName) {
        String sso = UrlUtil.getUrl(ssoUrl);

        //获取domain 信息
        DomainInfo byDomainName = domainInfoService.getByDomainName(serverName);
        if (null == byDomainName) {
            throw new CustomException(ResultCode.FAILED, "解析token获取租户失败,请检查当前token");
        }
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
                    .tokenLocation(sso).setGrantType(GrantType.CLIENT_CREDENTIALS)
                    .setClientId(byDomainName.getClientId()).setClientSecret(byDomainName.getClientSecret())
                    .setScope(scope.replace(",", " ")).buildBodyMessage();
        } catch (OAuthSystemException e) {
            log.error("token 获取 : ->" + e.getMessage());
            e.printStackTrace();
            throw new CustomException(ResultCode.FAILED, "token 获取失败:" + e.getMessage());
        }
        OAuthClient oAuthClient = new OAuthClient(new SSLConnectionClient());
        OAuthJSONAccessTokenResponse oAuthClientResponse = null;
        try {
            oAuthClientResponse = oAuthClient.accessToken(oAuthClientRequest, "POST", OAuthJSONAccessTokenResponse.class);
        } catch (OAuthSystemException | OAuthProblemException e) {
            log.error("token 获取" + e.getMessage());
            e.printStackTrace();
            throw new CustomException(ResultCode.FAILED, "token 获取失败:" + e.getMessage());
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

    private JSONArray invokeForData(String dataUrl, UpstreamType upstreamType, String domainName, Map<String, Map<String, String>> dataMapField) throws Exception {
        log.info("source url " + dataUrl);
        //获取字段映射
        List<UpstreamTypeField> fields = upstreamTypeService.findFields(upstreamType.getId());
        Map<String, String> collect = fields.stream().collect(Collectors.toMap(UpstreamTypeField::getSourceField, UpstreamTypeField::getTargetField));

        typeFields.put(upstreamType.getId(), fields);
        String[] type = upstreamType.getGraphqlUrl().split("/");
        GraphqlClient graphqlClient = GraphqlClient.buildGraphqlClient(dataUrl);
        String methodName = type[5];
        Map<String, Object> result = null;
        JSONArray objects = new JSONArray();
        if (upstreamType.getIsPage()) {
            if ("query".equals(type[4])) {
                GraphqlQuery query = new DefaultGraphqlQuery(upstreamType.getSynType() + ":" + methodName);
                // 增量同步添加入参
                if (null != upstreamType.getIsIncremental() && upstreamType.getIsIncremental()) {
                    Timestamp timestamp = this.processTime(upstreamType);
                    if (null != timestamp && collect.containsKey("updateTime")) {
                        Map<String, Object> timeMap = new HashMap<>();
                        timeMap.put("gt", timestamp);
                        Map<String, Object> filterMap = new HashMap<>();
                        String time = collect.get("updateTime");
                        filterMap.put(time.substring(1), timeMap);
                        query.getRequestParameter().addObjectParameter("filter", filterMap);
                    }
                }
                ResultAttributtes edges = new ResultAttributtes("edges");
                ResultAttributtes node = new ResultAttributtes("node");
                //不需要处理的映射字段(常量)
                ArrayList<UpstreamTypeField> upstreamTypeFields = new ArrayList<>();
                //存储映射字段(表达式需处理的)
                HashMap<String, List<String>> map = new HashMap<>();
                //存储忽略值映射需要数据库数据覆盖的
                HashMap<String, List<String>> ignoreMap = new HashMap<>();
                //query拼接所需查询的字段(简单映射的重命名以及表达式的处理(源字段查询))
                Map<String, String> nodeMap = new ConcurrentHashMap<>();
                for (UpstreamTypeField field : fields) {
                    //名称校验
                    String pattern = "\\$[a-zA-Z0-9_]+";
                    Pattern r = Pattern.compile(pattern);
                    Matcher m = r.matcher(field.getTargetField());
                    //ENTITY校验
                    String enPattern = "\\$[a-zA-Z0-9_]+.[a-zA-Z0-9_]+";
                    Pattern enR = Pattern.compile(enPattern);
                    Matcher enM = enR.matcher(field.getTargetField());
                    if (field.getTargetField().contains("=")) {
                        List<String> groupList = new ArrayList<>();
                        while (m.find()) {
                            System.out.println("Found value: " + m.group(0));
                            if (!m.group(0).equals("$ENTITY")) {
                                groupList.add(m.group(0).substring(1));
                                nodeMap.put(m.group(0).substring(1), m.group(0).substring(1));
                            }
                            if (!field.getTargetField().contains("$ENTITY")) {
                                nodeMap.put(m.group(0).substring(1), m.group(0).substring(1));
                            }
                        }
                        if (!field.getTargetField().contains("$ENTITY")) {
                            map.put(field.getSourceField(), groupList);
                        } else {
                            while (enM.find()) {
                                System.out.println("Ignore Found value: " + enM.group(0));
                                groupList.add(enM.group(0).substring(1));
                            }
                            ignoreMap.put(field.getSourceField(), groupList);
                            //if ("tags".equals(field.getSourceField())) {
                            //    log.info("{}: ignoreMap{}", upstreamType.getSynType(), ignoreMap);
                            //}
                        }
                        // 表达式 最终需要进行运算
                    } else if (!field.getTargetField().contains("=") && field.getTargetField().contains("$ENTITY")) {
                        // 覆盖映射，查询后进行字段值覆盖
                        if (enM.find()) {
                            List<String> groupList = new ArrayList<>();
                            System.out.println("Ignore Found value: " + enM.group(0));
                            // node.addResultAttributes(field.getSourceField() + ":" + m.group(0).substring(1));
                            groupList.add(enM.group(0).substring(1));
                            ignoreMap.put(field.getSourceField(), groupList);
                        }
                    } else if (!field.getTargetField().contains("=") && field.getTargetField().contains("$")) {
                        // 简单映射，直接查询时候进行重命名
                        if (m.find()) {
                            System.out.println("Found value: " + m.group(0));
                            // node.addResultAttributes(field.getSourceField() + ":" + m.group(0).substring(1));
                            nodeMap.put(field.getSourceField() + ":" + m.group(0).substring(1), m.group(0).substring(1));
                            //nodeMap.put(m.group(0).substring(1), m.group(0).substring(1));

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

                query.getRequestParameter().addObjectParameter("offset", 0);
                query.getRequestParameter().addObjectParameter("first", 5000);

                Integer totalCount = doQueryData(upstreamType, graphqlClient, objects, query);
                double ceil = Math.ceil(totalCount / 5000.0);
                for (int i = 1; i < ceil; i++) {
                    query.getRequestParameter().addObjectParameter("offset", i * 5000);
                    query.getRequestParameter().addObjectParameter("first", 5000);
                    doQueryData(upstreamType, graphqlClient, objects, query);
                }

                log.info("类型:{},权威源类型:{},获取数据成功,总计:{}条", upstreamType.getSynType(), upstreamType.getId(), totalCount);

                JSONArray resultJson = new JSONArray();
                //获取sso数据
                Tenant tenant = tenantDao.findByDomainName(domainName);
                if (null == tenant) {
                    throw new CustomException(ResultCode.FAILED, "租户不存在");
                }
                //数据库数据存储容器
                List<TreeBean> beans;
                Map<String, TreeBean> treeMap = new HashMap<>();
                List<Person> persons;
                Map<String, Person> personCardMap = new HashMap<>();
                Map<String, Person> personAccountMap = new HashMap<>();
                List<OccupyDto> occupyDtos;
                Map<String, OccupyDto> occupyDtoMap = new HashMap<>();
                Map<String, OccupyDto> occupyDtoAccountMap = new HashMap<>();
                if ("dept".equals(upstreamType.getSynType())) {
                    beans = deptDao.findByTenantId(tenant.getId(), null, null);
                    if (!CollectionUtils.isEmpty(beans)) {
                        treeMap = beans.stream().collect(Collectors.toMap(TreeBean::getCode, bean -> bean));
                    }
                } else if ("post".equals(upstreamType.getSynType())) {
                    beans = postDao.findByTenantId(tenant.getId());
                    if (!CollectionUtils.isEmpty(beans)) {
                        treeMap = beans.stream().collect(Collectors.toMap(TreeBean::getCode, bean -> bean));
                    }

                } else if ("person".equals(upstreamType.getSynType())) {
                    persons = personDao.getAll(tenant.getId());
                    if (!CollectionUtils.isEmpty(persons)) {
                        personAccountMap = persons.stream().filter(person ->
                                !StringUtils.isBlank(person.getAccountNo())).collect(Collectors.toMap(Person::getAccountNo, person -> person, (v1, v2) -> v2));
                        personCardMap = persons.stream().filter(person -> !StringUtils.isBlank(person.getCardType()) && !StringUtils.isBlank(person.getCardNo())).collect(Collectors.toMap(person -> (person.getCardType() + ":" + person.getCardNo()), person -> person, (v1, v2) -> v2));
                    }

                } else if ("occupy".equals(upstreamType.getSynType())) {
                    //获取sso的所有人员
                    persons = personDao.getAll(tenant.getId());
                    if (!CollectionUtils.isEmpty(persons)) {
                        personAccountMap = persons.stream().filter(person ->
                                !StringUtils.isBlank(person.getAccountNo())).collect(Collectors.toMap(Person::getAccountNo, person -> person, (v1, v2) -> v2));
                        personCardMap = persons.stream().filter(person -> !StringUtils.isBlank(person.getCardType()) && !StringUtils.isBlank(person.getCardNo())).collect(Collectors.toMap(person -> (person.getCardType() + ":" + person.getCardNo()), person -> person, (v1, v2) -> v2));
                    }
                    occupyDtos = occupyDao.findAll(tenant.getId(), null, null);
                    if (!CollectionUtils.isEmpty(occupyDtos)) {
                        occupyDtoMap = occupyDtos.stream().filter(occupyDto -> !StringUtils.isBlank(occupyDto.getPostCode()) && !StringUtils.isBlank(occupyDto.getDeptCode())).collect(Collectors.toMap(occupyDto -> (occupyDto.getPersonId() + ":" + occupyDto.getPostCode() + ":" + occupyDto.getDeptCode()), occupyDto -> occupyDto, (v1, v2) -> v2));
                        occupyDtoAccountMap = occupyDtos.stream().filter(occupyDto -> !StringUtils.isBlank(occupyDto.getAccountNo())).collect(Collectors.toMap(occupyDto -> (occupyDto.getPersonId() + ":" + occupyDto.getAccountNo()), occupyDto -> occupyDto, (v1, v2) -> v2));
                    }


                }
                log.info("-------------开始处理上游数据映射");
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
                            // 如果是map 表达式 =map(XX,DataMapCode)
                            if (reg.startsWith("map(") && reg.endsWith(")")) {
                                if (null == dataMapField || dataMapField.size() <= 0) {
                                    throw new Exception("无法进行代码映射，请检查" + entry.getKey() + "字段映射关系。");
                                }
                                // Pattern pattern = Pattern.compile("(?<=\\=map\\()[^\\)]+");
                                String v = reg.substring(4, reg.length() - 1);
                                final String[] vSplit = v.split(",");
                                if (vSplit.length != 2) {
                                    throw new Exception("数据映射表达式异常");
                                }
                                String value = bindings.get(vSplit[0]).toString();
                                // 代码表
                                if (dataMapField.containsKey(vSplit[1])) {
                                    if (dataMapField.get(vSplit[1]).containsKey(value)) {
                                        innerMap.put(entry.getKey(), dataMapField.get(vSplit[1]).get(value));
                                    } else if (dataMapField.get(vSplit[1]).containsKey("=other()")) {
                                        String reg2 = dataMapField.get(vSplit[1]).get("=other()");
                                        if (reg2.equals("=old()")) {
                                            innerMap.put(entry.getKey(), value);
                                        } else {
                                            reg2 = reg2.substring(1);
                                            ScriptEngineManager sem = new ScriptEngineManager();
                                            ScriptEngine engine = sem.getEngineByName("js");
                                            final Object eval = engine.eval(reg2, bindings);
                                            innerMap.put(entry.getKey(), eval);
                                        }
                                    }

                                }

                            } else {
                                ScriptEngineManager sem = new ScriptEngineManager();
                                ScriptEngine engine = sem.getEngineByName("js");
                                final Object eval = engine.eval(reg, bindings);
                                // jsonObject.put(entry.getValue(), eval);
                                innerMap.put(entry.getKey(), eval);
                            }

                        } catch (ScriptException e) {
                            log.error("eval处理数据异常{}", collect.get(map.get(entry.getKey())));
                            throw new Exception("eval处理数据异常" + collect.get(map.get(entry.getKey())));
                        }
                    }
                    //常量
                    if (null != upstreamTypeFields && upstreamTypeFields.size() > 0) {
                        for (UpstreamTypeField field : upstreamTypeFields) {
                            innerMap.put(field.getSourceField(), field.getTargetField());
                        }
                    }
                    //处理entity
                    for (Map.Entry<String, List<String>> entry : ignoreMap.entrySet()) {
                        try {

                            //处理前缀
                            SimpleBindings bindings = new SimpleBindings();
                            for (int i = 0; i < entry.getValue().size(); i++) {
                                if (!entry.getValue().get(i).contains("ENTITY")) {
                                    bindings.put("$" + entry.getValue().get(i), null == innerMap.get(entry.getValue().get(i)) ? "" : innerMap.get(entry.getValue().get(i)));
                                } else {
                                    //判断类型实体
                                    if ("dept".equals(upstreamType.getSynType()) || "post".equals(upstreamType.getSynType())) {
                                        if (!CollectionUtils.isEmpty(treeMap) && treeMap.containsKey(innerMap.get("code"))) {
                                            TreeBean code = treeMap.get(innerMap.get("code"));
                                            bindings.put("$ENTITY", code);
                                        } else {
                                            bindings.put("$ENTITY", new TreeBean());
                                        }
                                    } else if ("person".equals(upstreamType.getSynType())) {
                                        if (!CollectionUtils.isEmpty(personCardMap) && personCardMap.containsKey(innerMap.get("cardType") + ":" + innerMap.get("cardNo"))) {
                                            Person code = personCardMap.get(innerMap.get("cardType") + ":" + innerMap.get("cardNo"));
                                            bindings.put("$ENTITY", code);
                                        } else if (!CollectionUtils.isEmpty(personAccountMap) && personAccountMap.containsKey(innerMap.get("accountNo"))) {
                                            Person code = personAccountMap.get(innerMap.get("accountNo"));
                                            bindings.put("$ENTITY", code);
                                        } else {
                                            bindings.put("$ENTITY", new Person());
                                        }

                                    } else if ("occupy".equals(upstreamType.getSynType())) {

                                        //人员标识是否有有效数据
                                        if (!CollectionUtils.isEmpty(personCardMap) && personCardMap.containsKey(innerMap.get("personCardType") + ":" + innerMap.get("personCardNo"))) {
                                            Person code = personCardMap.get(innerMap.get("personCardType") + ":" + innerMap.get("personCardNo"));
                                            if (!CollectionUtils.isEmpty(occupyDtoMap) && occupyDtoMap.containsKey(code.getId() + ":" + innerMap.get("postCode") + ":" + innerMap.get("deptCode"))) {
                                                OccupyDto occupyDto = occupyDtoMap.get(code.getId() + ":" + innerMap.get("postCode") + ":" + innerMap.get("deptCode"));
                                                bindings.put("$ENTITY", occupyDto);
                                            } else {
                                                bindings.put("$ENTITY", new OccupyDto());
                                            }
                                        } else if (!CollectionUtils.isEmpty(personAccountMap) && personAccountMap.containsKey(innerMap.get("accountNo"))) {
                                            Person code = personAccountMap.get(innerMap.get("accountNo"));
                                            if (!CollectionUtils.isEmpty(occupyDtoAccountMap) && occupyDtoAccountMap.containsKey(code.getId() + ":" + innerMap.get("accountNo"))) {
                                                OccupyDto occupyDto = occupyDtoAccountMap.get(code.getId() + ":" + innerMap.get("accountNo"));
                                                bindings.put("$ENTITY", occupyDto);
                                            } else {
                                                bindings.put("$ENTITY", new OccupyDto());
                                            }
                                        } else {
                                            bindings.put("$ENTITY", new OccupyDto());
                                        }
                                    }
                                }
                            }
                            String reg = collect.get(entry.getKey());
                            if (reg.startsWith("=")) {
                                reg = reg.substring(1);
                            }
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
                    resultJson.add(innerMap);

                }
                log.info("-------------处理上游数据映射结束");


                return resultJson;
            }
        }
        return null;
    }

    private Integer doQueryData(UpstreamType upstreamType, GraphqlClient graphqlClient, JSONArray objects, GraphqlQuery query) {
        Map<String, Object> result;
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
        Integer totalCount = (Integer) deptMap.get("totalCount");
        if (null != deptArray) {
            for (Object deptOb : deptArray) {
                JSONObject nodeJson = (JSONObject) deptOb;
                JSONObject node1 = nodeJson.getJSONObject("node");

                objects.add(node1);
            }
        }
        return totalCount;
    }


    public Map<String, Map<String, String>> getDataMap(String url) throws Exception {


        Map<String, Map<String, String>> dataMapFieldMap = new HashMap<>();

        //Query
        JSONObject params = new JSONObject();
        String graphql = "query dataMaps($first: Int, $offset: Int, $filter: dataMapFilter) {" +
                "  dataMaps(first: $first, offset: $offset, filter: $filter) {" +
                "    totalCount" +
                "    edges {" +
                "      node {" +
                "        code" +
                "        fields {" +
                "          fromData" +
                "          toData" +
                "        }" +
                "      }" +
                "    }" +
                "  }" +
                "}";
        JSONObject variables = new JSONObject();
        variables.put("offset", 0);
        variables.put("first", 10000);
        params.put("query", graphql);
        params.put("variables", variables);

        url = UrlUtil.getUrl(url);


        String s = sendPostRequest(url, params);
        if (null == s || s.contains("errors")) {
            log.error("获取代码映射数据失败");
            throw new CustomException(ResultCode.GET_DATA_ERROR, null, null, s);
        }
        JSONObject dataMaps = JSONObject.parseObject(s);
        if (null != dataMaps && dataMaps.containsKey("data") && dataMaps.getJSONObject("data").getJSONObject("dataMaps").getInteger("totalCount") >= 1) {
            JSONArray jsonArray = dataMaps.getJSONObject("data").getJSONObject("dataMaps").getJSONArray("edges");
            List<DataMapNode> dataMapNodes = jsonArray.toJavaList(DataMapNode.class);
            for (DataMapNode dataMapNode : dataMapNodes) {
                Map<String, String> fieldMap = dataMapNode.getNode().getFields().stream().collect(Collectors.toMap(DataMapField::getFromData, DataMapField::getToData));
                dataMapFieldMap.put(dataMapNode.getNode().getCode(), fieldMap);
            }
            return dataMapFieldMap;
        }
        return null;

    }

    public Map getDataByBus(UpstreamType upstreamType, Integer offset, Integer first, DomainInfo domain) throws Exception {

        //todo通过token内省获取域名
        HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();

        DomainInfo introspect = certifiedConnector.introspect(request);


        //获取token
        String key = getToken(introspect.getDomainName());
        String[] split = upstreamType.getGraphqlUrl().split("/");

        //根据url 获取请求地址
        String substring = new StringBuffer(UrlUtil.getUrl(busUrl)).append("?access_token=").append(key)
                .append("&domain=").append(domain.getDomainName()).toString();
        //工具类过滤处理url
        String dealUrl = UrlUtil.getUrl(substring);


        //调用获取资源url
        String dataUrl = invokeUrl(dealUrl, split);
        //请求获取资源
        String u = dataUrl + "/" + "?access_token=" + key + "&domain=" + domain.getDomainName();

        //调用获取资源url
        String dataMapUrl = invokeUrl(dealUrl, new String[]{"", "", "catalog"});
        //
        Map<String, Map<String, String>> dataMap = null;
        if (StringUtils.isNotBlank(dataMapUrl)) {
            log.info("无法内省 catalog 服务地址");
            dataMap = getDataMap(dataMapUrl + "?access_token=" + key + "&domain=" + domain.getDomainName());
        }
        return invokeForMapData(UrlUtil.getUrl(u), upstreamType, offset, first, domain.getDomainName(), dataMap);
    }


    private Map invokeForMapData(String dataUrl, UpstreamType upstreamType, Integer offset, Integer first, String domainName, Map<String, Map<String, String>> dataMapField) throws Exception {
        log.info("source url " + dataUrl);
        //获取字段映射
        List<UpstreamTypeField> fields = upstreamTypeService.findFields(upstreamType.getId());
        String[] type = upstreamType.getGraphqlUrl().split("/");
        GraphqlClient graphqlClient = GraphqlClient.buildGraphqlClient(dataUrl);
        //存储忽略值映射需要数据库数据覆盖的
        HashMap<String, List<String>> ignoreMap = new HashMap<>();
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
                //ENTITY校验
                String enPattern = "\\$[a-zA-Z0-9_]+.[a-zA-Z0-9_]+";
                Pattern enR = Pattern.compile(enPattern);
                Matcher enM = enR.matcher(field.getTargetField());
                // 表达式 最终需要进行运算
                if (field.getTargetField().contains("=")) {
                    // 普通表达式
                    List<String> groupList = new ArrayList<>();
                    while (m.find()) {
                        log.info("Found value: " + m.group(0));
                        if (!m.group(0).equals("$ENTITY")) {
                            groupList.add(m.group(0).substring(1));
                            nodeMap.put(m.group(0).substring(1), m.group(0).substring(1));
                        }
                        if (!field.getTargetField().contains("$ENTITY")) {
                            nodeMap.put(m.group(0).substring(1), m.group(0).substring(1));
                        }
                    }
                    if (!field.getTargetField().contains("$ENTITY")) {
                        map.put(field.getSourceField(), groupList);
                    } else {
                        while (enM.find()) {
                            log.info("Ignore Found value: " + enM.group(0));
                            groupList.add(enM.group(0).substring(1));
                        }
                        ignoreMap.put(field.getSourceField(), groupList);
                    }

                    // 表达式 最终需要进行运算
                } else if (!field.getTargetField().contains("=") && field.getTargetField().contains("$ENTITY")) {
                    // 覆盖映射，查询后进行字段值覆盖
                    if (enM.find()) {
                        List<String> groupList = new ArrayList<>();
                        System.out.println("Ignore Found value: " + enM.group(0));
                        // node.addResultAttributes(field.getSourceField() + ":" + m.group(0).substring(1));
                        groupList.add(enM.group(0).substring(1));
                        ignoreMap.put(field.getSourceField(), groupList);
                    }
                } else if (!field.getTargetField().contains("=") && field.getTargetField().contains("$")) {
                    // 简单映射，直接查询时候进行重命名
                    if (m.find()) {
                        System.out.println("Found value: " + m.group(0));
                        // node.addResultAttributes(field.getSourceField() + ":" + m.group(0).substring(1));
                        nodeMap.put(field.getSourceField() + ":" + m.group(0).substring(1), m.group(0).substring(1));
                        //nodeMap.put(m.group(0).substring(1), m.group(0).substring(1));

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
                    throw new CustomException(ResultCode.FAILED, "类型:" + upstreamType.getSynType() + "权威源类型:" + upstreamType.getId() + ",查询数据失败,请检查权威源配置");
                }
            }

            if (null == result || null == result.get("data")) {

                throw new CustomException(ResultCode.FAILED, "类型:" + upstreamType.getSynType() + "权威源类型:" + upstreamType.getId() + ",映射字段错误,数据获取失败");

            }

            Map dataMap = (Map) result.get("data");
            Map deptMap = (Map) dataMap.get(upstreamType.getSynType());
            JSONArray deptArray = (JSONArray) JSONArray.toJSON(deptMap.get("edges"));
            Integer totalCount = (Integer) deptMap.get("totalCount");
            log.info("类型:{},权威源类型:{},获取数据成功,总计:{}条", upstreamType.getSynType(), upstreamType.getId(), totalCount);
            if (null != deptArray) {
                for (Object deptOb : deptArray) {
                    JSONObject nodeJson = (JSONObject) deptOb;
                    objects.add(nodeJson.getJSONObject("node"));
                }
            }

            rs = new ArrayList<>();
            Map<String, String> collect = fields.stream().collect(Collectors.toMap(UpstreamTypeField::getSourceField, UpstreamTypeField::getTargetField));
            //获取sso数据
            Tenant tenant = tenantDao.findByDomainName(domainName);
            if (null == tenant) {
                throw new CustomException(ResultCode.FAILED, "租户不存在");
            }
            //数据库数据存储容器
            List<TreeBean> beans;
            Map<String, TreeBean> treeMap = new HashMap<>();
            List<Person> persons;
            Map<String, Person> personCardMap = new HashMap<>();
            Map<String, Person> personAccountMap = new HashMap<>();
            List<OccupyDto> occupyDtos;
            Map<String, OccupyDto> occupyDtoMap = new HashMap<>();
            Map<String, OccupyDto> occupyDtoAccountMap = new HashMap<>();
            if ("dept".equals(upstreamType.getSynType())) {
                beans = deptDao.findByTenantId(tenant.getId(), null, null);
                if (!CollectionUtils.isEmpty(beans)) {
                    treeMap = beans.stream().collect(Collectors.toMap(TreeBean::getCode, bean -> bean));
                }
            } else if ("post".equals(upstreamType.getSynType())) {
                beans = postDao.findByTenantId(tenant.getId());
                if (!CollectionUtils.isEmpty(beans)) {
                    treeMap = beans.stream().collect(Collectors.toMap(TreeBean::getCode, bean -> bean));
                }

            } else if ("person".equals(upstreamType.getSynType())) {
                persons = personDao.getAll(tenant.getId());
                if (!CollectionUtils.isEmpty(persons)) {
                    personAccountMap = persons.stream().filter(person ->
                            !StringUtils.isBlank(person.getAccountNo())).collect(Collectors.toMap(Person::getAccountNo, person -> person, (v1, v2) -> v2));
                    personCardMap = persons.stream().filter(person -> !StringUtils.isBlank(person.getCardType()) && !StringUtils.isBlank(person.getCardNo())).collect(Collectors.toMap(person -> (person.getCardType() + ":" + person.getCardNo()), person -> person, (v1, v2) -> v2));
                }

            } else if ("occupy".equals(upstreamType.getSynType())) {
                //获取sso的所有人员
                persons = personDao.getAll(tenant.getId());
                if (!CollectionUtils.isEmpty(persons)) {
                    personAccountMap = persons.stream().filter(person ->
                            !StringUtils.isBlank(person.getAccountNo())).collect(Collectors.toMap(Person::getAccountNo, person -> person, (v1, v2) -> v2));
                    personCardMap = persons.stream().filter(person -> !StringUtils.isBlank(person.getCardType()) && !StringUtils.isBlank(person.getCardNo())).collect(Collectors.toMap(person -> (person.getCardType() + ":" + person.getCardNo()), person -> person, (v1, v2) -> v2));
                }
                occupyDtos = occupyDao.findAll(tenant.getId(), null, null);
                if (!CollectionUtils.isEmpty(occupyDtos)) {
                    occupyDtoMap = occupyDtos.stream().filter(occupyDto -> !StringUtils.isBlank(occupyDto.getPostCode()) && !StringUtils.isBlank(occupyDto.getDeptCode())).collect(Collectors.toMap(occupyDto -> (occupyDto.getPersonId() + ":" + occupyDto.getPostCode() + ":" + occupyDto.getDeptCode()), occupyDto -> occupyDto, (v1, v2) -> v2));
                    occupyDtoAccountMap = occupyDtos.stream().filter(occupyDto -> !StringUtils.isBlank(occupyDto.getAccountNo())).collect(Collectors.toMap(occupyDto -> (occupyDto.getPersonId() + ":" + occupyDto.getAccountNo()), occupyDto -> occupyDto, (v1, v2) -> v2));
                }


            }
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
                        // 如果是map 表达式 =map(XX,DataMapCode)
                        if (reg.startsWith("map(") && reg.endsWith(")")) {
                            if (null == dataMapField || dataMapField.size() <= 0) {
                                throw new Exception("无法进行代码映射，请检查" + entry.getKey() + "字段映射关系。");
                            }
                            // Pattern pattern = Pattern.compile("(?<=\\=map\\()[^\\)]+");
                            String v = reg.substring(4, reg.length() - 1);
                            final String[] vSplit = v.split(",");
                            if (vSplit.length != 2) {
                                throw new Exception("数据映射表达式异常");
                            }
                            String value = bindings.get(vSplit[0]).toString();
                            // 代码表
                            if (dataMapField.containsKey(vSplit[1])) {
                                if (dataMapField.get(vSplit[1]).containsKey(value)) {
                                    innerMap.put(entry.getKey(), dataMapField.get(vSplit[1]).get(value));
                                } else if (dataMapField.get(vSplit[1]).containsKey("=other()")) {
                                    String reg2 = dataMapField.get(vSplit[1]).get("=other()");
                                    if (reg2.equals("=old()")) {
                                        innerMap.put(entry.getKey(), value);
                                    } else {
                                        reg2 = reg2.substring(1);
                                        ScriptEngineManager sem = new ScriptEngineManager();
                                        ScriptEngine engine = sem.getEngineByName("js");
                                        final Object eval = engine.eval(reg2, bindings);
                                        innerMap.put(entry.getKey(), eval);
                                    }
                                }

                            }

                        } else {
                            ScriptEngineManager sem = new ScriptEngineManager();
                            ScriptEngine engine = sem.getEngineByName("js");
                            final Object eval = engine.eval(reg, bindings);
                            // jsonObject.put(entry.getValue(), eval);
                            innerMap.put(entry.getKey(), eval);
                        }

                    } catch (ScriptException e) {
                        log.error("eval处理数据异常{}", collect.get(map.get(entry.getKey())));
                        throw new Exception("eval处理数据异常" + collect.get(map.get(entry.getKey())));
                    }
                }
                //常量
                if (null != upstreamTypeFields && upstreamTypeFields.size() > 0) {
                    for (UpstreamTypeField field : upstreamTypeFields) {
                        //jsonObject.put(field.getSourceField(), field.getTargetField());
                        innerMap.put(field.getSourceField(), field.getTargetField());
                    }
                }
                //处理entity
                for (Map.Entry<String, List<String>> entry : ignoreMap.entrySet()) {
                    try {
                        //处理前缀
                        SimpleBindings bindings = new SimpleBindings();
                        for (int i = 0; i < entry.getValue().size(); i++) {
                            if (!entry.getValue().get(i).contains("ENTITY")) {
                                bindings.put("$" + entry.getValue().get(i), null == innerMap.get(entry.getValue().get(i)) ? "" : innerMap.get(entry.getValue().get(i)));
                            } else {
                                //判断类型实体
                                if ("dept".equals(upstreamType.getSynType()) || "post".equals(upstreamType.getSynType())) {
                                    if (!CollectionUtils.isEmpty(treeMap) && treeMap.containsKey(innerMap.get("code"))) {
                                        TreeBean code = treeMap.get(innerMap.get("code"));
                                        bindings.put("$ENTITY", code);
                                    } else {
                                        bindings.put("$ENTITY", new TreeBean());
                                    }
                                } else if ("person".equals(upstreamType.getSynType())) {
                                    if (!CollectionUtils.isEmpty(personCardMap) && personCardMap.containsKey(innerMap.get("cardType") + ":" + innerMap.get("cardNo"))) {
                                        Person code = personCardMap.get(innerMap.get("cardType") + ":" + innerMap.get("cardNo"));
                                        bindings.put("$ENTITY", code);
                                    } else if (!CollectionUtils.isEmpty(personAccountMap) && personAccountMap.containsKey(innerMap.get("accountNo"))) {
                                        Person code = personAccountMap.get(innerMap.get("accountNo"));
                                        bindings.put("$ENTITY", code);
                                    } else {
                                        bindings.put("$ENTITY", new Person());
                                    }

                                } else if ("occupy".equals(upstreamType.getSynType())) {

                                    //人员标识是否有有效数据
                                    if (!CollectionUtils.isEmpty(personCardMap) && personCardMap.containsKey(innerMap.get("personCardType") + ":" + innerMap.get("personCardNo"))) {
                                        Person code = personCardMap.get(innerMap.get("personCardType") + ":" + innerMap.get("personCardNo"));
                                        if (!CollectionUtils.isEmpty(occupyDtoMap) && occupyDtoMap.containsKey(code.getId() + ":" + innerMap.get("postCode") + ":" + innerMap.get("deptCode"))) {
                                            OccupyDto occupyDto = occupyDtoMap.get(code.getId() + ":" + innerMap.get("postCode") + ":" + innerMap.get("deptCode"));
                                            bindings.put("$ENTITY", occupyDto);
                                        } else {
                                            bindings.put("$ENTITY", new OccupyDto());
                                        }
                                    } else if (!CollectionUtils.isEmpty(personAccountMap) && personAccountMap.containsKey(innerMap.get("accountNo"))) {
                                        Person code = personAccountMap.get(innerMap.get("accountNo"));
                                        if (!CollectionUtils.isEmpty(occupyDtoAccountMap) && occupyDtoAccountMap.containsKey(code.getId() + ":" + innerMap.get("accountNo"))) {
                                            OccupyDto occupyDto = occupyDtoAccountMap.get(code.getId() + ":" + innerMap.get("accountNo"));
                                            bindings.put("$ENTITY", occupyDto);
                                        } else {
                                            bindings.put("$ENTITY", new OccupyDto());
                                        }
                                    } else {
                                        bindings.put("$ENTITY", new OccupyDto());
                                    }
                                }
                            }
                        }
                        String reg = collect.get(entry.getKey());
                        if (reg.startsWith("=")) {
                            reg = reg.substring(1);
                        }
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
                LinkedHashMap<String, Object> stringObjectLinkedHashMap = new LinkedHashMap<>();
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
                        "    type : \"%s\"," +
                        "    source : \"%s\", " +
                        "    subject : \"%s\", " +
                        "    id : \"%s\",         " +
                        "    time : \"%s\",  " +
                        "    datacontenttype : \"application/json\",        " +
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
                graphql.append(RandomStringUtils.randomAlphabetic(20) + ":pub(message:" + pub + "){id}");
            }

        } else if ("person".equals(type)) {
            List<Person> insertPersonList = personMap.get("insert");
            if (null != insertPersonList && insertPersonList.size() > 0) {
                for (Person person : insertPersonList) {
                    String pub = "{" +
                            "    type : \"%s\"," +
                            "    source : \"%s\", " +
                            "    subject : \"%s\", " +
                            "    id : \"%s\",         " +
                            "    time : \"%s\",  " +
                            "    datacontenttype : \"application/json\",        " +
                            "    data : \"%s\", " +
                            "}";
                    pub = String.format(pub, "person.created", domain.getClientId(),
                            person.getOpenId(), UUID.randomUUID(),
                            person.getCreateTime().toEpochSecond(ZoneOffset.of("+8")), JSONObject.toJSONString(person).replace("\"", "\\\""));
                    graphql.append(RandomStringUtils.randomAlphabetic(20) + ":pub(message:" + pub + "){id}");
                }
            }
            List<Person> updatePersonList = personMap.get("update");
            if (null != updatePersonList && updatePersonList.size() > 0) {
                for (Person person : updatePersonList) {
                    String pub = "{" +
                            "    type : \"%s\"," +
                            "    source : \"%s\", " +
                            "    subject : \"%s\", " +
                            "    id : \"%s\",         " +
                            "    time : \"%s\",  " +
                            "    datacontenttype : \"application/json\",        " +
                            "    data : \"%s\", " +
                            "}";
                    pub = String.format(pub, "person.updated", domain.getClientId(),
                            person.getOpenId(), UUID.randomUUID(),
                            person.getCreateTime().toEpochSecond(ZoneOffset.of("+8")), JSONObject.toJSONString(person).replace("\"", "\\\""));
                    graphql.append(RandomStringUtils.randomAlphabetic(20) + ":pub(message:" + pub + "){id}");
                }
            }
            List<Person> deletePersonList = personMap.get("delete");
            if (null != deletePersonList && deletePersonList.size() > 0) {
                for (Person person : deletePersonList) {
                    String pub = "{" +
                            "    type : \"%s\"," +
                            "    source : \"%s\", " +
                            "    subject : \"%s\", " +
                            "    id : \"%s\",         " +
                            "    time : \"%s\",  " +
                            "    datacontenttype : \"application/json\",        " +
                            "    data : \"%s\", " +
                            "}";
                    pub = String.format(pub, "person.deleted", domain.getClientId(),
                            person.getOpenId(), UUID.randomUUID(),
                            person.getCreateTime().toEpochSecond(ZoneOffset.of("+8")), JSONObject.toJSONString(person).replace("\"", "\\\""));
                    graphql.append(RandomStringUtils.randomAlphabetic(20) + ":pub(message:" + pub + "){id}");
                }
            }

        } else if ("occupy".equals(type)) {
            List<OccupyDto> insert = occupyMap.get("insert");
            if (null != insert && insert.size() > 0) {
                for (OccupyDto occupy : insert) {
                    String pub = "{" +
                            "    type : \"%s\"," +
                            "    source : \"%s\", " +
                            "    subject : \"%s\", " +
                            "    id : \"%s\",         " +
                            "    time : \"%s\",  " +
                            "    datacontenttype : \"application/json\",        " +
                            "    data : \"%s\", " +
                            "}";
                    pub = String.format(pub, "user.position.created", domain.getClientId(),
                            occupy.getOccupyId(), UUID.randomUUID(),
                            occupy.getCreateTime().toEpochSecond(ZoneOffset.of("+8")), JSONObject.toJSONString(occupy).replace("\"", "\\\""));
                    graphql.append(RandomStringUtils.randomAlphabetic(20) + ":pub(message:" + pub + "){id}");
                }
            }
            List<OccupyDto> update = occupyMap.get("update");
            if (null != update && update.size() > 0) {
                for (OccupyDto occupy : update) {
                    String pub = "{" +
                            "    type : \"%s\"," +
                            "    source : \"%s\", " +
                            "    subject : \"%s\", " +
                            "    id : \"%s\",         " +
                            "    time : \"%s\",  " +
                            "    datacontenttype : \"application/json\",        " +
                            "    data : \"%s\", " +
                            "}";
                    pub = String.format(pub, "user.position.updated", domain.getClientId(),
                            occupy.getOccupyId(), UUID.randomUUID(),
                            occupy.getCreateTime().toEpochSecond(ZoneOffset.of("+8")), JSONObject.toJSONString(occupy).replace("\"", "\\\""));
                    graphql.append(RandomStringUtils.randomAlphabetic(20) + ":pub(message:" + pub + "){id}");
                }
            }
            List<OccupyDto> delete = occupyMap.get("delete");
            if (null != delete && delete.size() > 0) {
                for (OccupyDto occupy : delete) {
                    String pub = "{" +
                            "    type : \"%s\"," +
                            "    source : \"%s\", " +
                            "    subject : \"%s\", " +
                            "    id : \"%s\",         " +
                            "    time : \"%s\",  " +
                            "    datacontenttype : \"application/json\",        " +
                            "    data : \"%s\", " +
                            "}";
                    pub = String.format(pub, "user.position.deleted", domain.getClientId(),
                            occupy.getOccupyId(), UUID.randomUUID(),
                            occupy.getCreateTime().toEpochSecond(ZoneOffset.of("+8")), JSONObject.toJSONString(occupy).replace("\"", "\\\""));
                    graphql.append(RandomStringUtils.randomAlphabetic(20) + ":pub(message:" + pub + "){id}");
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
        return sendPostRequest(busUrl + "?access_token=" + token + "&domain=" + domain.getDomainName(), params);

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
            throw new CustomException(ResultCode.FAILED, "token 获取失败:" + e.getMessage());
        }
        OAuthClient oAuthClient = new OAuthClient(new SSLConnectionClient());
        OAuthJSONAccessTokenResponse oAuthClientResponse = null;
        try {
            oAuthClientResponse = oAuthClient.accessToken(oAuthClientRequest, "POST", OAuthJSONAccessTokenResponse.class);
        } catch (OAuthSystemException | OAuthProblemException e) {
            log.error("token 获取" + e.getMessage());
            e.printStackTrace();
            throw new CustomException(ResultCode.FAILED, "token 获取失败:" + e.getMessage());
        }
        assert oAuthClientResponse != null;
        String accessToken = oAuthClientResponse.getAccessToken();
        long exp = System.currentTimeMillis() + (oAuthClientResponse.getExpiresIn() * 1000 - (10 * 60 * 1000));
        tokenMap.put(domainInfo.getDomainName(), new Token(oAuthClientResponse.getAccessToken(), exp, System.currentTimeMillis()));
        return accessToken;
    }

    private Timestamp processTime(UpstreamType upstreamType) {
        //获取最近一次增量数据同步的日志信息
        List<IncrementalTask> incrementalTasks = incrementalTaskService.findByDomainAndType(upstreamType.getDomain(), upstreamType.getSynType(), upstreamType.getId());
        long maxTime;
        //long now = System.currentTimeMillis();
        if (!CollectionUtils.isEmpty(incrementalTasks)) {
            //取最近一次的同步结束时间作为时间起始点
            maxTime = incrementalTasks.get(0).getTime().getTime();
        } else {
            //如果没有则默认走全量
            return null;
        }
        //maxTime = Math.min(now, maxTime);
        ////当返回来的数据的MAX.TIME超过48小时，下次同步时间不会去取超过48小时之外的数据
        //maxTime = Math.max(now - 2 * 24 * 60 * 60 * 1000, maxTime);

        return new Timestamp(maxTime);
    }
}
