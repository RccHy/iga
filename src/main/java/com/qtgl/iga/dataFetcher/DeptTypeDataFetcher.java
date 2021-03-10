package com.qtgl.iga.dataFetcher;


import com.alibaba.fastjson.JSON;
import com.qtgl.iga.bo.DeptType;
import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.service.DeptTypeService;
import com.qtgl.iga.utils.CertifiedConnector;
import graphql.schema.DataFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DeptTypeDataFetcher {

    public static Logger logger = LoggerFactory.getLogger(DeptTypeDataFetcher.class);


    @Autowired
    DeptTypeService deptTypeService;


    public DataFetcher deptTypes() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            //2。解析查询参数+租户进行  进行查询
            return deptTypeService.getAllDeptTypes(arguments, domain.getId());
        };
    }


    public DataFetcher deleteSchemaField() throws Exception {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            return deptTypeService.deleteSchemaField(arguments, domain.getId());
        };
    }

    public DataFetcher saveSchemaField() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            DeptType deptType = JSON.parseObject(JSON.toJSONString(arguments.get("entity")), DeptType.class);
            DeptType data = deptTypeService.saveSchemaField(deptType, domain.getId());
            if (null != data) {
                return data;
            }
            throw new Exception("添加失败");
        };
    }

    public DataFetcher updateSchemaField() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            DeptType deptType = JSON.parseObject(JSON.toJSONString(arguments.get("entity")), DeptType.class);
            deptType.setDomain(domain.getId());
            DeptType data = deptTypeService.updateSchemaField(deptType);
            if (null != data) {
                return data;
            }
            throw new Exception("修改失败");
        };
    }
}
