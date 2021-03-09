package com.qtgl.iga.provider;

import com.qtgl.iga.config.GraphQLConfig;
import com.qtgl.iga.dataFetcher.UpStreamFetcher;
import com.qtgl.iga.dataFetcher.UpStreamTypeFetcher;
import graphql.schema.idl.TypeRuntimeWiring;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;

/**
 * <FileName> UpStreamTypeProvider
 * <Desc> 上游源类型
 **/

@Component
public class UpStreamTypeProvider {


    @Autowired
    UpStreamTypeFetcher upStreamTypeFetcher;

    public TypeRuntimeWiring.Builder buildQueryRuntimeWiring() {
        TypeRuntimeWiring.Builder builder = newTypeWiring("Query")
                .dataFetcher("upStreamTypes", upStreamTypeFetcher.upStreamTypes());
        return builder;
    }


    public TypeRuntimeWiring.Builder buildMutationRuntimeWiring() throws Exception {
        TypeRuntimeWiring.Builder builder = newTypeWiring("Mutation")
                .dataFetcher("saveUpStreamType", upStreamTypeFetcher.saveUpStreamType())
                .dataFetcher("deleteUpStreamType", upStreamTypeFetcher.deleteUpStreamType())
                .dataFetcher("updateUpStreamType", upStreamTypeFetcher.updateUpStreamType());
        return builder;

    }

    @Autowired
    private GraphQLConfig graphQLConfig;

    @PostConstruct
    private void init() throws Exception {
        String key = this.getClass().getName();
        graphQLConfig.builderConcurrentMap.put(key + "-Query", buildQueryRuntimeWiring());
        graphQLConfig.builderConcurrentMap.put(key + "-Mutation", buildMutationRuntimeWiring());
    }
}
