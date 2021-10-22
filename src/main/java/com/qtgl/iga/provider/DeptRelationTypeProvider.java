package com.qtgl.iga.provider;

import com.qtgl.iga.bo.DeptRelationType;
import com.qtgl.iga.config.GraphQLConfig;
import com.qtgl.iga.dataFetcher.DeptRelationTypeFetcher;
import com.qtgl.iga.dataFetcher.DeptTypeDataFetcher;
import graphql.schema.idl.TypeRuntimeWiring;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;

@Component
public class DeptRelationTypeProvider {


    @Autowired
    DeptRelationTypeFetcher deptRelationTypeFetcher;

    public TypeRuntimeWiring.Builder buildQueryRuntimeWiring() {
        return newTypeWiring("Query")
                .dataFetcher("deptRelationTypes", deptRelationTypeFetcher.findDeptRelationType());
    }



    @Autowired
    private GraphQLConfig graphQLConfig;

    @PostConstruct
    private void init() throws Exception {
        String key = this.getClass().getName();
        graphQLConfig.builderConcurrentMap.put(key + "-Query", buildQueryRuntimeWiring());
    }
}

