package com.qtgl.iga.bean;


import lombok.Data;


import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * @author 1
 */
@Data
public class TreeBean implements Serializable {

    /**
     * 主键标识
     */
    private String id;
    /**
     * 代码
     */
    private String code;
    /**
     * 名称
     */
    private String name;
    /**
     * 英文名称
     */
    private String enName;
    /**
     * 是否顶级
     */
    private Integer independent;
    /**
     * 父级代码
     */
    private String parentCode;
    /**
     * 标签
     */
    private String tags;
    /**
     * 描述
     */
    private String description;

    /**
     * 来源
     */
    private String source;
    /**
     * 来源(机读)
     * push  PULL  BUILTIN
     */
    private String dataSource;
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    /**
     * 修改时间
     */
    private LocalDateTime updateTime;
    /**
     * 权威源类型
     */
    private String upstreamTypeId;
    /**
     * 排序字段
     */
    private Integer index;
    /**
     * 组织机构代码
     */
    private String treeType;
    /**
     * 简称
     */
    private String abbreviation;
    /**
     * 删除标记
     */
    private Integer delMark;
    /**
     * 类型
     */
    private String type;
    /**
     * 启用状态
     */
    private Integer active;
    /**
     * 规则
     */
    private String ruleId;
    /**
     * 代表色
     */
    private String color;
    /**
     * 是否包含规则
     */
    private Boolean isRuled;
    /**
     * 是否身份岗
     */
    private Boolean formal;

    /**
     * 组织机构-关系类型
     */
    private String relationType;

    private Map<String,String> dynamic;

    /**
     * 逻辑处理字段 规则是否启用
     */
    private Boolean ruleStatus;

//    private String deptTreeTypeId;
//    public DeptBean(Dept dept) {
//        this.setCode(dept.getDeptCode());
//        this.setParentCode(dept.getParentCode());
//        this.setName(dept.getDeptName());
//        this.setSource(dept.getSource());
//    }

    public TreeBean() {
    }
}
