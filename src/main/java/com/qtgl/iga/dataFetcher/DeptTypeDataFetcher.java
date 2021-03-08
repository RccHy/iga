package com.qtgl.iga.dataFetcher;


import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.service.DeptTypeService;
import com.qtgl.iga.utils.CertifiedConnector;
import graphql.schema.DataFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Iterator;
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
            Iterator<Map.Entry<String, Object>> it = arguments.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, Object> entry = it.next();
                System.out.println("key = " + entry.getKey() + ", value = " + entry.getValue());
            }
            return deptTypeService.getAllDeptTypes();
        };
    }
}
