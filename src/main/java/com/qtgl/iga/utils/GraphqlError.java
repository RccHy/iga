package com.qtgl.iga.utils;

import com.qtgl.iga.bean.ErrorData;
import graphql.ErrorClassification;
import graphql.GraphQLError;
import graphql.language.SourceLocation;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * <FileName> GraphqlError
 * <Desc> graphql异常类
 **/
@Data
public class GraphqlError implements GraphQLError {
    private List<SourceLocation> locations;
    private List<Object> absolutePath;
    private String message;
    private ErrorClassification errorType;
    private Map<String, Object> extensions;
    private List<ErrorData> errorData;

    private String detail;

    public GraphqlError(List<SourceLocation> locations, List<Object> absolutePath, String message, ErrorClassification errorType, Map<String, Object> extensions) {
        this.locations = locations;
        this.absolutePath = absolutePath;
        this.message = message;
        this.errorType = errorType;
        this.extensions = extensions;
    }

    public GraphqlError(String message, List<ErrorData> errorData) {

        this.message = message;
        this.errorData = errorData;

    }

    public GraphqlError(String message, String detail) {
        this.message = message;
        this.detail = detail;
    }

    public GraphqlError(String message, Map<String, Object> extensions) {
        this.message = message;
        this.extensions = extensions;

    }
//    public GraphqlError(String message, String extensions) {
//        this.message = message;
//        this.extensions = extensions;
//
//    }

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
