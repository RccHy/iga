package com.qtgl.iga.service.impl;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.qtgl.iga.bo.ShadowCopy;
import com.qtgl.iga.dao.ShadowCopyDao;
import com.qtgl.iga.service.ShadowCopyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

@Service
@Slf4j
public class ShadowCopyServiceImpl implements ShadowCopyService {
    @Resource
    ShadowCopyDao shadowCopyDao;

    @Override
    public ShadowCopy save(ShadowCopy shadowCopy) {
        if ("person".equals(shadowCopy.getType())) {
            System.out.println(1);
        }
        System.out.println("------------开始压缩" + LocalDateTime.now());
        //通过deflater 压缩数据
        Deflater deflater = new Deflater(8); // 0 ~ 9 压缩等级 低到高
        deflater.setInput(shadowCopy.getData());
        deflater.finish();

        final byte[] bytes = new byte[256];
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(256);

        while (!deflater.finished()) {
            int length = deflater.deflate(bytes);
            outputStream.write(bytes, 0, length);
        }

        deflater.end();
        System.out.println("------------压缩完毕" + LocalDateTime.now());
        Base64.Encoder encoder = Base64.getEncoder();
        String str = encoder.encodeToString(outputStream.toByteArray());
        //根据权威源类型及type 及租户查询是否已有副本
        ShadowCopy shadowCopyFromDb = shadowCopyDao.findByUpstreamTypeAndType(shadowCopy.getUpstreamTypeId(), shadowCopy.getType(), shadowCopy.getDomain());
        if (null != shadowCopyFromDb) {
            shadowCopy = shadowCopyFromDb;
        }
        shadowCopy.setData(str.getBytes(StandardCharsets.UTF_8));
        return shadowCopyDao.save(shadowCopy);
    }

    @Override
    public JSONArray findDataByUpstreamTypeAndType(String upstreamTypeId, String type, String domain) {
        ShadowCopy shadowCopy = shadowCopyDao.findDataByUpstreamTypeAndType(upstreamTypeId, type, domain);
        if (null != shadowCopy) {
            Base64.Decoder decoder = Base64.getDecoder();
            byte[] decode = decoder.decode(shadowCopy.getData());

            Inflater inflater = new Inflater();
            inflater.setInput(decode);
            final byte[] bytes1 = new byte[256];
            ByteArrayOutputStream outputStream1 = new ByteArrayOutputStream(256);
            try {
                while (!inflater.finished()) {
                    int length = inflater.inflate(bytes1);
                    outputStream1.write(bytes1, 0, length);
                }
            } catch (DataFormatException e) {
                e.printStackTrace();
                return null;
            } finally {
                inflater.end();
            }
            return JSON.parseArray(outputStream1.toString());
        }
        return new JSONArray();
    }
}
