package com.qtgl.iga.provider;

import com.qtgl.iga.config.GraphQLConfig;
import com.qtgl.iga.dataFetcher.DeptTypeDataFetcher;
import com.qtgl.iga.dataFetcher.UpStreamFetcher;
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
public class UpStreamProvider {


    @Autowired
    UpStreamFetcher upStreamFetcher;

    public TypeRuntimeWiring.Builder buildQueryRuntimeWiring() {
        TypeRuntimeWiring.Builder builder = newTypeWiring("Query")
                .dataFetcher("upStreams", upStreamFetcher.upStreams());
        return builder;
    }


    public TypeRuntimeWiring.Builder buildMutationRuntimeWiring() throws Exception {
        TypeRuntimeWiring.Builder builder = newTypeWiring("Mutation")
                .dataFetcher("saveUpStream", upStreamFetcher.saveUpStream())
                .dataFetcher("deleteUpStream", upStreamFetcher.deleteUpStream())
                .dataFetcher("updateUpStream", upStreamFetcher.updateUpStream());
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
