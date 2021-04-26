package com.qtgl.iga.bean;

import lombok.Data;

/**
 * <FileName> ErrorData
 * <Desc> 错误数据容器
 **/
@Data
public class ErrorData {

    private String deptTreeTypeId;

    private String nodeRulesId;

    private String code;

    public ErrorData() {
    }

    public ErrorData(String deptTreeTypeId, String nodeRulesId, String code) {
        this.deptTreeTypeId = deptTreeTypeId;
        this.nodeRulesId = nodeRulesId;
        this.code = code;
    }
}
