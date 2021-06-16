package com.qtgl.iga.dataFetcher;


import com.alibaba.fastjson.JSON;
import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.bo.PostType;
import com.qtgl.iga.service.PostTypeService;
import com.qtgl.iga.utils.CertifiedConnector;
import com.qtgl.iga.utils.exception.GraphqlExceptionUtils;
import com.qtgl.iga.utils.enumerate.ResultCode;
import com.qtgl.iga.utils.exception.CustomException;
import graphql.schema.DataFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class PostTypeDataFetcher {

    public static Logger logger = LoggerFactory.getLogger(PostTypeDataFetcher.class);


    @Autowired
    PostTypeService postTypeService;

    /**
     * 查询岗位类型
     *
     * @return
     */
    public DataFetcher postTypes() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            //2。解析查询参数+租户进行  进行查询
            try {
                List<PostType> postTypes = postTypeService.postTypes(arguments, domain.getId());
                return postTypes;
            } catch (CustomException e) {
                e.printStackTrace();
                logger.error(domain.getDomainName() + e.getMessage());

                return GraphqlExceptionUtils.getObject("查询岗位类型失败", e);
            }
        };
    }

    /**
     * 删除岗位类型
     *
     * @return
     */
    public DataFetcher deletePostType() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            try {
                PostType postType = postTypeService.deletePostType(arguments, domain.getId());
                return postType;
            } catch (CustomException e) {
                e.printStackTrace();
                logger.error(domain.getDomainName() + e.getMessage());

                return GraphqlExceptionUtils.getObject("删除岗位类型失败", e);
            }
        };
    }

    /**
     * 添加岗位类型
     *
     * @return
     */
    public DataFetcher savePostType() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            PostType postType = JSON.parseObject(JSON.toJSONString(arguments.get("entity")), PostType.class);
            try {
                PostType data = postTypeService.savePostType(postType, domain.getId());
                if (null != data) {
                    return data;
                }
                throw new CustomException(ResultCode.FAILED, "添加岗位类型失败");
            } catch (CustomException e) {
                e.printStackTrace();
                logger.error(domain.getDomainName() + e.getMessage());

                return GraphqlExceptionUtils.getObject("添加岗位类型失败", e);
            }
        };
    }

    /**
     * 修改岗位类型
     *
     * @return
     */
    public DataFetcher updatePostType() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            PostType postType = JSON.parseObject(JSON.toJSONString(arguments.get("entity")), PostType.class);
            postType.setDomain(domain.getId());
            try {
                PostType data = postTypeService.updatePostType(postType);
                if (null != data) {
                    return data;
                }
                throw new CustomException(ResultCode.FAILED, "修改岗位类型失败");
            } catch (CustomException e) {
                e.printStackTrace();
                logger.error(domain.getDomainName() + e.getMessage());

                return GraphqlExceptionUtils.getObject("修改岗位类型失败", e);
            }
        };
    }
}
