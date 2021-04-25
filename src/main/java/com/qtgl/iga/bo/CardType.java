package com.qtgl.iga.bo;


import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CardType {


    private String id;
    private String cardTypeName;
    private String cardTypeCode;
    private String cardTypeReg;
    private String tenantId;
    private LocalDateTime updateTime;
    private LocalDateTime createTime;

}
