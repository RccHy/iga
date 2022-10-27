package com.qtgl.iga.utils.exception;


import com.qtgl.iga.bean.ErrorData;
import com.qtgl.iga.utils.enumerate.ResultCode;
import lombok.Data;

import java.util.List;

@Data
public class CustomException extends RuntimeException {
    /**
     * 友好地错误信息
     */
    private String errorMsg;
    /**
     * 错误代码
     */
    private long code = 1100;
    /**
     * 详尽的技术信息
     */
    private List<ErrorData> machineMsg;
    /**
     * 数据
     */
    private Object data;


    public CustomException(ResultCode resultCode, List<ErrorData> machineMsg, Object data, String... args) {
        super(resultCode.getMessage());
        System.out.println(resultCode.getMessage());
        this.errorMsg = String.format(resultCode.getMessage(), args);
        System.out.println(errorMsg);
        this.code = resultCode.getCode();
        this.machineMsg = machineMsg;
        this.data = data;
    }

    public CustomException(ResultCode resultCode, List<ErrorData> machineMsg, Object data) {
        super(resultCode.getMessage());
        this.errorMsg = resultCode.getMessage();
        this.code = resultCode.getCode();
        this.machineMsg = machineMsg;
        this.data = data;
    }


//    @Override
//    public Throwable fillInStackTrace() {
//        return this;
//    }

    public CustomException(ResultCode resultCode, String message) {
        super(resultCode.getMessage());
        this.errorMsg = message;
        this.code = resultCode.getCode();
    }

    public CustomException(ResultCode resultCode, String message, Object data) {
        super(resultCode.getMessage());
        this.errorMsg = message;
        this.code = resultCode.getCode();
        this.data = data;
    }


}
