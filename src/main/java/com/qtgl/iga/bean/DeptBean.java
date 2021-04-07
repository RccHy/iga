package com.qtgl.iga.bean;


import com.qtgl.iga.bo.Dept;
import lombok.Data;


import java.sql.Timestamp;
import java.time.LocalDateTime;

@Data
public class DeptBean {

    private String code;

    private String name;

    private String parentCode;

    private Integer independent;

    private String tags;

    private String description;

    private String meta;

    private String source;

    /**
     * push  pull  builtin
     */
    private String dataSource;

    private LocalDateTime createTime;

    /**
     * todo 部门排序 暂不考虑
     */
    private Integer deptIndex;

    private String treeType;

    private String abbreviation;


//    public DeptBean(Dept dept) {
//        this.setCode(dept.getDeptCode());
//        this.setParentCode(dept.getParentCode());
//        this.setName(dept.getDeptName());
//        this.setSource(dept.getSource());
//    }

    public DeptBean() {
    }
}
