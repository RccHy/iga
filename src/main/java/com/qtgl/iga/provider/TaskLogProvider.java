package com.qtgl.iga.provider;


import com.qtgl.iga.config.GraphQLConfig;
import com.qtgl.iga.dataFetcher.TaskLogDataFetcher;
import graphql.schema.idl.TypeRuntimeWiring;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;

@Component
public class TaskLogProvider {


    @Autowired
    TaskLogDataFetcher dataFetcher;

    public TypeRuntimeWiring.Builder buildQueryRuntimeWiring() {
        return newTypeWiring("Query")
                .dataFetcher("taskLogs", dataFetcher.taskLogs());
    }


    public TypeRuntimeWiring.Builder buildMutationRuntimeWiring() {
        return newTypeWiring("Mutation")
                .dataFetcher("markLogs", dataFetcher.markLogs());

    }

    @Autowired
    private GraphQLConfig graphQLConfig;

    @PostConstruct
    private void init() {
        String key = this.getClass().getName();
        graphQLConfig.builderConcurrentMap.put(key + "-Query", buildQueryRuntimeWiring());
        graphQLConfig.builderConcurrentMap.put(key + "-Mutation", buildMutationRuntimeWiring());
    }
}

