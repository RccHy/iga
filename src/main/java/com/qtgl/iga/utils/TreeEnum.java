package com.qtgl.iga.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <FileName> FilterCodeEnum
 * <Desc> 匹配关系枚举类
 **/
@Getter
@AllArgsConstructor
public enum TreeEnum {


    CODE("code", "代码"),

    NAME("name", "名称"),

    PARENTCODE("parentId", "父级"),
    SOURCE("source","来源");


    private String code;

    private String desc;

    public static String getDescByCode(String code) {
        for (TreeEnum codeEnum : TreeEnum.values()) {
            if (codeEnum.getCode().equals(code)) {
                return codeEnum.getDesc();
            }
        }
        return null;
    }
}
