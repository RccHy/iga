package com.qtgl.iga.bo;

import lombok.Data;
import java.util.Date;


@Data
public class DomainInfo {

    private String id;
    private String domainId;
    private String domainName;
    private String clientId;
    private String clientSecret;
    private Integer status;
    private Date  createTime;
    private Date updateTime;


}
