package com.qtgl.iga.bo;


import lombok.Data;

import java.sql.Timestamp;

/**
 * 节点规则
 */
@Data
public class Node {

    private String id;

    //是否继承父级规则
    private Boolean inherit;

    //是否允许手工
    private Boolean manual;

    //节点代码
    private String nodeCode;

    //创建时间
    private Long createTime;

    //修改时间
    private Long updateTime;

    //
    private String domain;

}
