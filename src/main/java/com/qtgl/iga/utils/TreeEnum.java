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

    PARENT_CODE("parentCode", "父级"),

    ACTIVE("active", "是否有效"),

    DEL_MARK("delMark", "是否删除"),

    SOURCE("source", "来源"),

    CREATE_TIME("createTime", "创建时间"),

    POST_TYPE("type", "岗位代码"),

    UPDATE_TIME("updateTime", "修改时间");


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
