package com.qtgl.iga.dataFetcher;

import com.qtgl.iga.service.DeptService;
import com.qtgl.iga.service.DomainInfoService;
import graphql.schema.DataFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DoMainInfoFetcher {

    public static Logger logger = LoggerFactory.getLogger(DoMainInfoFetcher.class);


    @Autowired
    DomainInfoService domainInfoService;


    public DataFetcher doMainInfos() {
        return dataFetchingEvn -> {


            return domainInfoService.findAll();
        };
    }
}
