package com.qtgl.iga.bo;


import lombok.Data;

import java.io.Serializable;

@Data
public class DynamicValue implements Serializable,Cloneable {

    private String id;

    private String entityId;

    private String attrId;

    private String value;

    private String tenantId;

    private String key;

    private String code;

    @Override
    public Object clone()  {
        DynamicValue dynamicValue= null;
        try {
            dynamicValue = (DynamicValue) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return dynamicValue;
    }
}
