package com.qtgl.iga.provider;

import com.qtgl.iga.config.GraphQLConfig;
import com.qtgl.iga.dataFetcher.UpstreamFetcher;
import graphql.schema.idl.TypeRuntimeWiring;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;

/**
 * <FileName> UpStreamProvider
 * <Desc> 上游源注册信息
 **/

@Component
public class UpstreamProvider {


    @Autowired
    UpstreamFetcher upstreamFetcher;

    public TypeRuntimeWiring.Builder buildQueryRuntimeWiring() {
        TypeRuntimeWiring.Builder builder = newTypeWiring("Query")
                .dataFetcher("upstreams", upstreamFetcher.upstreams())
                .dataFetcher("upstreamsAndTypes", upstreamFetcher.upstreamsAndTypes());
        return builder;
    }


    public TypeRuntimeWiring.Builder buildMutationRuntimeWiring() throws Exception {
        TypeRuntimeWiring.Builder builder = newTypeWiring("Mutation")
                .dataFetcher("saveUpstream", upstreamFetcher.saveUpstream())
                .dataFetcher("deleteUpstream", upstreamFetcher.deleteUpstream())
                .dataFetcher("updateUpstream", upstreamFetcher.updateUpstream())
                .dataFetcher("saveUpstreamAndTypes", upstreamFetcher.saveUpstreamAndTypes());
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
