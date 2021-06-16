package com.qtgl.iga.dataFetcher;


import com.alibaba.fastjson.JSON;
import com.qtgl.iga.bo.DomainInfo;

import com.qtgl.iga.bo.UpstreamType;
import com.qtgl.iga.service.UpstreamTypeService;
import com.qtgl.iga.utils.CertifiedConnector;
import com.qtgl.iga.utils.exception.GraphqlExceptionUtils;
import com.qtgl.iga.utils.enumerate.ResultCode;
import com.qtgl.iga.utils.exception.CustomException;
import graphql.schema.DataFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class UpstreamTypeFetcher {

    public static Logger logger = LoggerFactory.getLogger(UpstreamTypeFetcher.class);


    @Autowired
    UpstreamTypeService upStreamTypeService;

    /**
     * 查询权威源类型
     *
     * @return
     */
    public DataFetcher upstreamTypes() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            //2。解析查询参数  进行查询
            try {
                return upStreamTypeService.findAll(arguments, domain.getId());
            } catch (CustomException e) {
                e.printStackTrace();
                logger.error(domain.getDomainName() + e.getMessage());

                return GraphqlExceptionUtils.getObject("查询权威源类型失败", e);
            }
        };
    }

    /**
     * 删除权威源类型
     *
     * @return
     * @throws Exception
     */
    public DataFetcher deleteUpstreamType() throws Exception {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            try {
                return upStreamTypeService.deleteUpstreamType(arguments, domain.getId());
            } catch (CustomException e) {
                e.printStackTrace();
                logger.error(domain.getDomainName() + e.getMessage());

                return GraphqlExceptionUtils.getObject("删除权威源类型失败", e);
            }
        };
    }

    /**
     * 添加权威源类型
     *
     * @return
     */
    public DataFetcher saveUpstreamType() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();

            UpstreamType upstreamType = JSON.parseObject(JSON.toJSONString(arguments.get("entity")), UpstreamType.class);

            try {
                UpstreamType data = upStreamTypeService.saveUpstreamType(upstreamType, domain.getId());
                if (null != data) {
                    return data;
                }
                throw new CustomException(ResultCode.FAILED, "添加权威源类型失败");
            } catch (CustomException e) {
                e.printStackTrace();
                logger.error(domain.getDomainName() + e.getMessage());

                return GraphqlExceptionUtils.getObject("添加权威源类型失败", e);
            }
        };
    }

    /**
     * 修改权威源类型
     *
     * @return
     */
    public DataFetcher updateUpstreamType() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            UpstreamType upstreamType = JSON.parseObject(JSON.toJSONString(arguments.get("entity")), UpstreamType.class);
            upstreamType.setDomain(domain.getId());
            try {
                UpstreamType data = upStreamTypeService.updateUpstreamType(upstreamType);
                if (null != data) {
                    return data;
                }
                throw new CustomException(ResultCode.FAILED, "修改权威源类型失败");
            } catch (CustomException e) {
                e.printStackTrace();
                logger.error(domain.getDomainName() + e.getMessage());

                return GraphqlExceptionUtils.getObject("修改权威源类型失败", e);
            }
        };
    }

    /**
     * 获取权威源拉取的数据
     *
     * @return
     */
    public DataFetcher upstreamTypesData() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            //2。解析查询参数  进行查询
            HashMap<Object, Object> map = null;
            try {
                map = upStreamTypeService.upstreamTypesData(arguments, domain.getDomainName());
            } catch (CustomException e) {
                e.printStackTrace();
                return GraphqlExceptionUtils.getObject("查询失败", e);
            }

            return map;
        };
    }
}
