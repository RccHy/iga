package com.qtgl.iga.provider;

import com.qtgl.iga.config.GraphQLConfig;
import com.qtgl.iga.dataFetcher.UpstreamTypeFetcher;
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
public class UpstreamTypeProvider {


    @Autowired
    UpstreamTypeFetcher upstreamTypeFetcher;

    public TypeRuntimeWiring.Builder buildQueryRuntimeWiring() {
        return newTypeWiring("Query")
                .dataFetcher("upstreamTypes", upstreamTypeFetcher.upstreamTypes())
                .dataFetcher("upstreamTypesData", upstreamTypeFetcher.upstreamTypesData());
    }


    public TypeRuntimeWiring.Builder buildMutationRuntimeWiring() throws Exception {
        return newTypeWiring("Mutation")
                .dataFetcher("saveUpstreamType", upstreamTypeFetcher.saveUpstreamType())
                .dataFetcher("deleteUpstreamType", upstreamTypeFetcher.deleteUpstreamType())
                .dataFetcher("updateUpstreamType", upstreamTypeFetcher.updateUpstreamType());

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
