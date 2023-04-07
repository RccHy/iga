package com.qtgl.iga.bo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ShadowCopy {

    private String id;
    private byte[] data;
    private String upstreamTypeId;
    private String type;
    private String domain;

    private LocalDateTime createTime;
}
