package com.qtgl.iga.dataFetcher;


import com.alibaba.fastjson.JSON;
import com.qtgl.iga.bo.UpStream;
import com.qtgl.iga.service.UpStreamService;
import graphql.schema.DataFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class UpStreamFetcher {

    public static Logger logger = LoggerFactory.getLogger(UpStreamFetcher.class);


    @Autowired
    UpStreamService upStreamService;


    public DataFetcher upStreams() {
        return dataFetchingEvn -> {
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            return upStreamService.findAll(arguments);
        };
    }

    public DataFetcher deleteUpStream() throws Exception {
        return dataFetchingEvn -> {
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            return upStreamService.deleteUpStream(arguments);
        };
    }

    public DataFetcher saveUpStream() {
        return dataFetchingEvn -> {
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            UpStream upStream = JSON.parseObject(JSON.toJSONString(arguments.get("entity")), UpStream.class);
            if (null != upStreamService.saveUpStream(upStream)) {
                return upStreamService.saveUpStream(upStream);
            }
            throw new Exception("添加失败");
        };
    }

    public DataFetcher updateUpStream() {
        return dataFetchingEvn -> {
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            UpStream upStream = JSON.parseObject(JSON.toJSONString(arguments.get("entity")), UpStream.class);
            if (null != upStreamService.updateUpStream(upStream)) {
                return upStreamService.updateUpStream(upStream);
            }
            throw new Exception("修改失败");
        };
    }
}
