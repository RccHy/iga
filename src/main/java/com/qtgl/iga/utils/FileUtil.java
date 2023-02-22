package com.qtgl.iga.utils;


import com.alibaba.fastjson.JSONObject;
import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.utils.enumerate.ResultCode;
import com.qtgl.iga.utils.exception.CustomException;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;

@Slf4j
@Component
public class FileUtil {

    @Value("${file.url}")
    String fileUrl;


    //String clientId = "SKvpw2Nm1ZOSifdDeNUk";
    //
    //String secret = "2FE5C11C98E53DD9EFD0F2A731B9261515B67A928051A246";
    //
    //String scope = "storage";
    //
    String token = null;

    @Resource
    DataBusUtil dataBusUtil;


    public String putFile(byte[] file, String fileName, DomainInfo domainInfo) {
        try {
            MultipartEntityBuilder builder = MultipartEntityBuilder.create()
                    .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
                    .setCharset(Charset.forName("utf-8"));
            builder.addBinaryBody("file", file, ContentType.MULTIPART_FORM_DATA, fileName);
            //处理路径
            //if (!(fileApi.startsWith("https://") || fileApi.startsWith("http://"))) {
            //    URL url = new URL(ssoUrl);
            //    fileApi = url.getProtocol() + "://" + url.getPath() + fileApi;
            //}
            fileUrl = UrlUtil.getUrl(fileUrl);
            //String url = fileUrl.replace("/file", "");
            //getApiToken();
            //获取token
            token = dataBusUtil.getToken(domainInfo.getDomainName());
            System.out.println(fileUrl + "?access_token=" + token);
            String content = Request.Put(fileUrl + "?access_token=" + token)
                    .body(builder.build())
                    .execute().returnContent().asString();
            if (null != content && 0 == JSONObject.parseObject(content).getInteger("errno")) {
                JSONObject object = JSONObject.parseObject(content);
                //String uri = url + object.getJSONArray("entities").getJSONObject(0).getString("uri");
                //System.out.println(uri);
                return content;
            }
        } catch (IOException ioException) {
            ioException.printStackTrace();
            log.error("put file error:{}", ioException);
            throw new CustomException(ResultCode.FAILED, "上传文件失败,上传路径为:" + fileUrl);
        }
        return null;

    }

    public String putFile(File file, String fileName, DomainInfo domainInfo) {
        String fileGqlUrl = null;
        try {
            // 通过 fluent.Request 发送post请求，设置请求头为multipart/form-data，参数为operations 和 map
            // operations 为json字符串，包含query和variables，其中variables中的file为null
            // map 为json字符串，包含0: ["variables.file"]
            // 0 为文件的索引，对应operations中的variables.file
            // @a.txt 为文件路径

            String operations = "{ \"query\": \"mutation ($file: Upload!) { upload(file: $file) { uri } }\", \"variables\": { \"file\": null } }";
            String map = "{\"0\":[\"variables.file\"]}";

            byte[] bytes = Files.readAllBytes(file.toPath());
            MultipartEntityBuilder builder = MultipartEntityBuilder.create()
                    .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
                    .setCharset(Charset.forName("utf-8"));
            builder.addTextBody("operations", operations, ContentType.MULTIPART_FORM_DATA);
            builder.addTextBody("map", map, ContentType.MULTIPART_FORM_DATA);
            builder.addBinaryBody("0", bytes, ContentType.MULTIPART_FORM_DATA, fileName);
            fileGqlUrl = dataBusUtil.invokeUrlByName(domainInfo.getDomainName(), "file_v2");
            fileGqlUrl = UrlUtil.getUrl(fileGqlUrl);
            log.info("put file to fileAPI-V2:" + fileGqlUrl + "?access_token=" + dataBusUtil.getToken(domainInfo.getDomainName()) + "&domain=" + domainInfo.getDomainName());
            String content = Request.Post(fileGqlUrl + "?access_token=" + dataBusUtil.getToken(domainInfo.getDomainName()) + "&domain=" + domainInfo.getDomainName())
                    .body(builder.build())
                    .execute().returnContent().asString();
            if (null != content && null != JSONObject.parseObject(content).getJSONObject("data")) {
                JSONObject object = JSONObject.parseObject(content).getJSONObject("data").getJSONObject("upload");
                //String uri = url + object.getJSONArray("entities").getJSONObject(0).getString("uri");
                //System.out.println(uri);
                return object.getString("uri");
            }
        } catch (Exception ioException) {
            ioException.printStackTrace();
            log.error("put file error:{}", ioException);
            throw new CustomException(ResultCode.FAILED, "上传文件失败,上传路径为:" + fileGqlUrl);
        }
        return "";

    }


    public String putFileByGql(byte[] file, String fileName, DomainInfo domainInfo) {
        String fileGqlUrl = null;
        try {
            // 通过 fluent.Request 发送post请求，设置请求头为multipart/form-data，参数为operations 和 map
            // operations 为json字符串，包含query和variables，其中variables中的file为null
            // map 为json字符串，包含0: ["variables.file"]
            // 0 为文件的索引，对应operations中的variables.file
            // @a.txt 为文件路径

            String operations = "{ \"query\": \"mutation ($file: Upload!) { upload(file: $file) { uri } }\", \"variables\": { \"file\": null } }";
            String map = "{\"0\":[\"variables.file\"]}";

            MultipartEntityBuilder builder = MultipartEntityBuilder.create()
                    .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
                    .setCharset(Charset.forName("utf-8"));
            builder.addTextBody("operations", operations, ContentType.MULTIPART_FORM_DATA);
            builder.addTextBody("map", map, ContentType.MULTIPART_FORM_DATA);
            builder.addBinaryBody("0", file, ContentType.MULTIPART_FORM_DATA, fileName);

            fileGqlUrl = dataBusUtil.invokeUrlByName(domainInfo.getDomainName(), "file_v2");
            fileGqlUrl = UrlUtil.getUrl(fileGqlUrl);
            log.info("put file to fileAPI-V2:" + fileGqlUrl + "?access_token=" + dataBusUtil.getToken(domainInfo.getDomainName()) + "&domain=" + domainInfo.getDomainName());
            String content = Request.Post(fileGqlUrl + "?access_token=" + dataBusUtil.getToken(domainInfo.getDomainName()) + "&domain=" + domainInfo.getDomainName())
                    .body(builder.build())
                    .execute().returnContent().asString();
            if (null != content && null != JSONObject.parseObject(content).getJSONObject("data")) {
                JSONObject object = JSONObject.parseObject(content).getJSONObject("data").getJSONObject("upload");
                //String uri = url + object.getJSONArray("entities").getJSONObject(0).getString("uri");
                //System.out.println(uri);
                return object.getString("uri");
            }
        } catch (Exception ioException) {
            ioException.printStackTrace();
            log.error("put file error:{}", ioException);
            throw new CustomException(ResultCode.FAILED, "上传文件失败,上传路径为:" + fileGqlUrl);
        }
        return "";

    }


    //private void getApiToken() {
    //    if (null == token) {
    //
    //        String sso = fileUrl.replace("/file", "/sso");
    //
    //        OAuthClientRequest oAuthClientRequest = null;
    //        try {
    //            oAuthClientRequest = OAuthClientRequest
    //                    .tokenLocation(sso + "/oauth2/token").setGrantType(GrantType.CLIENT_CREDENTIALS)
    //                    .setClientId(clientId).setClientSecret(secret)
    //                    .setScope(scope).buildBodyMessage();
    //        } catch (OAuthSystemException e) {
    //            log.error("token 获取 : ->" + e.getMessage());
    //            e.printStackTrace();
    //        }
    //        OAuthClient oAuthClient = new OAuthClient(new SSLConnectionClient());
    //        OAuthJSONAccessTokenResponse oAuthClientResponse = null;
    //        try {
    //            oAuthClientResponse = oAuthClient.accessToken(oAuthClientRequest, "POST", OAuthJSONAccessTokenResponse.class);
    //        } catch (OAuthSystemException | OAuthProblemException e) {
    //            log.error("token 获取" + e.getMessage());
    //            e.printStackTrace();
    //        }
    //        assert oAuthClientResponse != null;
    //        String accessToken = oAuthClientResponse.getAccessToken();
    //        long exp = System.currentTimeMillis() + (oAuthClientResponse.getExpiresIn() * 1000 - (10 * 60 * 1000));
    //        token = accessToken;
    //    }
    //}
}
