package com.qtgl.iga.utils;

import graphql.ErrorClassification;
import graphql.GraphQLError;
import graphql.language.SourceLocation;

import java.util.List;
import java.util.Map;

/**
 * <FileName> GraphqlError
 * <Desc> graphql异常类
 **/
public class GraphqlError  implements GraphQLError  {
    private  List<SourceLocation> locations;
    private  List<Object> absolutePath;
    private  String message;
    private  ErrorClassification errorType;
    private  Map<String, Object> extensions;

    public GraphqlError(List<SourceLocation> locations, List<Object> absolutePath, String message, ErrorClassification errorType, Map<String, Object> extensions) {
        this.locations = locations;
        this.absolutePath = absolutePath;
        this.message = message;
        this.errorType = errorType;
        this.extensions = extensions;
    }

    public GraphqlError(String message) {

        this.message = message;

    }

    @Override
    public String getMessage() {
        return this.message;
    }

    @Override
    public List<SourceLocation> getLocations() {
        return this.locations;
    }

    @Override
    public ErrorClassification getErrorType() {
        return this.errorType;
    }
}
