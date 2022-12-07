package com.qtgl.iga.utils;

import com.qtgl.iga.utils.enumerate.ResultCode;
import com.qtgl.iga.utils.exception.CustomException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.mountcloud.graphql.GraphqlClient;
import org.mountcloud.graphql.request.mutation.DefaultGraphqlMutation;
import org.mountcloud.graphql.request.mutation.GraphqlMutation;
import org.mountcloud.graphql.response.GraphqlResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class RoleBingUtil {

    @Value("${bus.url}")
    private String busUrl;
    @Resource
    DataBusUtil dataBusUtil;

    public String addRoleBinding(String clientId, String serviceName, String type, Map<String, String> igaRules, ArrayList<String> permissions) {
        if (StringUtils.isEmpty(busUrl)) {
            throw new CustomException(ResultCode.FAILED, "BusUrl IS NULL");
        }
        GraphqlClient graphqlClient = GraphqlClient.buildGraphqlClient(busUrl);
        Map<String, String> map = new HashMap<>();
        map.put("Authorization", "Bearer " + dataBusUtil.getToken(serviceName));
        //map.put("Authorization", "Bearer 3426299750c494733395edd6b4b889d3");
        graphqlClient.setHttpHeaders(map);

        GraphqlMutation graphqlMutation = new DefaultGraphqlMutation("addRoleBinding");

        List<Map<String, Object>> roleBindings = new ArrayList<>();
        Map<String, Object> roleBinding = new HashMap<>();
        Map<String, Object> app = new HashMap<>();
        app.put("clientId", clientId);
        roleBinding.put("app", app);

        //Map<String, Object> user = new HashMap<>();
        //user.put("account", openid);
        //roleBinding.put("user", user);
        roleBinding.put("source", "iga");

        Map<String, Object> role = new HashMap<>();
        role.put("name", "Iga-" + type + "-" + clientId);

        List<Map<String, Object>> rules = new ArrayList<>();

        String roleType = "";
        if (!CollectionUtils.isEmpty(igaRules)) {
            for (String key : igaRules.keySet()) {
                if ("dept".equals(type)) {
                    roleType = "Department";

                } else {
                    roleType = "Post";
                }
                for (String permission : permissions) {
                    Map<String, Object> rule = new HashMap<>();
                    Map<String, Object> service = new HashMap<>();
                    service.put("name", "iam");
                    rule.put("service", service);
                    rule.put("scope", "RWX");
                    rule.put("type", roleType);
                    rule.put("permission", permission);

                    HashMap<String, Object> codeMap = new HashMap<>();
                    Map<String, Object> filter = new HashMap<>();
                    if (!"parent".equals(key)) {
                        codeMap.put("eq", igaRules.get(key));
                    } else {
                        ArrayList<String> objects = new ArrayList<>();
                        objects.add(igaRules.get(key));
                        codeMap.put("in", objects.toArray());
                    }
                    filter.put(key, codeMap);
                    rule.put("filter", filter);
                    rules.add(rule);
                }

            }
        } else {
            //为空则为人员或人员身份
            if ("person".equals(type)) {
                roleType = "User";

            } else if ("occupy".equals(type)) {
                roleType = "Triple";
            }
            for (String permission : permissions) {
                Map<String, Object> rule = new HashMap<>();
                Map<String, Object> service = new HashMap<>();
                service.put("name", "iam");
                rule.put("service", service);
                rule.put("scope", "RWX");
                rule.put("type", roleType);
                rule.put("permission", permission);
                Map<String, Object> filter = new HashMap<>();
                rule.put("filter", filter);
                rules.add(rule);
            }
        }


        role.put("rules", rules);
        roleBinding.put("role", role);
        roleBindings.add(roleBinding);
        graphqlMutation.getRequestParameter().addObjectParameter("roleBindings", roleBindings).addObjectParameter("overlay", true);
        graphqlMutation.addResultAttributes("id");
        GraphqlResponse graphqlResponse = null;
        try {
            graphqlResponse = graphqlClient.doMutation(graphqlMutation);
        } catch (IOException e) {
            e.printStackTrace();
            throw new CustomException(ResultCode.FAILED, e.getMessage());
        }
        Map result = graphqlResponse.getData();
        if (result.containsKey("errors")) {
            throw new CustomException(ResultCode.FAILED, result.get("errors").toString());
        }
        Map data = (Map) result.get("data");
        ArrayList services = (ArrayList) data.get("addRoleBinding");


        if (null != services) {
            Map o = (Map) services.get(0);
            Object id = o.get("id");
            //todo 返回id 为null 无法判断是否添加成功
            log.info("添加权限成功,id为{}", id);
            return null;
        }
        return null;

    }

}
