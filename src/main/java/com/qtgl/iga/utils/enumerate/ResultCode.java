package com.qtgl.iga.utils.enumerate;

import lombok.Getter;

/**
 * 枚举异常对应模板
 */
@Getter
public enum ResultCode {
    SUCCESS(1000, "操作成功"),
    UNMOUNT(1001, "节点%s规则的:%s无法找到挂载节点"),
    NO_MOUNT(1002, "节点%s规则的 :%s没有挂载规则 "),
    NULL_MOUNT(1003, "配置节点为空"),
    ILLEGAL_MOUNT(1004, "%s节点(%s)中的挂载规则%s表达式非法,请检查"),
    ILLEGAL_EXCLUSION(1010, "%s节点(%s)中的排除规则%s表达式非法,请检查"),
    NO_EXCLUSION(1011, "节点%s规则:%s无法找到排除节点"),
    NULL_EXCLUSION(1012, "节点%s规则的:%s 排除规则为空"),
    MONITOR_ERROR(1020, "%s删除数量%s,超出监控设定"),
    NO_UPSTREAM_TYPE(1030, "对应拉取节点(%s)无权威源类型数据"),
//    CYCLE_ERROR(1040, "%s节点(%s(%s))中的数据%s(%s)与%s节点(%s(%s))中的数据%s(%s)循环依赖"),
    CYCLE_ERROR(1040, "节点<span style='color:#40c97c;font-weight: bold'>%s(%s)</span>与节点<span style='color:#40c97c;font-weight: bold'>%s(%s)</span> code循环依赖"),
//    REPEAT_ERROR(1041, "%s节点(%s(%s))中的数据%s(%s)与%s节点(%s(%s))中的数据%s(%s)重复"),
    REPEAT_ERROR(1041, "节点<span style='color:#40c97c;font-weight: bold'>%s(%s)</span>与节点<span style='color:#40c97c;font-weight: bold'>%s(%s)</span> code重复"),
    ILLEGAL_DATA(1050, "含非法数据(名称或CODE为空的数据),请检查"),
    //    POST_REQUEST_ERROR(1060, "post请求失败，url:%s;params:%s"),
    PERSON_ERROR(1070, "人员治理中:%s类型%s"),
    OCCUPY_ERROR(1071, "人员身份治理中类型:%s中%s"),
    EXPRESSION_ERROR(1080, "%s节点%s中的类型%s表达式异常"),
    GET_DATA_ERROR(1081, "graphql获取权威源类型%s数据失败:%s"),
    ADD_UPSTREAM_ERROR(1082, "添加权威源失败:%s"),
    UPDATE_UPSTREAM_ERROR(1083, "修改权威源%s失败"),
    REPEAT_UPSTREAM_ERROR(1084, "appCode 或 appName 不能重复,添加失败%s---%s"),
    INVALID_PARAMETER(500, "无效的参数"),
    FAILED(1100, "操作失败");
    private long code;
    private String message;

    private ResultCode(long code, String message) {
        this.code = code;
        this.message = message;
    }

    public long getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
