package com.qtgl.iga.provider;


import com.qtgl.iga.config.GraphQLConfig;
import com.qtgl.iga.dataFetcher.MergeAttrRuleFetcher;
import graphql.schema.idl.TypeRuntimeWiring;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;

@Component
public class MergeAttrRuleProvider {


    @Resource
    MergeAttrRuleFetcher mergeAttrRuleFetcher;

    public TypeRuntimeWiring.Builder buildQueryRuntimeWiring() {
        return newTypeWiring("Query")
                .dataFetcher("mergeAttrRules", mergeAttrRuleFetcher.mergeAttrRules());

    }

    public TypeRuntimeWiring.Builder buildMutationRuntimeWiring() {
        return newTypeWiring("Mutation")
                .dataFetcher("saveMergeAttrRule", mergeAttrRuleFetcher.saveMergeAttrRule())
                .dataFetcher("deleteMergeAttrRule", mergeAttrRuleFetcher.deleteMergeAttrRule());

    }

    @Resource
    private GraphQLConfig graphQLConfig;

    @PostConstruct
    private void init() {
        String key = this.getClass().getName();
        graphQLConfig.builderConcurrentMap.put(key + "-Query", buildQueryRuntimeWiring());
        graphQLConfig.builderConcurrentMap.put(key + "-Mutation", buildMutationRuntimeWiring());
    }
}
