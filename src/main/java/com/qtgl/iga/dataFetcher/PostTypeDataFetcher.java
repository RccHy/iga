package com.qtgl.iga.dataFetcher;


import com.alibaba.fastjson.JSON;
import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.bo.PostType;
import com.qtgl.iga.service.PostTypeService;
import com.qtgl.iga.utils.CertifiedConnector;
import graphql.schema.DataFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PostTypeDataFetcher {

    public static Logger logger = LoggerFactory.getLogger(PostTypeDataFetcher.class);


    @Autowired
    PostTypeService postTypeService;


    public DataFetcher postTypes() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            //2。解析查询参数+租户进行  进行查询
            return postTypeService.postTypes(arguments, domain.getId());
        };
    }


    public DataFetcher deletePostType() throws Exception {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            return postTypeService.deletePostType(arguments, domain.getId());
        };
    }

    public DataFetcher savePostType() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            PostType postType = JSON.parseObject(JSON.toJSONString(arguments.get("entity")), PostType.class);
            PostType data = postTypeService.savePostType(postType, domain.getId());
            if (null != data) {
                return data;
            }
            throw new Exception("添加失败");
        };
    }

    public DataFetcher updatePostType() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            PostType postType = JSON.parseObject(JSON.toJSONString(arguments.get("entity")), PostType.class);
            postType.setDomain(domain.getId());
            PostType data = postTypeService.updatePostType(postType);
            if (null != data) {
                return data;
            }
            throw new Exception("修改失败");
        };
    }
}