package com.qtgl.iga.utils;

import com.qtgl.iga.utils.enumerate.ResultCode;
import lombok.Data;

import java.io.Serializable;

@Data
public class ApiResult<T> implements Serializable {

    /**
     * 返回码
     */
    private long code;

    /**
     * 返回码描述(人读msg)
     */
    private String msg;

    /**
     * 服务端返回的具体结果数据
     */
    private T data;

    /**
     * 机读msg
     */
    private String machineMsg;

    protected ApiResult() {
    }

    protected ApiResult(long code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public static ApiResult success() {
        return new ApiResult(ResultCode.SUCCESS.getCode(), "", null);
    }

    public static ApiResult success(String msg) {
        return new ApiResult(ResultCode.SUCCESS.getCode(), msg, null);
    }

    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<T>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    public static <T> ApiResult<T> success(T data, String msg) {
        return new ApiResult<T>(ResultCode.SUCCESS.getCode(), msg, data);
    }


    public static <T> ApiResult<T> failed(String msg) {
        return new ApiResult<T>(ResultCode.FAILED.getCode(), msg, null);
    }

    public static <T> ApiResult<T> failed(long errorCode, String msg) {
        return new ApiResult<T>(errorCode, msg, null);
    }


    public static <T> ApiResult<T> failed(ResultCode errorCode) {
        return new ApiResult<T>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    public static <T> ApiResult<T> failed() {
        return failed(ResultCode.FAILED); //ResultCode 枚举操作码
    }

    public static <T> ApiResult<T> failed(String message, String... args) {
        return failed(ResultCode.FAILED.getCode(), String.format(message, args));
    }


}
