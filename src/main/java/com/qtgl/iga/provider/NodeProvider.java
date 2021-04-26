package com.qtgl.iga.provider;

import com.qtgl.iga.config.GraphQLConfig;
import com.qtgl.iga.dataFetcher.NodeDataFetcher;
import graphql.schema.idl.TypeRuntimeWiring;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;

@Component
public class NodeProvider {


    @Autowired
    NodeDataFetcher dataFetcher;

    public TypeRuntimeWiring.Builder buildQueryRuntimeWiring() {

        return newTypeWiring("Query")
                .dataFetcher("nodes", dataFetcher.findNodes())
                .dataFetcher("nodesPlus", dataFetcher.findNodesPlus());
    }


    public TypeRuntimeWiring.Builder buildMutationRuntimeWiring() throws Exception {
        return newTypeWiring("Mutation")
                .dataFetcher("saveNode", dataFetcher.saveNode())
                .dataFetcher("deleteNode", dataFetcher.deleteNode())
                .dataFetcher("updateNode", dataFetcher.updateNode())
                .dataFetcher("applyNode", dataFetcher.applyNode())
                .dataFetcher("rollbackNode", dataFetcher.rollbackNode());

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

