package com.qtgl.iga.utils.enums;


import jdk.internal.org.objectweb.asm.tree.JumpInsnNode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor

public enum DataStatusEnum {

    // 错误数据
    ERROR_DATA("error_data", "0"),
    // 自动合重
    AUTO_MERGE("auto_merge", "1"),
    // 手动合重
    MANUAL_MERGE("manual_merge", "2"),
    // 新增
    NEW("new", "3"),
    // 修改
    UPDATE("update", "4"),

    // 上游直接提供的删除数据
    DELETE("del", "4"),
    // 规则未启用，跳过
    RULE_DISABLE("RULE_DISABLE", "5"),
    // 无变化
    NO_CHANGE("no_change", "6");
    //DELETE("delete", "5");

    private String code;

    private String desc;

    DataStatusEnum(){}

    public static Integer getDescByCode(String code) {
        for (FilterCodeEnum codeEnum : FilterCodeEnum.values()) {
            if (codeEnum.getCode().equals(code)) {
                return Integer.valueOf(codeEnum.getDesc());
            }
        }
        return null;
    }
}
