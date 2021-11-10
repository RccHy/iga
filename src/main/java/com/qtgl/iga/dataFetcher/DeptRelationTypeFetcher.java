package com.qtgl.iga.dataFetcher;

import com.qtgl.iga.bo.DeptRelationType;
import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.service.DeptRelationTypeService;
import com.qtgl.iga.utils.CertifiedConnector;
import com.qtgl.iga.utils.exception.CustomException;
import com.qtgl.iga.utils.exception.GraphqlExceptionUtils;
import graphql.schema.DataFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DeptRelationTypeFetcher {
        public static Logger logger = LoggerFactory.getLogger(DeptDataFetcher.class);


    @Autowired
    DeptRelationTypeService deptRelationTypeService;

     public DataFetcher findDeptRelationType() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            try {
                List<DeptRelationType> dept = deptRelationTypeService.findAll(domain.getId());
                return dept;
            } catch (CustomException e) {
                e.printStackTrace();
                logger.error(domain.getDomainName() + e.getErrorMsg());
                return GraphqlExceptionUtils.getObject("查询部门关系分类失败", e);


            }
        };
    }
}
