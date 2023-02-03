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
                .dataFetcher("igaOccupy", dataFetcher.igaUsers())
                .dataFetcher("igaUser", dataFetcher.igaPersons())
                .dataFetcher("igaPost", dataFetcher.igaPosts())
                .dataFetcher("igaDepartment", dataFetcher.igaDepartments());
    }


    public TypeRuntimeWiring.Builder buildMutationRuntimeWiring() throws Exception {
        return newTypeWiring("Mutation")
                .dataFetcher("testUserTask", dataFetcher.testPersonTask())
                .dataFetcher("testOccupyTask", dataFetcher.testUserTask())
                .dataFetcher("testDeptTask", dataFetcher.testDeptTask())
                .dataFetcher("testPostTask", dataFetcher.testUserTypeTask());

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

