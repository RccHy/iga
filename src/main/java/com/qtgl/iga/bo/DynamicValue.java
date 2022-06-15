package com.qtgl.iga.bo;


import lombok.Data;

import java.io.Serializable;

@Data
public class DynamicValue implements Serializable {

    private String id;

    private String entityId;

    private String attrId;

    private String value;

    private String tenantId;

    private String key;

    private String code;

}
