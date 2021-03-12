package com.qtgl.iga.utils;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.apache.oltu.oauth2.common.utils.OAuthUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

public class DataBusUtil {

    public static Logger logger = LoggerFactory.getLogger(DataBusUtil.class);


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
        busUrl=UrlUtil.getUrl(busUrl);
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
        url=UrlUtil.getUrl(url);
        logger.info("pub post url:" + url);
        try {
            String s = Request.Post(url).bodyString(params.toJSONString(), ContentType.APPLICATION_JSON).execute().returnContent().asString();
            logger.info("pub post response:" + s);
            return s;
        } catch (Exception e) {
            e.printStackTrace();
            logger.info("pub post errro:" + e);
        }
        return null;


    }
}
