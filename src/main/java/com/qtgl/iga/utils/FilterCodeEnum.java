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

    LIKE("like", "%"),

    IEQ("ieq", "悠方礼品卡"),

    IN("in", "微信支付"),

    NIN("nin", "支付宝"),

    GT("gt", "其他");



    private String code;

    private String desc;

//    public static String getCodeByDesc(String desc){
//        for(FilterCodeEnum codeEnum : FilterCodeEnum.values()){
//            if(codeEnum.getDesc().equals(desc)){
//                return codeEnum.getCode();
//            }else {
//                return null;
//            }
//        }
//    }
}
