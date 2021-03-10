package com.qtgl.iga.dataFetcher;


import com.alibaba.fastjson.JSON;
import com.qtgl.iga.bo.DeptTreeType;
import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.service.DeptTreeTypeService;
import com.qtgl.iga.utils.CertifiedConnector;
import graphql.schema.DataFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DeptTreeTypeDataFetcher {

    public static Logger logger = LoggerFactory.getLogger(DeptTreeTypeDataFetcher.class);


    @Autowired
    DeptTreeTypeService deptTreeTypeService;


    public DataFetcher deptTreeTypes() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            //2。解析查询参数  进行查询
            return deptTreeTypeService.findAll(arguments, domain.getId());
        };
    }

    public DataFetcher deleteDeptTreeType() throws Exception {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            return deptTreeTypeService.deleteDeptTreeType(arguments, domain.getId());
        };
    }

    public DataFetcher saveDeptTreeType() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            DeptTreeType deptTreeType = JSON.parseObject(JSON.toJSONString(arguments.get("entity")), DeptTreeType.class);
            DeptTreeType data = deptTreeTypeService.saveDeptTreeType(deptTreeType, domain.getId());
            if (null != data) {
                return data;
            }
            throw new Exception("添加失败");
        };
    }

    public DataFetcher updateDeptTreeType() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户
            DomainInfo domain = CertifiedConnector.getDomain();
            // 获取传入参数
            Map<String, Object> arguments = dataFetchingEvn.getArguments();
            DeptTreeType deptTreeType = JSON.parseObject(JSON.toJSONString(arguments.get("entity")), DeptTreeType.class);
            deptTreeType.setDomain(domain.getId());
            DeptTreeType data = deptTreeTypeService.updateDeptTreeType(deptTreeType);
            if (null != data) {
                return data;
            }
            throw new Exception("修改失败");
        };
    }
}
