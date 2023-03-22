package com.qtgl.iga.bean;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MergeAttrRule {

    private String id;
    private String attrName;
    private String entityId;
    private String fromEntityId;
    private String dynamicAttrId;

    private LocalDateTime createTime;

}
