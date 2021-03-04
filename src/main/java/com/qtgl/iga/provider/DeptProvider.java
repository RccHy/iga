package com.qtgl.iga.provider;

import com.qtgl.iga.config.GraphQLConfig;
import com.qtgl.iga.dataFetcher.DeptDataFetcher;
import graphql.schema.idl.TypeRuntimeWiring;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;

import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;

public class DeptProvider {



    @Autowired
    DeptDataFetcher dataFetcher;

    public TypeRuntimeWiring.Builder buildQueryRuntimeWiring() {
        TypeRuntimeWiring.Builder builder = newTypeWiring("Query")
                .dataFetcher("depts", dataFetcher.depts());
        return builder;
    }


    public TypeRuntimeWiring.Builder buildMutationRuntimeWiring() {
        TypeRuntimeWiring.Builder builder = newTypeWiring("Mutation");
        return builder;

    }

    @Autowired
    private GraphQLConfig graphQLConfig;
    @PostConstruct
    private void init() {
        String key=this.getClass().getName();
        graphQLConfig.builderConcurrentMap.put(key+"-Query", buildQueryRuntimeWiring());
        graphQLConfig.builderConcurrentMap.put(key+"-Mutation", buildMutationRuntimeWiring());
    }
}

