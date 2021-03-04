package com.qtgl.iga.dataFetcher;

import graphql.schema.DataFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeptTypeDataFetcher {

    public static Logger logger = LoggerFactory.getLogger(DeptTypeDataFetcher.class);





    public DataFetcher deptTypes() {
        return dataFetchingEvn -> {
            //1。更具token信息验证是否合法，并判断其租户

            //2。解析查询参数+租户进行  进行查询
            return null;
        };
    }
}
