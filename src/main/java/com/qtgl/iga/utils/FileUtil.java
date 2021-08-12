package com.qtgl.iga.utils;


import com.alibaba.fastjson.JSONObject;
import com.qtgl.iga.bo.DomainInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;

@Slf4j
@Component
public class FileUtil {

    @Value("${file.url}")
    String fileApi;

    @Value("${sso.url}")
    String ssoUrl;

//    String clientId = "SKvpw2Nm1ZOSifdDeNUk";
//
//    String secret = "2FE5C11C98E53DD9EFD0F2A731B9261515B67A928051A246";
//
//    String scope = "storage";
//
//    String token = null;

    @Resource
    DataBusUtil dataBusUtil;


    public String putFile(byte[] file, String fileName, DomainInfo domainInfo) {
        try {
            MultipartEntityBuilder builder = MultipartEntityBuilder.create()
                    .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
                    .setCharset(Charset.forName("utf-8"));
            builder.addBinaryBody("file", file, ContentType.MULTIPART_FORM_DATA, fileName);
            //处理路径
            if (!(fileApi.startsWith("https://") || fileApi.startsWith("http://"))) {
                URL url = new URL(ssoUrl);
                fileApi = url.getProtocol() + "://" + url.getPath() + fileApi;
            }
            String fileUrl = fileApi.replace("/file", "");
//            getApiToken();
            //获取token
            String token = dataBusUtil.getToken(domainInfo.getDomainName());

            String content = Request.Put(fileApi + "?access_token=" + token)
                    .body(builder.build())
                    .execute().returnContent().asString();
            if (null != content && 0 == JSONObject.parseObject(content).getInteger("errno")) {
                JSONObject object = JSONObject.parseObject(content);
                String uri = fileUrl + object.getJSONArray("entities").getJSONObject(0).getString("uri");
                System.out.println(uri);
                return content;
            }
        } catch (IOException ioException) {
            ioException.printStackTrace();
            log.error("put file error:{}", ioException);
        }
        return null;

    }


//    private void getApiToken() {
//        if (null == token) {
//
//            String sso = fileApi.replace("/file", "/sso");
//
//            OAuthClientRequest oAuthClientRequest = null;
//            try {
//                oAuthClientRequest = OAuthClientRequest
//                        .tokenLocation(sso + "/oauth2/token").setGrantType(GrantType.CLIENT_CREDENTIALS)
//                        .setClientId(clientId).setClientSecret(secret)
//                        .setScope(scope).buildBodyMessage();
//            } catch (OAuthSystemException e) {
//                log.error("token 获取 : ->" + e.getMessage());
//                e.printStackTrace();
//            }
//            OAuthClient oAuthClient = new OAuthClient(new SSLConnectionClient());
//            OAuthJSONAccessTokenResponse oAuthClientResponse = null;
//            try {
//                oAuthClientResponse = oAuthClient.accessToken(oAuthClientRequest, "POST", OAuthJSONAccessTokenResponse.class);
//            } catch (OAuthSystemException | OAuthProblemException e) {
//                log.error("token 获取" + e.getMessage());
//                e.printStackTrace();
//            }
//            assert oAuthClientResponse != null;
//            String accessToken = oAuthClientResponse.getAccessToken();
//            long exp = System.currentTimeMillis() + (oAuthClientResponse.getExpiresIn() * 1000 - (10 * 60 * 1000));
//            token = accessToken;
//        }
//    }
}
