package com.qtgl.iga.utils;


import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.Charset;

@Slf4j
@Component
public class FileUtil {

    @Value("${file.api}")
    String fileApi;


    public String putFile(byte[] file, String fileName) {
        try {
            MultipartEntityBuilder builder = MultipartEntityBuilder.create()
                    .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
                    .setCharset(Charset.forName("utf-8"));
            builder.addBinaryBody("file", file, ContentType.MULTIPART_FORM_DATA, fileName);
            String fileUrl = fileApi.replace("/file", "");
            String content = Request.Put(fileApi + "?access_token=XXX")
                    .body(builder.build())
                    .execute().returnContent().asString();
            if (null != content && 0 == JSONObject.parseObject(content).getInteger("errno")) {
                JSONObject object = JSONObject.parseObject(content);
                String uri = fileUrl + object.getJSONArray("entities").getJSONObject(0).getString("uri");
                return uri;
            }
        } catch (IOException ioException) {
            ioException.printStackTrace();
            log.error("put file error:{}", ioException);
        }
        return null;

    }
}
