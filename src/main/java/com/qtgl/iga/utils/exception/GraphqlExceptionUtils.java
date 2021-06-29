package com.qtgl.iga.utils.exception;

import com.qtgl.iga.utils.GraphqlError;
import com.qtgl.iga.utils.exception.CustomException;
import graphql.execution.DataFetcherResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GraphqlExceptionUtils {

    public static Object getObject(String message, CustomException e) {
        HashMap<String, Object> map = new HashMap<>();
        map.put("code", e.getCode());
        map.put("prompt", e.getErrorMsg());
        map.put("detail", e.getMachineMsg());
        HashMap<String, Object> map1 = new HashMap<>();
        map1.put("stacktrace", e.getStackTrace());
        map.put("exception", map1);

        List<GraphqlError> errorsList = new ArrayList<>();

        errorsList.add(new GraphqlError(message, map));

        return new DataFetcherResult(e.getData(), errorsList);
    }
}
