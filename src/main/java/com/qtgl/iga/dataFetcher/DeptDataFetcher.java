package com.qtgl.iga.dataFetcher;

import com.qtgl.iga.service.DeptService;
import graphql.schema.DataFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class DeptDataFetcher {

    public static Logger logger = LoggerFactory.getLogger(DeptDataFetcher.class);


    @Autowired
    DeptService deptService;


    public DataFetcher depts() {
        return dataFetchingEvn -> {

            System.out.println("123123");
            return deptService.getAllDepts();
        };
    }
}
