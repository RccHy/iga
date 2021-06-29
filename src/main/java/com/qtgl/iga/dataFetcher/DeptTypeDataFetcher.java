package com.qtgl.iga.dataFetcher;


import com.alibaba.fastjson.JSON;
import com.qtgl.iga.bo.DeptType;
import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.service.DeptTypeService;
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
public class DeptTypeDataFetcher {

    public static Logger logger = LoggerFactory.getLogger(DeptTypeDataFetcher.class);


    @Autowired
    DeptTypeService deptTypeService;

    /**
     * 查询组织机构类型
     *
     * @return
     */
    public DataFetcher deptTypes() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            //2。解析查询参数+租户进行  进行查询
            try {
                List<DeptType> allDeptTypes = deptTypeService.getAllDeptTypes(arguments, domain.getId());
                return allDeptTypes;
            } catch (CustomException e) {
                e.printStackTrace();
                logger.error(domain.getDomainName() + e.getMessage());

                return GraphqlExceptionUtils.getObject("查询组织机构类型失败", e);
            }
        };
    }

    /**
     * 删除组织机构类型
     *
     * @return
     */
    public DataFetcher deleteDeptTypes() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            try {
                DeptType deptType = deptTypeService.deleteDeptTypes(arguments, domain.getId());
                return deptType;
            } catch (CustomException e) {
                e.printStackTrace();
                logger.error(domain.getDomainName() + e.getMessage());

                return GraphqlExceptionUtils.getObject("删除组织机构类型失败", e);
            }
        };
    }

    /**
     * 添加组织机构类型
     *
     * @return
     */
    public DataFetcher saveDeptTypes() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            DeptType deptType = JSON.parseObject(JSON.toJSONString(arguments.get("entity")), DeptType.class);
            try {
                DeptType data = deptTypeService.saveDeptTypes(deptType, domain.getId());
                if (null != data) {
                    return data;
                }
                throw new CustomException(ResultCode.FAILED, "添加组织机构类型失败");
            } catch (CustomException e) {
                e.printStackTrace();
                logger.error(domain.getDomainName() + e.getMessage());

                return GraphqlExceptionUtils.getObject("添加组织机构类型失败", e);
            }

        };
    }

    /**
     * 修改组织机构类型
     *
     * @return
     */
    public DataFetcher updateDeptTypes() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            DeptType deptType = JSON.parseObject(JSON.toJSONString(arguments.get("entity")), DeptType.class);
            deptType.setDomain(domain.getId());
            try {
                DeptType data = deptTypeService.updateDeptTypes(deptType);
                if (null != data) {
                    return data;
                }
                throw new CustomException(ResultCode.FAILED, "修改组织机构类型失败");

            } catch (CustomException e) {
                e.printStackTrace();
                logger.error(domain.getDomainName() + e.getMessage());

                return GraphqlExceptionUtils.getObject("修改组织机构类型失败", e);
            }
        };
    }
}
