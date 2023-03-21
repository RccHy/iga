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
    @Autowired
    DomainIgnoreService domainIgnoreService;


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
