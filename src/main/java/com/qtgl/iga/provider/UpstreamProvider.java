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
        return newTypeWiring("Query")
                .dataFetcher("upstreams", upstreamFetcher.upstreams())
                .dataFetcher("upstreamsAndTypes", upstreamFetcher.upstreamsAndTypes());
    }


    public TypeRuntimeWiring.Builder buildMutationRuntimeWiring() throws Exception {
        return newTypeWiring("Mutation")
                .dataFetcher("saveUpstream", upstreamFetcher.saveUpstream())
                .dataFetcher("deleteUpstream", upstreamFetcher.deleteUpstream())
                .dataFetcher("updateUpstream", upstreamFetcher.updateUpstream())
                .dataFetcher("saveUpstreamAndTypes", upstreamFetcher.saveUpstreamAndTypes())
                .dataFetcher("updateUpstreamAndTypes", upstreamFetcher.updateUpstreamAndTypes());

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
