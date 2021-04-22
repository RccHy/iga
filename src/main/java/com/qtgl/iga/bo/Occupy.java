package com.qtgl.iga.bo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Occupy {

    //select identity_user.identity_id as userId,identity_user.user_id as occupyId,u.user_type as postCode,u.dept_code as deptCode,u.card_type as occupyCardType,u.card_no as occupyCardNo,del_mark as delMark,source,data_source as dataSource,create_time as createTime,update_time as updateTime
    // from identity_user left join user u on identity_user.user_id = u.id;


    private String personId;
    private String occupyId;
    private String postCode;
    private String deptCode;
    private String occupyCardType;
    private String occupyCardNo;
    private Integer delMark;
    private String source;
    private String dataSource;
    private LocalDateTime updateTime;
    private LocalDateTime createTime;

}
