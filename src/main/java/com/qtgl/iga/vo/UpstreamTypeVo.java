package com.qtgl.iga.vo;

import com.qtgl.iga.bo.*;
import lombok.Data;

import java.sql.Timestamp;
import java.util.List;

/**
 * <FileName> UpstreamTypeVo
 * <Desc> 上游源类型表
 **/
@Data
public class UpstreamTypeVo extends UpstreamType {


    //上游源
    private Upstream upstream;



    //属组织机构类别
    private DeptType deptType;

    //属组织机构类别树
    private DeptTreeType deptTreeType;



}
