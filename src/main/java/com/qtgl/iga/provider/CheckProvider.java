package com.qtgl.iga.provider;


import com.qtgl.iga.config.GraphQLConfig;
import com.qtgl.iga.dataFetcher.CheckFetcher;
import com.qtgl.iga.dataFetcher.DeptDataFetcher;
import graphql.schema.idl.TypeRuntimeWiring;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;

@Component
public class CheckProvider {


    @Autowired
    CheckFetcher checkFetcher;

    public TypeRuntimeWiring.Builder buildQueryRuntimeWiring() {
        return newTypeWiring("Query")
                .dataFetcher("checkList", checkFetcher.checkList());

    }


    @Autowired
    private GraphQLConfig graphQLConfig;

    @PostConstruct
    private void init() {
        String key = this.getClass().getName();
        graphQLConfig.builderConcurrentMap.put(key + "-Query", buildQueryRuntimeWiring());
    }
}
