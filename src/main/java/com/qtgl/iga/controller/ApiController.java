package com.qtgl.iga.controller;


import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.qtgl.iga.bean.OccupyDto;
import com.qtgl.iga.bean.TreeBean;
import com.qtgl.iga.bo.*;
import com.qtgl.iga.dao.TenantDao;
import com.qtgl.iga.dao.UpstreamDao;
import com.qtgl.iga.dao.impl.UpstreamTypeDaoImpl;
import com.qtgl.iga.service.*;
import com.qtgl.iga.task.TaskConfig;
import com.qtgl.iga.utils.CertifiedConnector;
import com.qtgl.iga.utils.DataBusUtil;
import com.qtgl.iga.utils.UrlUtil;
import com.qtgl.iga.utils.enumerate.ResultCode;
import com.qtgl.iga.utils.exception.CustomException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author rcc
 */
@Controller
@RequestMapping("/api")
@Slf4j
public class ApiController {

    public static Logger logger = LoggerFactory.getLogger(ApiController.class);

    @Autowired
    DomainInfoService domainInfoService;

    @Autowired
    DataBusUtil dataBusUtil;

    @Value("${bus.url}")
    private String busUrl;
    @Autowired
    private UpstreamService upstreamService;
    @Autowired
    UpstreamDao upstreamDao;
    @Autowired
    UpstreamTypeDaoImpl upstreamTypeDao;
    @Autowired
    TenantDao tenantDao;
    @Autowired
    DsConfigService dsConfigService;
    @Autowired
    NodeRulesService nodeRulesService;
    @Autowired
    TaskConfig taskConfig;
    @Autowired
    TaskLogService taskLogService;
    @Autowired
    DeptService deptService;
    @Autowired
    PostService postService;
    @Autowired
    PersonService personService;
    @Autowired
    OccupyService occupyService;


    @PostMapping("/event")
    @ResponseBody
    public JSONObject event(@RequestBody JSONObject jsonObject) {
        String clientId = jsonObject.getString("clientId");
        String authCode = jsonObject.getString("authCode");
        String timestamp = jsonObject.getString("timestamp");
        String eventType = jsonObject.getString("eventType");
        String tenantId = jsonObject.getString("tenantId");


        switch (eventType) {
            //租户启用
            case "create_tenant":
                //通过 clientid authCode 换取密钥【接口暂无】
                String clientSecret = authCode;
                // 创建租户
                DomainInfo domainInfo = new DomainInfo();
                domainInfo.setId(UUID.randomUUID().toString());
                domainInfo.setDomainName(tenantId);
                domainInfo.setClientId(clientId);
                domainInfo.setClientSecret(clientSecret);
                domainInfo.setStatus(0);
                domainInfo.setCreateTime(new Timestamp(System.currentTimeMillis()));
                try {
                    //授权
                    // this.domainAuthorization(clientId, domainInfo);
                    domainInfoService.install(domainInfo);
                    //GraphQLService.setDomainGraphQLMap(runner.buildGraphql());
                } catch (CustomException e) {
                    e.printStackTrace();
                    logger.error("create tenant error{}", e);
                    JSONObject jo = new JSONObject();
                    jo.put("success", false);
                    jo.put("msg", e.getErrorMsg());
                    return jo;
                } catch (Exception e) {
                    e.printStackTrace();
                    logger.error("create tenant error{}", e);
                    JSONObject jo = new JSONObject();
                    jo.put("success", false);
                    jo.put("msg", e.getMessage());
                    return jo;
                }
                break;
            // 禁用租户
            case "disable_tenant":

                break;

            // 删除租户
            case "delete_tenant":

                break;


        }
        return JSONObject.parseObject("{\"success\":true}");
    }

    public void domainAuthorization(String clientId, DomainInfo domainInfo) throws Exception {
        String token = null;
        try {
            token = dataBusUtil.getApiToken(domainInfo);
        } catch (Exception e) {
            e.printStackTrace();
            throw new CustomException(ResultCode.FAILED, "oauth认证失败,请查看应用权限");
        }
        String url = busUrl + "?access_token=" + token + "&domain=" + domainInfo.getDomainName();
        url = UrlUtil.getUrl(url);
        String graphql = "mutation M1 {" +
                "  addRoleBinding(roleBindings: [{" +
                "      app: {" +
                "        clientId: \"" + clientId + "\"" +
                "      }" +
                "      role: {" +
                "        name: \"iga-super-admin\"" +
                "        rules: [{" +
                "          service: {" +
                "            name: \"*\"" +
                "          }" +
                "          type: \"*\"" +
                "          permission: \"*\"" +
                "          filter: {" +
                "          }" +
                "        }]" +
                "      }" +
                "      source: \"LOCAL\"" +
                "    }]){" +
                "    user{" +
                "      account" +
                "    }" +
                "    role{" +
                "      rules{" +
                "        filter" +
                "      }" +
                "    }" +
                "  }" +
                "}";
        JSONObject params = new JSONObject();
        params.put("query", graphql);

        try {
            dataBusUtil.sendPostRequest(url, params);
        } catch (CustomException e) {
            throw new CustomException(ResultCode.FAILED, "请检查网络或检查该应用权限是否匹配");
        }

    }

    @PostMapping(value = "/bootstrap")
    @ResponseBody
    public JSONObject postBootstrap(HttpServletRequest request) {
        String resource = request.getParameter("resource");
        logger.info(resource);
        //resource = "{\n" +
        //        "  \"apiVersion\": \"apiextensions.k8s.io/v1\",\n" +
        //        "  \"kind\": \"CustomResourceDefinition\",\n" +
        //        "  \"metadata\": {\n" +
        //        "    \"name\": \"qdatasource.ketanyun.cn\"\n" +
        //        "  },\n" +
        //        "  \"spec\": {\n" +
        //        "    \"type\": \"object\",\n" +
        //        "    \"required\": [\n" +
        //        "      \"upstream\"\n" +
        //        "    ],\n" +
        //        "    \"upstream\": {\n" +
        //        "      \"code\": \"0428\",\n" +
        //        "      \"name\": \"LQZTEST\",\n" +
        //        "      \"dataCode\": \" \",\n" +
        //        "      \"color\": \" \",\n" +
        //        "      \"tenant\": \"devel.ketanyun.cn\",\n" +
        //        "      \"upstreamTypes\": {\n" +
        //        "      \"dept\": [\n" +
        //        "        {\n" +
        //        "          \"treeType\": \"01\",\n" +
        //        "          \"description\": \"0428测试部门\",\n" +
        //        "          \"clientId\": \"gDGqfDGm8OrJAXCFDiuI\",\n" +
        //        "          \"node\": {\n" +
        //        "               \"code\": \" 01\",\n" +
        //        "                 \"treeType\": \"01\"\n" +
        //        "            }\n" +
        //        "        }\n" +
        //        "      ],\n" +
        //        "		\"post\": [\n" +
        //        "        {\n" +
        //        "          \"description\": \"0428测试岗位\",\n" +
        //        "          \"clientId\": \"gDGqfDGm8OrJAXCFDiuI\",\n" +
        //        "          \"node\": {\n" +
        //        "                \"code\": \" \" \n" +
        //        "            }\n" +
        //        "        }\n" +
        //        "      ],\n" +
        //        "		\"person\": [\n" +
        //        "        {\n" +
        //        "          \"description\": \"0428测试人员\",\n" +
        //        "          \"clientId\": \"gDGqfDGm8OrJAXCFDiuI\" \n" +
        //        "        }\n" +
        //        "      ],\n" +
        //        " 		\"occupy\": [\n" +
        //        "        {\n" +
        //        "          \"description\": \"0428测试人员身份\",\n" +
        //        "          \"clientId\": \"gDGqfDGm8OrJAXCFDiuI\" \n" +
        //        "        }\n" +
        //        "      ]\n" +
        //        "    }\n" +
        //        "    }\n" +
        //        "  }\n" +
        //        "}"
        ;

        JSONObject jsonObject = JSONObject.parseObject(resource);
        JSONObject result = new JSONObject();

        try {
            //check(jsonObject);
            JSONObject metadata = jsonObject.getJSONObject("metadata");
            JSONObject spec = jsonObject.getJSONObject("spec");
            JSONObject upstreamJson = spec.getJSONObject("upstream");
            JSONObject upstreamTypesJson = upstreamJson.getJSONObject("upstreamTypes");

            if (metadata == null || spec == null || null == upstreamJson || null == upstreamTypesJson) {
                throw new CustomException(ResultCode.INVALID_PARAMETER, "INVALID_PARAMETER");
            }

            String tenant = upstreamJson.getString("tenant");
            DomainInfo domainInfo = new DomainInfo();
            //检查租户
            if (StringUtils.isBlank(tenant)) {
                throw new CustomException(ResultCode.INVALID_PARAMETER, "INVALID_PARAMETER");
            } else {
                domainInfo = domainInfoService.getByDomainName(tenant);
                if (domainInfo == null) {
                    throw new CustomException(ResultCode.INVALID_PARAMETER, "INVALID_DOMAIN");
                }
            }
            //处理权威源
            Upstream upstream = getUpstream(upstreamJson, domainInfo);
            //判重
            upstreamDao.findByAppNameAndAppCode(upstream.getAppName(), upstream.getAppCode(), domainInfo.getId());
            //处理权威源类型及node
            JSONArray deptJson = upstreamTypesJson.getJSONArray("dept");
            JSONArray postJson = upstreamTypesJson.getJSONArray("post");
            JSONArray personJson = upstreamTypesJson.getJSONArray("person");
            JSONArray occupyJson = upstreamTypesJson.getJSONArray("occupy");
            List<UpstreamType> upstreamTypes = new ArrayList<>();
            List<Node> nodes = new ArrayList<>();
            List<NodeRules> nodeRulesList = new ArrayList<>();
            long now = System.currentTimeMillis();
            //组织机构
            if (null != deptJson) {
                dealUpstreamTypeAndNodes(deptJson,"dept",now,domainInfo,upstreamTypes,nodes,nodeRulesList,upstream);
            }
            if (null != postJson) {
                dealUpstreamTypeAndNodes(postJson,"post",now,domainInfo,upstreamTypes,nodes,nodeRulesList,upstream);
            }
            if (null != personJson) {
                dealUpstreamTypeAndNodes(personJson,"person",now,domainInfo,upstreamTypes,nodes,nodeRulesList,upstream);
            }
            if (null != occupyJson) {
                dealUpstreamTypeAndNodes(occupyJson,"occupy",now,domainInfo,upstreamTypes,nodes,nodeRulesList,upstream);
            }
            //List<UpstreamType> upstreamTypes = getUpstreamTypes(upstream, upstreamTypesJson, domainInfo);

            //UpstreamDto upstreamDto = new UpstreamDto(upstream);
            //权威源类型
            //upstreamDto.setUpstreamTypes(upstreamTypes);
            upstreamService.saveUpstreamAndTypesAndRoleBing(upstream, upstreamTypes, nodes,nodeRulesList,domainInfo);

            result.put("code", 200);
            result.put("status","success");
            result.put("message", "添加成功");

        } catch (CustomException e) {
            e.printStackTrace();
            result.put("code", 500);
            result.put("status","failed");
            result.put("message", e.getErrorMsg());
            logger.error(e + "");
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            result.put("code", 500);
            result.put("status","failed");
            result.put("message", e.getMessage());
            logger.error(e.getMessage());
            return result;
        }
        return result;
    }

    private List<UpstreamType> getUpstreamTypes(Upstream upstream, JSONObject upstreamTypesJson, DomainInfo domainInfo) {




        //
        ////岗位
        //if(null != postJson){
        //
        //}
        //UpstreamType postUpstreamType = new UpstreamType();
        //postUpstreamType.setSynWay(0);
        //postUpstreamType.setActive(true);
        //postUpstreamType.setServiceCode(clientId);
        //postUpstreamType.setSynType("post");
        //postUpstreamType.setDescription(upstream.getAppName() + "岗位推送权限");
        //upstreamTypes.add(postUpstreamType);
        //
        ////人员
        //UpstreamType personUpstreamType = new UpstreamType();
        //personUpstreamType.setSynWay(0);
        //personUpstreamType.setActive(true);
        //personUpstreamType.setServiceCode(clientId);
        //personUpstreamType.setSynType("person");
        //personUpstreamType.setDescription(upstream.getAppName() + "人员推送权限");
        //upstreamTypes.add(personUpstreamType);
        //
        ////人员身份
        //UpstreamType occupyUpstreamType = new UpstreamType();
        //occupyUpstreamType.setSynWay(0);
        //occupyUpstreamType.setActive(true);
        //occupyUpstreamType.setServiceCode(clientId);
        //occupyUpstreamType.setSynType("occupy");
        //occupyUpstreamType.setDescription(upstream.getAppName() + "人员身份推送权限");
        //upstreamTypes.add(occupyUpstreamType);
        //return upstreamTypes;
        return null;
    }

    private Upstream getUpstream(JSONObject upstreamJson, DomainInfo domainInfo) {
        String appCode = upstreamJson.getString("code");
        String appName = upstreamJson.getString("name");
        String dataCode = upstreamJson.getString("dataCode");
        String color = upstreamJson.getString("color");
        //校验必填参数
        if (StringUtils.isBlank(appCode)) {
            throw new CustomException(ResultCode.FAILED, "INVALID_PARAMETER");
        }
        if (StringUtils.isBlank(appName)) {
            throw new CustomException(ResultCode.FAILED, "INVALID_PARAMETER");
        }
        Upstream upstream = new Upstream();
        String upstreamId = UUID.randomUUID().toString();
        upstream.setId(upstreamId);
        upstream.setAppCode(appCode);
        upstream.setAppName(appName);
        upstream.setColor(color);
        upstream.setDataCode(dataCode);
        upstream.setCreateUser("IGA");
        upstream.setActive(true);
        upstream.setDomain(domainInfo.getId());

        return upstream;
    }

    private void dealUpstreamTypeAndNodes(JSONArray dataJson,String type,long now,DomainInfo domainInfo,List<UpstreamType> upstreamTypes,List<Node> nodes,List<NodeRules> nodeRulesList,Upstream upstream){
        for (int i = 0; i < dataJson.size(); i++) {
            JSONObject jsonObject = dataJson.getJSONObject(i);
            String description = jsonObject.getString("description");
            //校验必填参数
            if (StringUtils.isBlank(description)) {
                throw new CustomException(ResultCode.FAILED, "INVALID_PARAMETER");
            }
            String clientId = jsonObject.getString("clientId");
            //校验必填参数
            if (StringUtils.isBlank(clientId)) {
                throw new CustomException(ResultCode.FAILED, "INVALID_PARAMETER");
            }
            JSONObject nodeJson = jsonObject.getJSONObject("node");
            UpstreamType deptUpstreamType = new UpstreamType();
            String upstreamTypeId = UUID.randomUUID().toString();
            deptUpstreamType.setId(upstreamTypeId);
            deptUpstreamType.setUpstreamId(upstream.getId());
            deptUpstreamType.setSynWay(0);
            deptUpstreamType.setActive(true);
            deptUpstreamType.setServiceCode(clientId);
            deptUpstreamType.setSynType(type);
            deptUpstreamType.setDescription(description);
            deptUpstreamType.setIsPage(false);
            //校验名称重复
            List<UpstreamType> upstreamTypeList = upstreamTypeDao.findByUpstreamIdAndDescription(deptUpstreamType);
            if (null != upstreamTypeList && upstreamTypeList.size() > 0) {
                throw new CustomException(ResultCode.FAILED, "权威源类型描述重复");
            }
            if (null != nodeJson) {
                String treeType = nodeJson.getString("treeType");
                String code = nodeJson.getString("code");
                //校验必填参数
                if("dept".equals(type)){
                    if (StringUtils.isBlank(treeType)) {
                        throw new CustomException(ResultCode.FAILED, "INVALID_PARAMETER");
                    }
                }
                //处理node
                Node node = new Node();
                String nodeId = UUID.randomUUID().toString();
                node.setId(nodeId);
                node.setCreateTime(now);
                node.setDomain(domainInfo.getId());
                //node.setManual(node.getManual());
                node.setNodeCode(StringUtils.isBlank(code) ? " " : code);
                node.setStatus(0);
                node.setType(deptUpstreamType.getSynType());
                node.setDeptTreeType(treeType);
                nodes.add(node);
                NodeRules nodeRules = new NodeRules();
                nodeRules.setId(UUID.randomUUID().toString());
                nodeRules.setNodeId(nodeId);
                nodeRules.setType(0);
                nodeRules.setActive(true);
                nodeRules.setActiveTime(now);
                nodeRules.setServiceKey(upstreamTypeId);
                nodeRules.setStatus(0);
                nodeRulesList.add(nodeRules);
            }else {
                //人员身份和人员
                //处理node
                Node node = new Node();
                String nodeId = UUID.randomUUID().toString();
                node.setId(nodeId);
                node.setCreateTime(now);
                node.setDomain(domainInfo.getId());
                //node.setManual(node.getManual());
                node.setNodeCode(" ");
                node.setStatus(0);
                node.setType(deptUpstreamType.getSynType());
                nodes.add(node);
                NodeRules nodeRules = new NodeRules();
                nodeRules.setId(UUID.randomUUID().toString());
                nodeRules.setNodeId(nodeId);
                nodeRules.setType(0);
                nodeRules.setActive(true);
                nodeRules.setActiveTime(now);
                nodeRules.setServiceKey(upstreamTypeId);
                nodeRules.setStatus(0);
                nodeRulesList.add(nodeRules);
            }
            upstreamTypes.add(deptUpstreamType);
        }
    }

    private void check(JSONObject resource) {

        JSONObject metadata = resource.getJSONObject("metadata");
        JSONObject spec = resource.getJSONObject("spec");
        JSONObject upstreamJson = spec.getJSONObject("upstream");

        if (metadata == null || spec == null || null == upstreamJson) {
            throw new CustomException(ResultCode.INVALID_PARAMETER, "INVALID_PARAMETER");
        }

        String tenant = upstreamJson.getString("tenant");
        String appCode = upstreamJson.getString("code");
        String appName = upstreamJson.getString("name");
        String dataCode = upstreamJson.getString("dataCode");
        String color = upstreamJson.getString("color");
        JSONObject upstreamTypesJson = upstreamJson.getJSONObject("upstreamTypes");
        JSONArray deptJson = upstreamTypesJson.getJSONArray("dept");
        JSONArray postJson = upstreamTypesJson.getJSONArray("post");
        JSONArray personJson = upstreamTypesJson.getJSONArray("person");
        JSONArray occupyJson = upstreamTypesJson.getJSONArray("occupy");


        if (StringUtils.isBlank(tenant)) {
            throw new CustomException(ResultCode.INVALID_PARAMETER, "INVALID_PARAMETER");
        } else {
            DomainInfo domainInfo = domainInfoService.getByDomainName(tenant);
            if (domainInfo == null) {
                throw new CustomException(ResultCode.INVALID_PARAMETER, "INVALID_DOMAIN");
            }
        }
        if (StringUtils.isBlank(appCode)) {
            throw new CustomException(ResultCode.FAILED, "INVALID_PARAMETER");
        }
        if (StringUtils.isBlank(appName)) {
            throw new CustomException(ResultCode.FAILED, "INVALID_PARAMETER");
        }
        //if (StringUtils.isBlank(appClientId)) {
        //    throw new CustomException(ResultCode.FAILED, "INVALID_PARAMETER");
        //}
        //if (StringUtils.isBlank(deptTreeType)) {
        //    throw new CustomException(ResultCode.FAILED, "INVALID_PARAMETER");
        //}
    }

    @RequestMapping("/invokeTask")
    @ResponseBody
    public JSONObject invokeTask(){
        JSONObject jsonObject = new JSONObject();
        try {
            DomainInfo domainInfo = CertifiedConnector.getDomain();
            List<DsConfig> dsConfigs = dsConfigService.findAll();
            Map<String, DsConfig> collect = dsConfigs.stream().collect(Collectors.toMap(DsConfig::getTenantId, v -> v));
            // 如果 获取最近一次同步任务状况
            TaskLog lastTaskLog = taskLogService.last(domainInfo.getId());
            //  最近一次同步任务 状态成功后才能继续同步
            if ((null == lastTaskLog) || (null != lastTaskLog.getId() && !lastTaskLog.getStatus().equals("failed"))) {
                TaskConfig.errorData.remove(domainInfo.getId());
                Tenant tenant = tenantDao.findByDomainName(domainInfo.getDomainName());
                //判断sso是否有定时任务
                if (collect.containsKey(tenant.getId())) {
                    DsConfig dsConfig = collect.get(tenant.getId());
                    String syncWay = dsConfigService.getSyncWay(dsConfig);
                    if (StringUtils.isNotBlank(syncWay) && (syncWay.equals(TaskConfig.SYNC_WAY_BRIDGE) || syncWay.equals(TaskConfig.SYNC_WAY_ENTERPRISE) || syncWay.equals(TaskConfig.SYNC_WAY_ENTERPRISE_GRAPHQL))) {
                        logger.info("{}sso正在进行同步,跳过本次同步任务", domainInfo.getId());
                        TaskLog taskLog = new TaskLog();
                        taskLog.setId(UUID.randomUUID().toString());
                        taskLog.setReason("sso开启了同步配置，请先关闭！");
                        taskLogService.save(taskLog, domainInfo.getId(), "skip-error");
                        jsonObject.put("code","FAILED");
                        jsonObject.put("message","sso正在进行同步,跳过本次同步任务");
                        return jsonObject;
                    }
                }
                // 如果有编辑中的规则，则不进行数据同步
                Map<String, Object> arguments = new HashMap<>();
                arguments.put("status", 1);
                final List<NodeRules> nodeRules = nodeRulesService.findNodeRules(arguments, domainInfo.getId());
                TaskLog taskLog = new TaskLog();
                taskLog.setId(UUID.randomUUID().toString());
                // 如果有编辑中的规则，则不进行数据同步 &&
                if ((null == nodeRules || nodeRules.size() == 0)) {
                    try {
                        logger.info("{}开始同步,task:{}", domainInfo.getDomainName(), taskLog.getId());
                        taskLogService.save(taskLog, domainInfo.getId(), "save");
                        //部门数据同步至sso
                        Map<TreeBean, String> deptResult = deptService.buildDeptUpdateResult(domainInfo, lastTaskLog);
                        Map<String, List<Map.Entry<TreeBean, String>>> deptResultMap = deptResult.entrySet().stream().collect(Collectors.groupingBy(Map.Entry::getValue));
                        //处理数据
                        Integer recoverDept = deptResultMap.containsKey("recover") ? deptResultMap.get("recover").size() : 0;
                        Integer insertDept = (deptResultMap.containsKey("insert") ? deptResultMap.get("insert").size() : 0) + recoverDept;
                        Integer deleteDept = deptResultMap.containsKey("delete") ? deptResultMap.get("delete").size() : 0;
                        Integer updateDept = (deptResultMap.containsKey("update") ? deptResultMap.get("update").size() : 0);
                        Integer invalidDept = deptResultMap.containsKey("invalid") ? deptResultMap.get("invalid").size() : 0;
                        String deptNo = insertDept + "/" + deleteDept + "/" + updateDept + "/" + invalidDept;

                        logger.info(Thread.currentThread().getName() + ": 部门同步完成：{}==={}", deptNo, System.currentTimeMillis());
                        taskLog.setStatus("doing");
                        taskLog.setDeptNo(deptNo);
                        taskLogService.save(taskLog, domainInfo.getId(), "update");
                        // PUT   MQ
                        String pubResult = "";
                        if (deptResult.size() > 0) {
                            pubResult = dataBusUtil.pub(deptResult, null, null, "dept", domainInfo);
                            logger.info("dept pub:{}", pubResult);
                        }


                        //=============岗位数据同步至sso=================
                        final Map<TreeBean, String> postResult = postService.buildPostUpdateResult(domainInfo, lastTaskLog);
                        Map<String, List<Map.Entry<TreeBean, String>>> postResultMap = postResult.entrySet().stream().collect(Collectors.groupingBy(Map.Entry::getValue));
                        Integer recoverPost = postResultMap.containsKey("recover") ? postResultMap.get("recover").size() : 0;
                        Integer insertPost = (postResultMap.containsKey("insert") ? postResultMap.get("insert").size() : 0) + recoverPost;
                        Integer deletePost = postResultMap.containsKey("delete") ? postResultMap.get("delete").size() : 0;
                        Integer updatePost = (postResultMap.containsKey("update") ? postResultMap.get("update").size() : 0);
                        Integer invalidPost = postResultMap.containsKey("invalid") ? postResultMap.get("invalid").size() : 0;
                        String postNo = insertPost + "/" + deletePost + "/" + updatePost + "/" + invalidPost;
                        logger.info(Thread.currentThread().getName() + ": 岗位同步完成：{}==={}", postNo, System.currentTimeMillis());
                        taskLog.setPostNo(postNo);
                        taskLogService.save(taskLog, domainInfo.getId(), "update");

                        // PUT   MQ
                        if (postResult.size() > 0) {
                            pubResult = dataBusUtil.pub(postResult, null, null, "post", domainInfo);
                            logger.info("post pub:{}", pubResult);
                        }


                        //=============人员数据同步至sso=============
                        Map<String, List<Person>> personResult = personService.buildPerson(domainInfo, lastTaskLog);
                        Integer insertPerson = (personResult.containsKey("insert") ? personResult.get("insert").size() : 0);
                        Integer deletePerson = personResult.containsKey("delete") ? personResult.get("delete").size() : 0;
                        Integer updatePerson = (personResult.containsKey("update") ? personResult.get("update").size() : 0);
                        Integer invalidPerson = personResult.containsKey("invalid") ? personResult.get("invalid").size() : 0;
                        String personNo = insertPerson + "/" + deletePerson + "/" + updatePerson + "/" + invalidPerson;
                        logger.info(Thread.currentThread().getName() + ": 人员同步完成{}==={}", personNo, System.currentTimeMillis());
                        taskLog.setPersonNo(personNo);
                        //    taskLog.setData(errorData.get(domainInfo.getId()));
                        taskLogService.save(taskLog, domainInfo.getId(), "update");

                        // PUT   MQ
                        if (personResult.size() > 0 && (insertPerson + deletePerson + updatePerson + invalidPerson) < 100) {
                            pubResult = dataBusUtil.pub(null, personResult, null, "person", domainInfo);
                            logger.info("person pub:{}", pubResult);
                        }


                        //人员身份同步至sso
                        final Map<String, List<OccupyDto>> occupyResult = occupyService.buildOccupy(domainInfo, lastTaskLog);
                        //Integer recoverOccupy = occupyResult.containsKey("recover") ? deptResultMap.get("recover").size() : 0;
                        Integer insertOccupy = (occupyResult.containsKey("insert") ? occupyResult.get("insert").size() : 0);
                        Integer deleteOccupy = occupyResult.containsKey("delete") ? occupyResult.get("delete").size() : 0;
                        Integer updateOccupy = (occupyResult.containsKey("update") ? occupyResult.get("update").size() : 0);
                        Integer invalidOccupy = occupyResult.containsKey("invalid") ? occupyResult.get("invalid").size() : 0;
                        String occupyNo = insertOccupy + "/" + deleteOccupy + "/" + updateOccupy + "/" + invalidOccupy;
                        logger.info(Thread.currentThread().getName() + ": 人员身份同步完成{}==={}", occupyNo, System.currentTimeMillis());
                        taskLog.setStatus("done");
                        taskLog.setOccupyNo(occupyNo);
                        taskLogService.save(taskLog, domainInfo.getId(), "update");

                        // PUT   MQ
                        if (occupyResult.size() > 0 && (insertOccupy + deleteOccupy + updateOccupy + invalidOccupy) < 500) {
                            pubResult = dataBusUtil.pub(null, null, occupyResult, "occupy", domainInfo);
                            logger.info("occupy pub:{}", pubResult);
                        }
                        //数据上传
                        if (StringUtils.isNotBlank(TaskConfig.errorData.get(domainInfo.getId()))) {
                            taskConfig.upload(domainInfo, taskLog);
                        } else {
                            logger.info("{}本次同步无异常数据", domainInfo.getDomainName());
                        }
                        logger.info("{}同步结束,task:{}", domainInfo.getDomainName(), taskLog.getId());
                    } catch (CustomException e) {
                        logger.error("定时同步异常：" + e);
                        taskLog.setStatus("failed");
                        taskLog.setReason(e.getErrorMsg());
                        if (StringUtils.isNotBlank(TaskConfig.errorData.get(domainInfo.getId()))) {
                            taskConfig.upload(domainInfo, taskLog);
                        }else{
                            taskLogService.save(taskLog,domainInfo.getDomainId(),"update");
                        }
                        e.printStackTrace();
                        jsonObject.put("code","FAILED");
                        jsonObject.put("message",e.getErrorMsg());
                        return jsonObject;
                    } catch (Exception e) {
                        logger.error("定时同步异常：" + e);
                        taskLog.setStatus("failed");
                        taskLog.setReason(e.getMessage());
                        if (StringUtils.isNotBlank(TaskConfig.errorData.get(domainInfo.getId()))) {
                            taskConfig.upload(domainInfo, taskLog);
                        }else{
                            taskLogService.save(taskLog,domainInfo.getDomainId(),"update");
                        }
                        e.printStackTrace();
                        jsonObject.put("code","FAILED");
                        jsonObject.put("message",e.getMessage());
                        return jsonObject;
                    }
                } else {
                    taskLog.setReason("有编辑中规则，跳过数据同步");
                    taskLogService.save(taskLog, domainInfo.getId(), "skip");
                    logger.info("编辑中规则数:{}", nodeRules.size());
                    logger.info("有编辑中规则，跳过数据同步");
                    jsonObject.put("code","FAILED");
                    jsonObject.put("message","有编辑中规则，跳过数据同步");
                    return  jsonObject;
                }

            }else {
                jsonObject.put("code","FAILED");
                jsonObject.put("message","请检查最近一次同步的状态");
                return jsonObject;
            }
        } catch (Exception e) {
            e.printStackTrace();
            jsonObject.put("code","FAILED");
            jsonObject.put("message",e.getMessage());
            return jsonObject;
        }
        jsonObject.put("code","SUCCESS");
        jsonObject.put("message","同步成功");
        return jsonObject;
    }

}
