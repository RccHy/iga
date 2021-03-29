package com.qtgl.iga.dataFetcher;

import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.service.DeptService;
import com.qtgl.iga.service.UserTypeService;
import com.qtgl.iga.utils.CertifiedConnector;
import graphql.schema.DataFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DeptDataFetcher {

    public static Logger logger = LoggerFactory.getLogger(DeptDataFetcher.class);


    @Autowired
    DeptService deptService;

    @Autowired
    UserTypeService userTypeService;


    public DataFetcher findDept() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            return deptService.findDept(arguments, domain);
        };
    }

    public DataFetcher findUserType() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            return userTypeService.findUserType(domain);
        };
    }
}
