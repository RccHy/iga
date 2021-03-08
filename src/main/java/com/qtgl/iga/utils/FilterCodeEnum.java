package com.qtgl.iga.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * <FileName> FilterCodeEnum
 * <Desc> 匹配关系枚举类
 **/
@Getter
@AllArgsConstructor
public enum FilterCodeEnum {

    EQ("eq", "="),

    NEQ("neq", "!="),

    LIKE("like", "like"),

    IEQ("ieq", "ieq"),

    IN("in", "in"),

    NIN("nin", "not in"),

    GT("gt", ">"),

    LT("lt", "<"),

    GTE("gte", ">="),

    LTE("lte", "<=");


    private String code;

    private String desc;

    public static String getDescByCode(String code){
        for(FilterCodeEnum codeEnum : FilterCodeEnum.values()){
            if(codeEnum.getCode().equals(code)){
                return codeEnum.getDesc();
            }
        }
        return null;
    }
}
