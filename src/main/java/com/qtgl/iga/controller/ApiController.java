package com.qtgl.iga.controller;


import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.qtgl.iga.bean.QUserSource.Field;
import com.qtgl.iga.bean.QUserSource.QUserSource;
import com.qtgl.iga.bean.QUserSource.Rule;
import com.qtgl.iga.bean.QUserSource.Sources;
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
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.RejectedExecutionException;
import java.util.stream.Collectors;

/**
 * @author rcc
 */
@Controller
@RequestMapping("/api")
@Slf4j
public class ApiController {

    public static Logger logger = LoggerFactory.getLogger(ApiController.class);

    public static Long bootstrapStartTime = null;
    @Autowired
    DomainInfoService domainInfoService;

    @Autowired
    DataBusUtil dataBusUtil;

    @Value("${bus.url}")
    private String busUrl;
    @Autowired
    UpstreamService upstreamService;

    @Autowired
    UpstreamTypeService upstreamTypeService;
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
    @Autowired
    NodeService nodeService;


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
    public void postBootstrap(HttpServletRequest request,
                              @RequestParam String resource,
                              @RequestParam(required = false) String context) {

        logger.info(resource);

        JSONObject jsonObject = JSONObject.parseObject(resource);

        try {
            QUserSource qUserSource = jsonObject.getJSONObject("spec").toJavaObject(QUserSource.class);

            // 验证json格式是否合法

            String tenant = qUserSource.getTenant().getName();
            DomainInfo domainInfo = null;
            //检查租户
            if (StringUtils.isBlank(tenant)) {
                logger.error("[bootstrap] 租户信息为空");
                return;
            } else {
                domainInfo = domainInfoService.getByDomainName(tenant);
                if (domainInfo == null) {
                    logger.error("[bootstrap] 租户:{} 匹配不到", tenant);
                    return;
                }
            }
            //处理权威源
            Upstream upstream = setUpstream(qUserSource, domainInfo);
            // 检查是否需要更新,如果AppCode存在 则需要更新
            Upstream byCodeAndDomain = upstreamService.findByCodeAndDomain(upstream.getAppCode(), domainInfo.getId());
            boolean update = false;
            if (null != byCodeAndDomain) {
                logger.info("[bootstrap] {}-{}权威源已存在", upstream.getAppCode(), domainInfo.getDomainName());
                upstream.setId(byCodeAndDomain.getId());
                update = true;
            }
            // 权威源处理完成,保存权威源
            if (update) {
                upstreamService.updateUpstream(upstream);
                log.info("[bootstrap] {}-{}更新权威源完成", upstream.getAppCode(), domainInfo.getDomainName());
                upstreamService. delAboutNode(upstream, domainInfo);
                log.info("[bootstrap] {}-{}删除权威源相关规则节点完成", upstream.getAppCode(), domainInfo.getDomainName());
                // qUserSource.getSources 所有name集合
                List<String> codes = qUserSource.getSources().stream().map(Sources::getName).collect(Collectors.toList());
                // 删除权威源类型
                upstreamTypeService.deleteUpstreamTypeByCods(codes, domainInfo.getId());

            } else {
                upstreamService.saveUpstream(upstream, domainInfo.getId());
                log.info("[bootstrap] {}-{}新增权威源完成", upstream.getAppCode(), domainInfo.getDomainName());
            }

            //处理权威源类型及node
            List<UpstreamType> upstreamTypes = new ArrayList<>();
            List<UpstreamType> updateUpstreamTypes = new ArrayList<>();
            List<UpstreamTypeField> fields = new ArrayList<>();


            List<Node> nodes = new ArrayList<>();
            List<NodeRules> nodeRulesList = new ArrayList<>();
            List<NodeRulesRange> nodeRulesRangeList = new ArrayList<>();

            long now = System.currentTimeMillis();
            for (Sources source : qUserSource.getSources()) {
                dealUpstreamTypeAndNodes(source, now, domainInfo, upstreamTypes, updateUpstreamTypes, fields, nodes, nodeRulesList, nodeRulesRangeList, upstream);
            }
            // 权威源类型处理完成,保存权威源类型
            Integer statusCode = upstreamService.saveUpstreamTypesAndFields(upstreamTypes, updateUpstreamTypes, fields, domainInfo);
            if (statusCode != -1) {
                log.info("[bootstrap] {}-{}保存权威源类型完成", upstream.getAppCode(), domainInfo.getDomainName());
                // 保存node 信息
                statusCode = upstreamService.saveUpstreamAboutNodes(nodes, nodeRulesList, nodeRulesRangeList, domainInfo);
                log.info("[bootstrap] {}-{}保存权威源相关规则节点完成", upstream.getAppCode(), domainInfo.getDomainName());
                // todo 推送才需要 rolebinding
                if (statusCode != -1&&false) {
                    upstreamService.saveRoleBing(upstreamTypes, nodes, nodeRulesList, domainInfo);
                }else{
                    log.error("[bootstrap] {}-{}保存权威源相关规则节点失败", upstream.getAppCode(), domainInfo.getDomainName());
                }
            }else {
                log.error("[bootstrap] {}-{}保存权威源类型失败", upstream.getAppCode(), domainInfo.getDomainName());
            }


        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage());
        }
    }

    @PutMapping(value = "/bootstrap")
    @ResponseBody
    public void putBootstrap(HttpServletRequest request,
                             @RequestParam String resource,
                             @RequestParam(required = false) String context) {
        postBootstrap(request, resource, context);
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

    private Upstream setUpstream(QUserSource qUserSource, DomainInfo domainInfo) {
        // 机读
        String appCode = qUserSource.getName();
        // 人读
        String appName = qUserSource.getText();
        String color = qUserSource.getColor();
        //校验必填参数
        if (StringUtils.isBlank(appCode)) {
            logger.error("[bootstrap] source name is null");
            throw new CustomException(ResultCode.FAILED, "[bootstrap] source name is null");
        }
        if (StringUtils.isBlank(appName)) {
            logger.error("[bootstrap] source text is null");
            throw new CustomException(ResultCode.FAILED, "[bootstrap] source text is null");
        }
        Upstream upstream = new Upstream();
        upstream.setAppCode(appCode);
        upstream.setAppName(appName);
        upstream.setColor(color);
        upstream.setCreateUser("installer");
        upstream.setActive(true);
        upstream.setDomain(domainInfo.getId());

        return upstream;
    }

    private void dealUpstreamTypeAndNodes(Sources source, long now, DomainInfo domainInfo, List<UpstreamType> upstreamTypes, List<UpstreamType> updateUpstreamTypes,
                                          List<UpstreamTypeField> fields, List<Node> nodes, List<NodeRules> nodeRulesList, List<NodeRulesRange> nodeRulesRangeList, Upstream upstream) {
        //todo 参数检查

        log.info("[bootstrap] {}-{}-{}开始处理权威源类型及node",  domainInfo.getDomainName(),upstream.getAppCode(), source.getText());
        UpstreamType upstreamType = new UpstreamType();
        upstreamType.setId(UUID.randomUUID().toString());
        upstreamType.setCode(source.getName());
        upstreamType.setUpstreamId(upstream.getId());
        upstreamType.setSynType(source.getKind().equals("user")?"person":source.getKind());
        upstreamType.setActive(true);
        // 如果定义了 app 信息则为推送模式 , 定义了service 则为拉取模式. 1拉取 0推送 2自定义json
        switch (source.getMode()) {
            case "push":
                upstreamType.setSynWay(0);
                break;
            case "pull":
                upstreamType.setSynWay(1);
                break;
            case "builtin":
                upstreamType.setSynWay(2);
                break;
            default:
                break;
        }
        if ("push".equals(source.getMode())) {
            upstreamType.setServiceCode(source.getApp().getName());
        }
        if ("builtin".equals(source.getMode())) {
            upstreamType.setBuiltinData(source.getData().getValue());
        }
        String mode="拉取";
        if("push".equals(source.getMode())){
            mode="推送";
        }
        upstreamType.setDescription(StringUtils.isNotBlank(source.getText())?source.getText(): source.getKind()+mode);
        upstreamType.setIsPage(true);
        if (null != source.getService() && null != source.getService().getOperation()) {
            String serviceName = source.getService().getName();
            String operationName = source.getService().getOperation().getName();
            // waring 缺少 graphqlUrl中所需的 "类型" 信息, 但是不影响解析.有方法信息即可执行
            String graphqlUrl = "bus://%s/%s/query/%s";
            upstreamType.setGraphqlUrl(String.format(graphqlUrl, serviceName, operationName, operationName));
            if (null != source.getService().getOperation().getAttributes() &&
                    source.getService().getOperation().getAttributes().containsKey("pagination")) {
                Boolean pagination = (Boolean) source.getService().getOperation().getAttributes().get("pagination");
                upstreamType.setIsPage(pagination);
            }
            // todo  录取数据可定义 过滤条件
        }

        //  增量模式开关
        if (null != source.getStrategies()) {
            upstreamType.setIsIncremental(source.getStrategies().getIncremental());
        }
        // 如果kind 为person or occupy 时, 需要定义合重依据
        if ("user".equals(source.getKind()) || "occupy".equals(source.getKind())) {
            upstreamType.setPersonCharacteristic(null != source.getPrincipal().getName() ? source.getPrincipal().getName() : "CARD_NO");
        }
        // 判断upstreamType 是否需要更新,
        UpstreamType byUpstreamAndTypeAndDesc = upstreamTypeService.findByCode(source.getName());
        if (null != byUpstreamAndTypeAndDesc) {
            upstreamType.setId(byUpstreamAndTypeAndDesc.getId());
            updateUpstreamTypes.add(upstreamType);
        } else {
            upstreamTypes.add(upstreamType);
        }

        // 构造字段
        if (null != source.getFields() && source.getFields().size() > 0) {
            // 去重,防止重复字段
            Set<Field> fieldSet = new TreeSet<>(Comparator.comparing(Field::getName));
            fieldSet.addAll(source.getFields());
            new ArrayList<>(fieldSet).forEach(field -> {
                        UpstreamTypeField upstreamTypeField = new UpstreamTypeField();
                        upstreamTypeField.setId(UUID.randomUUID().toString());
                        upstreamTypeField.setUpstreamTypeId(upstreamType.getId());
                        upstreamTypeField.setSourceField(field.getName());
                        upstreamTypeField.setTargetField(field.getExpression().getValue());
                        upstreamTypeField.setCreateTime(new Timestamp(new java.util.Date().getTime()));
                        upstreamTypeField.setUpdateTime(new Timestamp(new java.util.Date().getTime()));
                        upstreamTypeField.setDomain(domainInfo.getId());
                        fields.add(upstreamTypeField);
                    }
            );
        }

        // 构造规则信息

        //构造 node
        Node node = new Node();
        node.setId(UUID.randomUUID().toString());
        node.setCreateTime(now);
        node.setDomain(domainInfo.getId());
        node.setNodeCode(" ");
        node.setStatus(0);
        node.setType(upstreamType.getSynType());

        String nodeRulesId = UUID.randomUUID().toString();
        //构造 nodeRule
        NodeRules nodeRules = new NodeRules();
        nodeRules.setId(nodeRulesId);
        nodeRules.setNodeId(node.getId());
        nodeRules.setType(0);
        nodeRules.setActive(false);
        if (null != source.getRule()) {
            nodeRules.setActive(source.getRule().getEnabled());
        }
        nodeRules.setActiveTime(now);
        // 如果是推送服务,则需要定义serviceKey, 拉取服务则定义upstreamType
        if (source.getMode().equals("push")) {
            nodeRules.setServiceKey(upstreamType.getId());
        } else {
            nodeRules.setUpstreamTypesId(upstreamType.getId());
            nodeRules.setType(1);
        }
        nodeRules.setStatus(0);
        nodeRulesList.add(nodeRules);

        if (source.getKind().equals("dept") || source.getKind().equals("post")) {
            //  source 下 rule 可为空, 则默认挂载到 [dept]单位类型/根节点 or [post]身份岗/根节点
            //  monut 可能为空, 为空则默认分配
            // todo 定义类型,默认01
            node.setDeptTreeType("01");
            node.setNodeCode(" ");

            // 构建rulesRange
            NodeRulesRange nodeRulesRange = new NodeRulesRange();
            nodeRulesRange.setId(UUID.randomUUID().toString());
            nodeRulesRange.setNodeRulesId(nodeRulesId);
            //  规则类型 0 挂载 1 排除
            nodeRulesRange.setType(0);
            nodeRulesRange.setRange(0);
            nodeRulesRange.setCreateTime(now);
            nodeRulesRange.setStatus(0);
            // source 下 rule 不为空时, 覆盖默认值
            if (null != source.getRule()) {
                Rule rule = source.getRule();
                if (null != rule.getMount()) {
                    String treeType = rule.getMount().getCategory();
                    node.setDeptTreeType(treeType);
                    // 挂载路径, 为空则是根节点
                    String code = source.getRule().getMount().getPath();
                    node.setNodeCode(null != code ? code : " ");
                    nodeRulesRange.setNode("=*");
                }
                nodeRulesRange.setType(source.getRule().getKind().equals("exclude") ? 1 : 0);

            }
            nodeRulesRangeList.add(nodeRulesRange);
        }
        nodes.add(node);

        log.info("[bootstrap] {}-{}-{}开始处理权威源类型及node",  domainInfo.getDomainName(),upstream.getAppCode(), source.getText());

    }


    @RequestMapping("/invokeTask")
    @ResponseBody
    public JSONObject invokeTask() {
        JSONObject jsonObject = new JSONObject();

        try {
            DomainInfo domainInfo = CertifiedConnector.getDomain();

            //TaskLog lastTaskLog = taskLogService.last(domainInfo.getId());
            Boolean flag = taskLogService.checkTaskStatus(domainInfo.getId());
            //if((null != lastTaskLog && lastTaskLog.getStatus().equals("failed"))){
            //    jsonObject.put("code","FAILED");
            //    jsonObject.put("message","最近一次同步任务状态异常,请处理后再进行同步");
            //    return jsonObject;
            //}
            if (!flag) {
                jsonObject.put("code", "FAILED");
                jsonObject.put("message", "最近三次同步状态均为失败,请处理后再进行同步");
                return jsonObject;
            }
            taskConfig.executeTask(domainInfo);

        } catch (RejectedExecutionException e) {
            e.printStackTrace();
            jsonObject.put("code", "FAILED");
            jsonObject.put("message", "当前线程正在进行数据同步,请稍后再试");
            return jsonObject;
        } catch (Exception e) {
            e.printStackTrace();
            jsonObject.put("code", "FAILED");
            jsonObject.put("message", e.getMessage());
            return jsonObject;
        }
        jsonObject.put("code", "SUCCESS");
        jsonObject.put("message", "触发成功");
        return jsonObject;
    }


    @RequestMapping("/dealWithPerson")
    @ResponseBody
    public JSONObject dealWithPerson() {
        JSONObject jsonObject = new JSONObject();

        try {
            DomainInfo domainInfo = CertifiedConnector.getDomain();
            jsonObject = personService.dealWithPerson(domainInfo);
        } catch (CustomException e) {
            e.printStackTrace();
            jsonObject.put("code", "FAILED");
            jsonObject.put("message", e.getErrorMsg());
        } catch (Exception e) {
            e.printStackTrace();
            jsonObject.put("code", "FAILED");
            jsonObject.put("message", e.getMessage());
        }


        return jsonObject;
    }

}
