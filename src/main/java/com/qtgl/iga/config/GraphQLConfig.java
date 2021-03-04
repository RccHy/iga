package com.qtgl.iga.config;

import com.qtgl.iga.utils.GraphqlScalarType;
import graphql.GraphQL;
import graphql.TypeResolutionEnvironment;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.TypeResolver;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import graphql.schema.idl.TypeRuntimeWiring.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static graphql.schema.idl.TypeRuntimeWiring.newTypeWiring;

@Configuration
public class GraphQLConfig {

    public static Logger logger = LoggerFactory.getLogger(GraphQLConfig.class);

    public ConcurrentMap<String, Builder> builderConcurrentMap = new ConcurrentHashMap<>();

    @Bean
    public GraphQL graphQL() {
        return GraphQL.newGraphQL(graphQLSchema()).build();
    }


    public GraphQLSchema graphQLSchema() {
        SchemaGenerator schemaGenerator = new SchemaGenerator();
        TypeDefinitionRegistry registry = null;
        try {
            registry = typeDefinitionRegistry();
        } catch (IOException e) {
            e.printStackTrace();
        }
        GraphQLSchema graphQLSchema = schemaGenerator.makeExecutableSchema(registry, runtimeWiring());

        return graphQLSchema;
    }


    @Bean/*(name = "builtinRunTime")*/
    public RuntimeWiring runtimeWiring() {
        RuntimeWiring.Builder builder = RuntimeWiring.newRuntimeWiring();
        builder.scalar(GraphqlScalarType.GraphQLUnixTime)
                .scalar(GraphqlScalarType.GraphQLTimestamp)
                .type(newTypeWiring("Node").typeResolver(getNodeTypeResolver()).build())
                .type(newTypeWiring("Edge").typeResolver(getEdgeTypeResolver()).build())
                .type(newTypeWiring("PublicConnection").typeResolver(getConnectionTypeResolver()).build());
        if (this.builderConcurrentMap != null && this.builderConcurrentMap.size() > 0) {
            for (Map.Entry<String, Builder> entry : this.builderConcurrentMap.entrySet()) {
                builder.type(entry.getValue());
            }
        }
        return builder.build();
    }

    //schema 类型注册 主要是用来整合各个schema文件路径
    public TypeDefinitionRegistry typeDefinitionRegistry() throws IOException {
        SchemaParser schemaParser = new SchemaParser();
        TypeDefinitionRegistry typeRegistry = new TypeDefinitionRegistry();

        //获取所有的graphqls文件进行加载
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        Resource[] resources = resolver.getResources("classpath*:graphql/iga/*.graphqls");
        File file = resolver.getResource("classpath:graphql/schema.graphqls").getFile();
        logger.info("resources length:" + resources.length);

        if (resources != null && resources.length > 0) {
            typeRegistry.merge(schemaParser.parse(file));
            for (Resource resource : resources) {
                typeRegistry.merge(schemaParser.parse(resource.getFile()));
            }

        }
        return typeRegistry;
    }


    public TypeResolver getNodeTypeResolver() {
        return new TypeResolver() {
            @Override
            public GraphQLObjectType getType(TypeResolutionEnvironment env) {
                return null;
            }
        };
    }

    public TypeResolver getEdgeTypeResolver() {
        return new TypeResolver() {
            @Override
            public GraphQLObjectType getType(TypeResolutionEnvironment env) {
                return null;
            }
        };
    }

    public TypeResolver getConnectionTypeResolver() {
        return new TypeResolver() {
            @Override
            public GraphQLObjectType getType(TypeResolutionEnvironment env) {
                return null;
            }
        };
    }


}
