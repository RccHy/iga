package com.qtgl.iga.bo;


import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class Config implements Serializable {


    private String id;
    private String tenantId;
    private String name;
    private String pluginName;
    private String config;
    private String status;
    private Boolean delMark;
    private LocalDateTime updateTime;
    private LocalDateTime createTime;

}
