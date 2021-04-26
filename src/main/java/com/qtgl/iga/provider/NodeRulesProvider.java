package com.qtgl.iga.provider;

import com.qtgl.iga.config.GraphQLConfig;
import com.qtgl.iga.dataFetcher.NodeRulesDataFetcher;
import graphql.schema.idl.TypeRuntimeWiring;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;

@Component
public class NodeRulesProvider {


    @Autowired
    NodeRulesDataFetcher dataFetcher;

    public TypeRuntimeWiring.Builder buildQueryRuntimeWiring() {
        return newTypeWiring("Query")
                .dataFetcher("nodeRules", dataFetcher.findNodeRules());
    }


    public TypeRuntimeWiring.Builder buildMutationRuntimeWiring() {
        return newTypeWiring("Mutation")
                .dataFetcher("saveRules", dataFetcher.saveRules())
                .dataFetcher("deleteRules", dataFetcher.deleteRules())
                .dataFetcher("updateRules", dataFetcher.updateRules())
                .dataFetcher("deleteBatchRules", dataFetcher.deleteBatchRules());

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

