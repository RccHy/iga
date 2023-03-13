package com.qtgl.iga.dataFetcher;


import com.alibaba.fastjson.JSON;
import com.qtgl.iga.bean.UpstreamDto;
import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.bo.Upstream;
import com.qtgl.iga.service.UpstreamService;
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
public class UpstreamFetcher {

    public static Logger logger = LoggerFactory.getLogger(UpstreamFetcher.class);


    @Autowired
    UpstreamService upstreamService;

    /**
     * 查询权威源
     *
     * @return
     */
    public DataFetcher upstreams() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            //2。解析查询参数  进行查询
            try {
                List<UpstreamDto> all = upstreamService.findAll(arguments, domain.getId());
                return all;
            } catch (CustomException e) {
                e.printStackTrace();
                logger.error(domain.getDomainName() + e.getMessage());

                return GraphqlExceptionUtils.getObject("查询权威源失败", e);
            }
        };
    }

    /**
     * 删除权威源
     *
     * @return
     */
    public DataFetcher deleteUpstream() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            try {
                return upstreamService.deleteUpstream(arguments, domain.getId());
            } catch (CustomException e) {
                e.printStackTrace();
                logger.error(domain.getDomainName() + e.getMessage());

                return GraphqlExceptionUtils.getObject("删除权威源失败", e);
            }
        };
    }

    /**
     * 添加权威源
     *
     * @return
     */
    public DataFetcher saveUpstream() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            Upstream upstream = JSON.parseObject(JSON.toJSONString(arguments.get("entity")), Upstream.class);
            try {
                Upstream data = upstreamService.saveUpstream(upstream, domain.getId());
                if (null != data) {
                    return data;
                }
                throw new CustomException(ResultCode.FAILED, "添加权威源失败");
            } catch (CustomException e) {
                e.printStackTrace();
                logger.error(domain.getDomainName() + e.getMessage());

                return GraphqlExceptionUtils.getObject("添加权威源失败", e);
            }
        };
    }

    /**
     * 修改权威源
     *
     * @return
     */
    public DataFetcher updateUpstream() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            Upstream upstream = JSON.parseObject(JSON.toJSONString(arguments.get("entity")), Upstream.class);
            upstream.setDomain(domain.getId());
            try {
                Upstream data = upstreamService.updateUpstream(upstream);
                if (null != data) {
                    return data;
                }
                throw new CustomException(ResultCode.FAILED, "修改权威源失败");
            } catch (CustomException e) {
                e.printStackTrace();
                logger.error(domain.getDomainName() + e.getMessage());

                return GraphqlExceptionUtils.getObject("修改权威源失败", e);
            }
        };
    }

    /**
     * 添加权威源及类型
     *
     * @return
     */
    public DataFetcher saveUpstreamAndTypes() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            UpstreamDto upstream = JSON.parseObject(JSON.toJSONString(arguments.get("entity")), UpstreamDto.class);
            try {
                UpstreamDto data = upstreamService.saveUpstreamAndTypes(upstream, domain.getId());
                if (null != data) {
                    return data;
                }
                throw new CustomException(ResultCode.FAILED, "添加权威源失败");
            } catch (CustomException e) {
                e.printStackTrace();
                logger.error(domain.getDomainName() + e.getMessage());

                return GraphqlExceptionUtils.getObject("添加权威源失败", e);
            }
        };

    }

    /**
     * 查询权威源及权威源类型
     *
     * @return
     */
    public DataFetcher upstreamsAndTypes() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            //2。解析查询参数  进行查询
            try {
                return upstreamService.upstreamsAndTypes(arguments, domain.getId());
            } catch (CustomException e) {
                e.printStackTrace();
                logger.error(domain.getDomainName() + e.getMessage());

                return GraphqlExceptionUtils.getObject("查询失败", e);
            }
        };
    }

    /**
     * 修改权威源及权威源类型
     *
     * @return
     */
    public DataFetcher updateUpstreamAndTypes() {

        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            UpstreamDto upstream = JSON.parseObject(JSON.toJSONString(arguments.get("entity")), UpstreamDto.class);
            upstream.setDomain(domain.getId());
            try {
                UpstreamDto data = upstreamService.updateUpstreamAndTypes(upstream);
                if (null != data) {
                    return data;
                }
                throw new CustomException(ResultCode.FAILED, "修改失败");
            } catch (CustomException e) {
                e.printStackTrace();
                logger.error(domain.getDomainName() + e.getMessage());

                return GraphqlExceptionUtils.getObject("修改失败", e);
            }
        };
    }
}
