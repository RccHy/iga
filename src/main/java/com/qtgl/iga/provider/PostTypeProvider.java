package com.qtgl.iga.provider;

import com.qtgl.iga.config.GraphQLConfig;
import com.qtgl.iga.dataFetcher.PostTypeDataFetcher;
import graphql.schema.idl.TypeRuntimeWiring;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;

@Component
public class PostTypeProvider {


    @Autowired
    PostTypeDataFetcher dataFetcher;

    public TypeRuntimeWiring.Builder buildQueryRuntimeWiring() {
        TypeRuntimeWiring.Builder builder = newTypeWiring("Query")
                .dataFetcher("postTypes", dataFetcher.postTypes());
        return builder;
    }


    public TypeRuntimeWiring.Builder buildMutationRuntimeWiring() throws Exception {
        TypeRuntimeWiring.Builder builder = newTypeWiring("Mutation")
                .dataFetcher("savePostType", dataFetcher.savePostType())
                .dataFetcher("deletePostType", dataFetcher.deletePostType())
                .dataFetcher("updatePostType", dataFetcher.updatePostType());
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

