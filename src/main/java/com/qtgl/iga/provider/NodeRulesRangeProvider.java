package com.qtgl.iga.provider;


import com.qtgl.iga.config.GraphQLConfig;
import com.qtgl.iga.dataFetcher.NodeRulesRangeDataFetcher;
import graphql.schema.idl.TypeRuntimeWiring;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;

@Component
public class NodeRulesRangeProvider {


    @Autowired
    NodeRulesRangeDataFetcher dataFetcher;

    public TypeRuntimeWiring.Builder buildQueryRuntimeWiring() {
        return newTypeWiring("Query")
                .dataFetcher("nodeRulesRanges", dataFetcher.findNodeRulesRange());
    }


    public TypeRuntimeWiring.Builder buildMutationRuntimeWiring() {
        return newTypeWiring("Mutation")
                .dataFetcher("saveNodeRulesRange", dataFetcher.saveNodeRulesRange())
                .dataFetcher("deleteNodeRulesRange", dataFetcher.deleteNodeRulesRange())
                .dataFetcher("updateNodeRulesRange", dataFetcher.updateNodeRulesRange());

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

