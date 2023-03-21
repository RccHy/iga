package com.qtgl.iga.controller;


import com.alibaba.fastjson.JSONObject;
import com.qtgl.iga.bean.QUserSource.Field;
import com.qtgl.iga.bean.QUserSource.QUserSource;
import com.qtgl.iga.bean.QUserSource.Rule;
import com.qtgl.iga.bean.QUserSource.Sources;
import com.qtgl.iga.bo.*;
import com.qtgl.iga.service.*;
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
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author 1
 */
@Controller
@Slf4j
public class StatusController {


    public static Logger logger = LoggerFactory.getLogger(StatusController.class);


    @Value("${iga.version}")
    private String version;

    @GetMapping("/livez")
    @ResponseBody
    public String livez() {
        return "200";
    }

    @GetMapping("/readyz")
    @ResponseBody
    public String readyz() {

        return "200";
    }

    @GetMapping("/status")
    @ResponseBody
    public Map status() {
        Map map = new HashMap();
        map.putAll(getSystemRuntime());
        return map;
    }

    @GetMapping("/version")
    @ResponseBody
    public String version() {

        return version;
    }

    public static String getClassVersion(String pattern) {
        Date created = new Date();
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            String path = cl.getResource("application.yml").getPath();
            if ("\\".equalsIgnoreCase(File.separator)) {
                path = path.replaceAll("/", "\\\\");
                if (path.substring(0, 1).equals("\\")) {
                    path = path.substring(1);
                }
            } else if ("/".equalsIgnoreCase(File.separator)) {
                path = path.replaceAll("\\\\", "/");
            }
            if (null != path) {
                long time = Files.readAttributes(Paths.get(path), BasicFileAttributes.class).creationTime().toMillis();
                created = new Date(time);
            }
        } catch (Exception e) {
            logger.error("Failed to obtain class attributes.", e);
        }
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        return sdf.format(created);
    }


    private Map getSystemRuntime() {
        try {
            Map rt = new HashMap();
            Runtime runtime = Runtime.getRuntime();
            rt.put("vm_total", runtime.totalMemory() / (1024 * 1024));
            rt.put("vm_free", runtime.freeMemory() / (1024 * 1024));
            rt.put("vm_max", runtime.maxMemory() / (1024 * 1024));
            ThreadGroup parentThread;
            int totalThread = 0;
            for (parentThread = Thread.currentThread().getThreadGroup(); parentThread
                    .getParent() != null; parentThread = parentThread.getParent()) {
                totalThread = parentThread.activeCount();
            }
            rt.put("vm_total_thread", totalThread);
            return rt;
        } catch (Exception e) {
            return new HashMap();
        }
    }




    @Autowired
    NodeService nodeService;
    @Autowired
    DomainIgnoreService domainIgnoreService;
    @Autowired
    DomainInfoService domainInfoService;
    @Autowired
    UpstreamService upstreamService;
    @Autowired
    UpstreamTypeService upstreamTypeService;

    @PostMapping(value = "/bootstrap")
    @ResponseBody
    public JSONObject postBootstrap(HttpServletRequest request,
                              @RequestParam String resource,
                              @RequestParam(required = false) String context) {

        logger.info(resource);
        JSONObject result = new JSONObject();

        JSONObject jsonObject = JSONObject.parseObject(resource);

        try {
            QUserSource qUserSource = jsonObject.getJSONObject("spec").toJavaObject(QUserSource.class);

            // 验证json格式是否合法

            String tenant = qUserSource.getTenant().getName();
            DomainInfo domainInfo = null;
            //检查租户
            if (StringUtils.isBlank(tenant)) {
                logger.error("[bootstrap] 租户信息为空");
                result.put("code", ResultCode.FAILED.getCode());
                result.put("message", "租户信息为空");
                return result;
            } else {
                domainInfo = domainInfoService.getByDomainName(tenant);
                if (domainInfo == null) {
                    logger.error("[bootstrap] 租户:{} 匹配不到", tenant);
                    result.put("code", ResultCode.FAILED.getCode());
                    result.put("message", "租户"+tenant+"匹配不到");
                    return result;
                }
            }
            //处理权威源
            Upstream upstream = setUpstream(qUserSource, domainInfo);
            // 检查是否需要更新,如果AppCode存在 则需要更新
            Upstream byCodeAndDomain = upstreamService.findByCodeAndDomain(upstream.getAppCode(), domainInfo.getId());
            boolean update = false;
            if (null != byCodeAndDomain) {
                logger.info("[bootstrap] {}-{}权威源已存在，进行更新", upstream.getAppCode(), domainInfo.getDomainName());
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
//                if (statusCode != -1) {
//                    upstreamService.saveRoleBing(upstreamTypes, nodes, nodeRulesList, domainInfo);
//                }else{
//                    log.error("[bootstrap] {}-{}保存权威源相关规则节点失败", upstream.getAppCode(), domainInfo.getDomainName());
//                }
                // 兼容旧版本升级情况下，会先有本地租户信息，再创建超级租户信息。 所以进行验证，如果本次创建的upstream是超级租户并且本地租住存在同appCode的upstream，则记录忽略信息

                result.put("code", ResultCode.SUCCESS.getCode());
                result.put("message", "保存成功");
                return result;
            } else {
                log.error("[bootstrap] {}-{}保存权威源类型失败", upstream.getAppCode(), domainInfo.getDomainName());
                result.put("code", ResultCode.FAILED.getCode());
                result.put("message", "保存权威源类型失败");
                return result;
            }


        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage());
            result.put("code", ResultCode.FAILED.getCode());
            result.put("message", e.getMessage());
            return result;
        }
    }

    @PutMapping(value = "/bootstrap")
    @ResponseBody
    public JSONObject putBootstrap(HttpServletRequest request,
                             @RequestParam String resource,
                             @RequestParam(required = false) String context) {
        return postBootstrap(request, resource, context);
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

        log.info("[bootstrap] {}-{}-{}开始处理权威源类型及node", domainInfo.getDomainName(), upstream.getAppCode(), source.getText());
        UpstreamType upstreamType = new UpstreamType();
        upstreamType.setId(UUID.randomUUID().toString());
        upstreamType.setCode(source.getName());
        upstreamType.setUpstreamId(upstream.getId());
        upstreamType.setSynType(source.getKind().equals("user") ? "person" : source.getKind());
        upstreamType.setActive(true);
        // 如果定义了 app 信息则为推送模式 , 定义了service 则为拉取模式. 0拉取 1推送 2自定义json
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
        String mode = "拉取";
        if ("push".equals(source.getMode())) {
            mode = "推送";
        }
        upstreamType.setDescription(StringUtils.isNotBlank(source.getText()) ? source.getText() : source.getKind() + mode);
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
        node.setNodeCode("");
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
            if ("post".equals(source.getKind())) {
                node.setDeptTreeType(null);
            }
            node.setNodeCode("");

            // 构建rulesRange
            NodeRulesRange nodeRulesRange = new NodeRulesRange();
            nodeRulesRange.setId(UUID.randomUUID().toString());
            nodeRulesRange.setNodeRulesId(nodeRulesId);
            //  规则类型 0 挂载 1 排除
            nodeRulesRange.setType(0);
            nodeRulesRange.setRange(0);
            nodeRulesRange.setCreateTime(now);
            nodeRulesRange.setStatus(0);
            nodeRulesRange.setNode("=*");
            // source 下 rule 不为空时, 覆盖默认值
            if (null != source.getRule()) {
                Rule rule = source.getRule();
                if (null != rule.getMount()) {
                    if ("dept".equals(source.getKind())) {
                        String treeType = rule.getMount().getCategory();
                        node.setDeptTreeType(treeType);
                    }
                    // 挂载路径, 为空则是根节点
                    String code = source.getRule().getMount().getPath();
                    node.setNodeCode(null != code ? code : "");
                }
                nodeRulesRange.setType(source.getRule().getKind().equals("exclude") ? 1 : 0);

            }
            nodeRulesRangeList.add(nodeRulesRange);
        }
        nodes.add(node);

        log.info("[bootstrap] {}-{}-{}开始处理权威源类型及node", domainInfo.getDomainName(), upstream.getAppCode(), source.getText());

    }




}
