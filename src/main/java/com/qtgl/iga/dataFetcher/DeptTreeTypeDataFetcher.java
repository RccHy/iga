package com.qtgl.iga.dataFetcher;


import com.alibaba.fastjson.JSON;
import com.qtgl.iga.bo.DeptTreeType;
import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.service.DeptTreeTypeService;
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
public class DeptTreeTypeDataFetcher {

    public static Logger logger = LoggerFactory.getLogger(DeptTreeTypeDataFetcher.class);


    @Autowired
    DeptTreeTypeService deptTreeTypeService;

    /**
     * 查询组织机构树类型
     *
     * @return
     */
    public DataFetcher deptTreeTypes() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            //2。解析查询参数  进行查询
            try {
                List<DeptTreeType> all = deptTreeTypeService.findAll(arguments, domain.getId());
                return all;
            } catch (CustomException e) {
                e.printStackTrace();
                logger.error(domain.getDomainName() + e.getMessage());

                return GraphqlExceptionUtils.getObject("查询组织机构失败", e);
            }
        };
    }

    /**
     * 删除组织机构数类型
     *
     * @return
     */
    public DataFetcher deleteDeptTreeType() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            try {
                DeptTreeType deptTreeType = deptTreeTypeService.deleteDeptTreeType(arguments, domain.getId());
                return deptTreeType;
            } catch (CustomException e) {
                e.printStackTrace();
                logger.error(domain.getDomainName() + e.getMessage());

                return GraphqlExceptionUtils.getObject("删除组织机构失败", e);
            }
        };
    }

    /**
     * 添加组织机构树类型
     *
     * @return
     */
    public DataFetcher saveDeptTreeType() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            DeptTreeType deptTreeType = JSON.parseObject(JSON.toJSONString(arguments.get("entity")), DeptTreeType.class);
            DeptTreeType data = null;
            try {
                data = deptTreeTypeService.saveDeptTreeType(deptTreeType, domain.getId());
                if (null != data) {
                    return data;
                }
                throw new CustomException(ResultCode.FAILED, "添加组织机构树类型失败");
            } catch (CustomException e) {
                e.printStackTrace();
                logger.error(domain.getDomainName() + e.getMessage());

                return GraphqlExceptionUtils.getObject("添加组织机构树类型失败", e);
            }

        };
    }

    /**
     * 修改组织机构树类型
     *
     * @return
     */
    public DataFetcher updateDeptTreeType() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            DeptTreeType deptTreeType = JSON.parseObject(JSON.toJSONString(arguments.get("entity")), DeptTreeType.class);
            deptTreeType.setDomain(domain.getId());
            try {
                DeptTreeType data = deptTreeTypeService.updateDeptTreeType(deptTreeType);
                if (null != data) {
                    return data;
                }
                throw new CustomException(ResultCode.FAILED, "修改组织机构树类型失败");
            } catch (CustomException e) {
                e.printStackTrace();
                logger.error(domain.getDomainName() + e.getMessage());

                return GraphqlExceptionUtils.getObject("修改组织机构树类型失败", e);
            }
        };
    }
}
