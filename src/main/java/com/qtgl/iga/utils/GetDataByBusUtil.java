package com.qtgl.iga.utils;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.oltu.oauth2.client.OAuthClient;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.response.OAuthJSONAccessTokenResponse;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <FileName> GetDataByBusUtil
 * <Desc> 通过url获取数据
 **/
public class GetDataByBusUtil {

    @Value("app.scope")
    private static String appScope;
    @Value("sso.url")
    private static String ssoUrl;
    @Value("app.client")
    private static String appKey;
    @Value("app.secret")
    private static String appSecret;

    private static ConcurrentHashMap<String,Object> token;



    public static String getDataByBus(String url) throws OAuthSystemException, OAuthProblemException {
        //根据url 获取实际url
        String dealUrl = dealUrl(url);
        System.out.println(dealUrl);
//        //获取token
//        String t = getToken();
//        //调用
//        invokeUrl(dealUrl, token);

        return dealUrl;
    }

    private  static String dealUrl(String url) {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        StringBuffer path = request.getRequestURL();
        String tempContextUrl = path.delete(path.length() - request.getRequestURI().length(), path.length())
                .append(request.getServletContext().getContextPath()).append("/").append("graphql").append("/").toString();
        //工具类过滤处理url
        String realUrl = UrlUtil.getUrl(tempContextUrl);
        String referer = request.getHeader("referer");
        System.out.println(referer);
        return realUrl;

    }

    private static String getToken() throws OAuthSystemException, OAuthProblemException {
        Object[] objects = Arrays.stream(appScope.replace("+", " ").split(" ")).filter(s -> s.contains("sys_")).toArray();
        String scope = ArrayUtils.toString(objects, ",").replace("{", "").replace("}", "");
        OAuthClientRequest oAuthClientRequest = OAuthClientRequest
                .tokenLocation(ssoUrl + "/oauth2/token").setGrantType(GrantType.CLIENT_CREDENTIALS)
                .setClientId(appKey).setClientSecret(appSecret)
                .setScope(scope.replace(",", " ")).buildBodyMessage();
        OAuthClient oAuthClient = new OAuthClient(new SSLConnectionClient());
        OAuthJSONAccessTokenResponse oAuthClientResponse = (OAuthJSONAccessTokenResponse) oAuthClient.accessToken(oAuthClientRequest, "POST", OAuthJSONAccessTokenResponse.class);
        String accessToken = oAuthClientResponse.getAccessToken();
//        token = new Token(oAuthClientResponse.getAccessToken(), oAuthClientResponse.getExpiresIn(), System.currentTimeMillis());
        return accessToken;
    }

    private void invokeUrl(String url, ConcurrentHashMap<String, Object> token) {

    }


}
