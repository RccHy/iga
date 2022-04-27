package com.qtgl.iga.controller;


import com.alibaba.fastjson.JSONObject;
import com.qtgl.iga.bean.UpstreamDto;
import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.bo.Upstream;
import com.qtgl.iga.bo.UpstreamType;
import com.qtgl.iga.service.DomainInfoService;
import com.qtgl.iga.service.UpstreamService;
import com.qtgl.iga.utils.DataBusUtil;
import com.qtgl.iga.utils.UrlUtil;
import com.qtgl.iga.utils.enumerate.ResultCode;
import com.qtgl.iga.utils.exception.CustomException;
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
import javax.servlet.http.HttpServletResponse;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author rcc
 */
@Controller
@RequestMapping("/api")
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
    public JSONObject addClient(HttpServletRequest request, HttpServletResponse httpServletResponse) {
        String resource = request.getParameter("resource");
        logger.info(resource);
        //resource = "{" +
        //        " \"apiVersion\": \"sss\" ," +
        //        " \"kind\": \"CustomResourceDefinition\"," +
        //        " \"metadata\": {" +
        //        " \"name\": \"qdatasource.ketanyun.cn\" " +
        //        " }," +
        //        "\"spec\": {" +
        //        " \"type\": \"object\"," +
        //        " \"required\": \"[upstream]\"," +
        //        " \"upstream\": {" +
        //        " \"appCode\": \"0426\"," +
        //        " \"appName\": \"LQZTEST\"," +
        //        " \"tenant\": \"devel.ketanyun.cn\"," +
        //        " \"appClientId\": \"gDGqfDGm8OrJAXCFDiuI\"," +
        //        " \"deptTreeType\": \"01\"," +
        //        " " +
        //        " }" +
        //        " }" +
        //        "}";

        JSONObject jsonObject = JSONObject.parseObject(resource);
        JSONObject result = new JSONObject();

        try {
            check(jsonObject);
            Upstream upstream = getUpstream(jsonObject);
            //添加权威源及权威源类型
            UpstreamDto upstreamDto = new UpstreamDto(upstream);
            //权威源类型
            String appClientId = jsonObject.getJSONObject("spec").getJSONObject("upstream").getString("appClientId");
            String domain = jsonObject.getJSONObject("spec").getJSONObject("upstream").getString("tenant");
            String deptTreeType = jsonObject.getJSONObject("spec").getJSONObject("upstream").getString("deptTreeType");
            List<UpstreamType> upstreamTypes = getUpstreamTypes(upstream, appClientId);
            upstreamDto.setUpstreamTypes(upstreamTypes);
            upstreamService.saveUpstreamAndTypesAndRoleBing(upstreamDto, upstream.getDomain(), deptTreeType);

            result.put("code", 200);
            result.put("message", "添加成功");

        } catch (CustomException e) {
            e.printStackTrace();
            result.put("code", 500);
            result.put("message", e.getErrorMsg());
            logger.error(e + "");
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            result.put("code", 500);
            result.put("message", e.getMessage());
            logger.error(e.getMessage());
            return result;
        }
        return result;
    }

    private List<UpstreamType> getUpstreamTypes(Upstream upstream, String clientId) {
        List<UpstreamType> upstreamTypes = new ArrayList<>();
        //组织机构
        UpstreamType deptUpstreamType = new UpstreamType();
        deptUpstreamType.setSynWay(0);
        deptUpstreamType.setActive(true);
        deptUpstreamType.setServiceCode(clientId);
        deptUpstreamType.setSynType("dept");
        deptUpstreamType.setDescription(upstream.getAppName() + "部门推送权限");
        upstreamTypes.add(deptUpstreamType);
        //岗位
        UpstreamType postUpstreamType = new UpstreamType();
        postUpstreamType.setSynWay(0);
        postUpstreamType.setActive(true);
        postUpstreamType.setServiceCode(clientId);
        postUpstreamType.setSynType("post");
        postUpstreamType.setDescription(upstream.getAppName() + "岗位推送权限");
        upstreamTypes.add(postUpstreamType);

        //人员
        UpstreamType personUpstreamType = new UpstreamType();
        personUpstreamType.setSynWay(0);
        personUpstreamType.setActive(true);
        personUpstreamType.setServiceCode(clientId);
        personUpstreamType.setSynType("person");
        personUpstreamType.setDescription(upstream.getAppName() + "人员推送权限");
        upstreamTypes.add(personUpstreamType);

        //人员身份
        UpstreamType occupyUpstreamType = new UpstreamType();
        occupyUpstreamType.setSynWay(0);
        occupyUpstreamType.setActive(true);
        occupyUpstreamType.setServiceCode(clientId);
        occupyUpstreamType.setSynType("occupy");
        occupyUpstreamType.setDescription(upstream.getAppName() + "人员身份推送权限");
        upstreamTypes.add(occupyUpstreamType);
        return upstreamTypes;
    }

    private Upstream getUpstream(JSONObject resource) {

        JSONObject spec = resource.getJSONObject("spec");
        JSONObject upstreamJson = spec.getJSONObject("upstream");

        String tenantName = upstreamJson.getString("tenant");
        String appCode = upstreamJson.getString("appCode");
        String appName = upstreamJson.getString("appName");
        String dataCode = upstreamJson.getString("dataCode");
        String color = upstreamJson.getString("color");
        DomainInfo domainInfo = domainInfoService.getByDomainName(tenantName);
        Upstream upstream = new Upstream();
        upstream.setAppCode(appCode);
        upstream.setAppName(appName);
        upstream.setColor(color);
        upstream.setDataCode(dataCode);
        upstream.setCreateUser("IGA");
        upstream.setActive(true);
        upstream.setDomain(domainInfo.getId());

        return upstream;
    }

    private void check(JSONObject resource) {

        JSONObject metadata = resource.getJSONObject("metadata");
        JSONObject spec = resource.getJSONObject("spec");
        JSONObject upstreamJson = spec.getJSONObject("upstream");

        if (metadata == null || spec == null || null == upstreamJson) {
            throw new CustomException(ResultCode.INVALID_PARAMETER, "INVALID_PARAMETER");
        }
        String tenant = upstreamJson.getString("tenant");
        String appCode = upstreamJson.getString("appCode");
        String appName = upstreamJson.getString("appName");
        String appClientId = upstreamJson.getString("appClientId");
        String deptTreeType = upstreamJson.getString("deptTreeType");

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
        if (StringUtils.isBlank(appClientId)) {
            throw new CustomException(ResultCode.FAILED, "INVALID_PARAMETER");
        }
        if (StringUtils.isBlank(deptTreeType)) {
            throw new CustomException(ResultCode.FAILED, "INVALID_PARAMETER");
        }
    }

}
