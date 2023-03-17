package com.qtgl.iga.utils;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.service.DeptRelationTypeService;
import com.qtgl.iga.service.DomainInfoService;
import com.qtgl.iga.utils.webSocket.SubWebSocket;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.oltu.oauth2.common.utils.OAuthUtils;
import org.apache.tomcat.util.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;


@Component
@Slf4j
public class CertifiedConnector {


    private static CertifiedConnector certifiedConnector;


    @Autowired
    DomainInfoService domainInfoService;
    @Autowired
    DeptRelationTypeService deptRelationTypeService;
    @Autowired
    SubWebSocket subWebSocket;
    @Value("${sso.introspect.url}")
    String url;

    @Value("${app.client}")
    String clientId;
    @Value("${app.secret}")
    String clientSecret;




    public void set(DomainInfoService domainInfoService, String url, String clientId, String clientSecret, DeptRelationTypeService deptRelationTypeService) {
        this.domainInfoService = domainInfoService;
        this.url = url;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.deptRelationTypeService = deptRelationTypeService;
    }

    @PostConstruct
    public void init() {
        certifiedConnector = this;
        certifiedConnector.domainInfoService = this.domainInfoService;
        certifiedConnector.url = this.url;
        certifiedConnector.clientId = this.clientId;
        certifiedConnector.clientSecret = this.clientSecret;
        certifiedConnector.deptRelationTypeService = this.deptRelationTypeService;
    }

    /**
     * 根据当前会话校验 并获取租户信息
     *
     * @return
     * @throws Exception
     */
    public static DomainInfo getDomain() throws Exception {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        CertifiedConnector certifiedConnector = new CertifiedConnector();
        DomainInfo domain = certifiedConnector.introspect(request);
        return domain;
    }


    /**
     * 校验当前domain信息
     *
     * @param request
     * @return
     * @throws Exception
     */
    public DomainInfo introspect(HttpServletRequest request) throws Exception {
        // 如果url是相对路径
        String ssoUrl = UrlUtil.getUrl(certifiedConnector.url);
        //DomainInfo domainInfo = certifiedConnector.domainInfoService.findAll().get(0);
        String domainName = CertifiedConnector.introspect(request, ssoUrl, certifiedConnector.clientId, certifiedConnector.clientSecret);
        if (null == domainName) {
            throw new Exception("ACCESS_TOKEN_INVALID");
        }
        DomainInfo byDomainName = certifiedConnector.domainInfoService.getByDomainName(domainName);
        if (null == byDomainName) {
            // 创建租户
            byDomainName = new DomainInfo(domainName, null, new Timestamp(System.currentTimeMillis()), 0, certifiedConnector.clientId, certifiedConnector.clientSecret);
            try {
                certifiedConnector.domainInfoService.install(byDomainName);
                // 租户初始化完成后,开启监听
                subWebSocket.listening(byDomainName);
                //todo 指定运行 权威源为console的 同步

                //GraphQLService.setDomainGraphQLMap(certifiedConnector.runner.buildGraphql());
            } catch (Exception e) {
                e.printStackTrace();
                throw new Exception("create tenant error");
            }
        } else {
            // 修复历史版本升级丢失 组织机构关系数据
            certifiedConnector.deptRelationTypeService.initialization(byDomainName.getId());
        }

        return byDomainName;
    }

    public static String introspect(HttpServletRequest request, String uri, String clientId, String clientSecret) throws Exception {

        Assert.hasText(uri, "OAUTH2_URI not set");
        Assert.hasText(clientId, "OAUTH2_CLIENT_ID not set");
        Assert.hasText(clientSecret, "OAUTH2_CLIENT_SECRET not set");

        String token = OAuthUtils.getAuthHeaderField(request.getHeader("Authorization"));

        if (StringUtils.isBlank(token))
            token = request.getParameter("access_token");


        Assert.hasText(token, "Missing required args '[access_token]'");

        String plainCreds = clientId + ":" + clientSecret;
        byte[] plainCredsBytes = plainCreds.getBytes();
        byte[] base64CredsBytes = Base64.encodeBase64(plainCredsBytes);
        String base64Creds = new String(base64CredsBytes);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Basic " + base64Creds);

        HttpEntity<String> httpEntity = new HttpEntity<>("body", headers);
        String url = String.format("%s?token=%s", uri, token);
        if (uri.indexOf("/oauth2/introspect") > -1)
            url = String.format("%s?token=%s", uri, token);
        try {
            log.info("introspect base64Creds:" + base64Creds);
            log.info("introspect url:" + url);
            ResponseEntity responseEntity = new RestTemplate().exchange(url, HttpMethod.POST, httpEntity, String.class);
            Map introspection = new ObjectMapper().readValue(responseEntity.getBody().toString(), Map.class);
            if (introspection != null && (Boolean) introspection.get("active")) {
                return introspection.containsKey("tenant") ? introspection.get("tenant").toString() : request.getParameter("domain");
            } else {
                return null;
            }
        } catch (Exception e) {
            throw new Exception(e);
        }
    }

    //private String getSSOUrl(HttpServletRequest request) {
    //
    //    String personalUrl = certifiedConnector.url;
    //    String port = ":" + request.getServerPort();
    //    if (("http".equalsIgnoreCase(request.getScheme()) && request.getServerPort() == 80)
    //            || ("https".equalsIgnoreCase(request.getScheme()) && request.getServerPort() == 443)) {
    //        port = "";
    //    }
    //
    //    String base = request.getScheme() + "://" + request.getServerName() + port;
    //    if (!isUrl(personalUrl)) {
    //        personalUrl = personalUrl.startsWith("/") ? personalUrl : ("/" + personalUrl);
    //        personalUrl = base + personalUrl;
    //    }
    //
    //    if (StringUtils.isBlank(certifiedConnector.url)) {
    //        return "";
    //    }
    //
    //    return personalUrl;
    //}
    //
    //private static boolean isUrl(String pInput) {
    //    if (pInput == null) {
    //        return false;
    //    }
    //    pInput = pInput.trim().toLowerCase();
    //    if (pInput.startsWith("http://") || pInput.startsWith("https://")) {
    //        return true;
    //    }
    //    return false;
    //}

}
