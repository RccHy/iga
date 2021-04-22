package com.qtgl.iga.provider;

import com.qtgl.iga.config.GraphQLConfig;
import com.qtgl.iga.dataFetcher.DeptDataFetcher;
import graphql.schema.idl.TypeRuntimeWiring;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;

@Component
public class DeptProvider {


    @Autowired
    DeptDataFetcher dataFetcher;

    public TypeRuntimeWiring.Builder buildQueryRuntimeWiring() {
        TypeRuntimeWiring.Builder builder = newTypeWiring("Query")
                .dataFetcher("depts", dataFetcher.findDept())
                .dataFetcher("posts", dataFetcher.findPosts())
                .dataFetcher("persons", dataFetcher.findPersons())
                .dataFetcher("occupies", dataFetcher.findOccupies());
        return builder;
    }


    @Autowired
    private GraphQLConfig graphQLConfig;

    @PostConstruct
    private void init() {
        String key = this.getClass().getName();
        graphQLConfig.builderConcurrentMap.put(key + "-Query", buildQueryRuntimeWiring());
    }
}

