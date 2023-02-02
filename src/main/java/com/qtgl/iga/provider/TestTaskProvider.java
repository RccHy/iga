package com.qtgl.iga.provider;

import com.qtgl.iga.config.GraphQLConfig;
import com.qtgl.iga.dataFetcher.TestTaskFetcher;
import graphql.schema.idl.TypeRuntimeWiring;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;

@Component
public class TestTaskProvider {


    @Autowired
    TestTaskFetcher dataFetcher;

    public TypeRuntimeWiring.Builder buildQueryRuntimeWiring() {
        return newTypeWiring("Query")
                .dataFetcher("igaUsers", dataFetcher.igaUsers())
                .dataFetcher("igaPersons", dataFetcher.igaPersons())
                .dataFetcher("igaPosts", dataFetcher.igaPosts())
                .dataFetcher("igaDepartments", dataFetcher.igaDepartments());
    }


    public TypeRuntimeWiring.Builder buildMutationRuntimeWiring() throws Exception {
        return newTypeWiring("Mutation")
                .dataFetcher("testPersonTask", dataFetcher.testPersonTask())
                .dataFetcher("testUserTask", dataFetcher.testUserTask())
                .dataFetcher("testDeptTask", dataFetcher.testDeptTask())
                .dataFetcher("testUserTypeTask", dataFetcher.testUserTypeTask());

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

