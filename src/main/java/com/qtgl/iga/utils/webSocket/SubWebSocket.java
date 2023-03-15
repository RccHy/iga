package com.qtgl.iga.utils.webSocket;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.bo.NodeRules;
import com.qtgl.iga.service.NodeRulesService;
import com.qtgl.iga.service.SubTaskService;
import com.qtgl.iga.service.impl.SubTaskServiceImpl;
import com.qtgl.iga.utils.DataBusUtil;
import com.qtgl.iga.utils.UrlUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Component
public class SubWebSocket {


    @Resource
    DataBusUtil dataBusUtil;
    @Autowired
    SubTaskService subTaskService;
    @Autowired
    NodeRulesService nodeRulesService;

    @Value("${bus.url}")
    private String busUrl;

    private Map<String, ReConnectWebSocketClient> clientMap = new HashMap<>();


    public void listening(DomainInfo domainInfo) throws Exception {
        String key = dataBusUtil.getToken(domainInfo.getDomainName());
        String url = new StringBuffer(UrlUtil.getUrl(busUrl).replace("https", "wss")).append("?access_token=").append(key)
                .append("&domain=").append(domainInfo.getDomainName()).toString();
        // 2023-02-28 增加 websocket 监听
        ReConnectWebSocketClient client =
                new ReConnectWebSocketClient(
                        new URI(url),
                        domainInfo.getDomainName(),
                        // 字符串消息处理
                        msg -> {
                            // todo 字符串消息处理
                            System.out.println("字符串消息:" + msg);
                            JSONObject msgJson = JSONObject.parseObject(msg);
                            // 判断是否是 身份权威源数据发生变化 消息, 如果是,则触发同步任务\
                            String type = msgJson.getJSONObject("sub").getString("type");
                            // 获取发送消息的服务
                            String[] services = (String[]) msgJson.getJSONObject("sub").getJSONArray("services").toArray();
                            List<NodeRules> nodeRules = new ArrayList<>();
                            if ("usersource.cdc".equals(type)) {
                                // 查找对应服务 有哪些规则
                                for (String service : services) {
                                    nodeRules.addAll(nodeRulesService.findNodeRulesByService(service, domainInfo.getDomainName(), "person"));
                                }
                                // 用户变更
                                subTaskService.subTask("PERSON", domainInfo, nodeRules);
                                // 查找对应服务 有哪些规则
                                for (String service : services) {
                                    nodeRules.addAll(nodeRulesService.findNodeRulesByService(service, domainInfo.getDomainName(), "occupy"));
                                }
                                // 身份变更
                                subTaskService.subTask("OCCUPY", domainInfo, nodeRules);
                            }
                        },
                        msgByte -> {
                        },
                        // 异常回调
                        error -> {
                            // todo 字符串消息处理
                            System.out.println("异常:" + error.getMessage());
                        });
        client.connect();
        clientMap.put(domainInfo.getDomainName(), client);
    }
}
