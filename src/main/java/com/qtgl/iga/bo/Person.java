package com.qtgl.iga.bo;

import com.qtgl.iga.utils.enums.DataStatusEnum;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * @author 1
 */
@Data
public class Person implements Serializable, Cloneable {

    /**
     * 主键
     */
    private String id;
    /**
     * 名称
     */
    private String name;

    /**
     * 性别
     */
    private String sex;
    /**
     * 生日
     */
    private LocalDateTime birthday;
    /**
     * 用户名
     */
    private String accountNo;
    /**
     * 证件类型
     */
    private String cardType;
    /**
     * 证件号码
     */
    private String cardNo;
    /**
     * 标签
     */
    private String tags;
    /**
     * 租户
     */
    private String tenantId;
    /**
     * 电话
     */
    private String cellphone;
    /**
     * 邮箱
     */
    private String email;
    /**
     * 来源
     */
    private String source;
    /**
     * 来源(机读)
     */
    private String dataSource;
    /**
     * 启用
     */
    private Integer active;
    /**
     * 启用时间
     */
    private LocalDateTime activeTime;
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    /**
     * 修改时间
     */
    private LocalDateTime updateTime;
    /**
     * 删除标记
     */
    private Integer delMark;
    /**
     * 权威源类型
     */
    private String upstreamType;

    private String openId;

    /**
     * 人员最终有效时间
     */
    private LocalDateTime validStartTime;

    private LocalDateTime validEndTime;

    /**
     * 冻结时间
     */
    private LocalDateTime freezeTime;

    /**
     * 密码
     */
    private String password;

    private Map<String, String> dynamic;
    /**
     * 逻辑处理字段 规则是否启用
     */
    private Boolean ruleStatus;

    //扩展字段值
    private List<DynamicValue> attrsValues;
    /**
     * 创建来源
     */
    private String createSource;
    /**
     * 创建来源(机读)
     * push  PULL  BUILTIN
     */
    private String createDataSource;
    /**
     * 同步后状态 0 无变化 1 新增 2 修改 3 删除 4 失效
     */
    private Integer syncState;

    /**
     * 头像url
     */
    private String avatarUrl;
    /**
     * 头像文件
     */
    private byte[] avatar;
    /**
     * 头像hashcode
     */
    private Integer avatarHashCode;
    /**
     * 头像更新时间
     */
    private LocalDateTime avatarUpdateTime;


    /**
     *
     */
    private String _uuid;
    /**
     * 权威源数据最终状态。  用于区分是否入库.
     * 可能存在  0 ：数据不过验证丢弃  1： 数据自动合重、 2: 数据手动合重、 3 数据新增、 4 数据修改 、 5 数据删除
     */
    private Integer upstreamDataStatus;
    /**
     * 权威源数据不入库的原因
     */
    private String upstreamDataReason;

    /**
     * 权威源是否入库状态
     *
     * @return
     */
    private Boolean storage;

    /**
     * 权威源来自具体规则
     * @return
     */
    public String upstreamRuleId;

    @Override
    public Object clone() {
        Person person = null;
        try {
            person = (Person) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return person;
    }


    public void upstreamMarkDel() {
        this.setUpstreamDataStatus(Integer.valueOf(DataStatusEnum.DELETE.getDesc()));
        this.setUpstreamDataReason("该数据上游直接标记为删除");
        this.setStorage(false);
    }

    public void upstreamMarkRuleDisable() {
        this.setUpstreamDataStatus(Integer.valueOf(DataStatusEnum.RULE_DISABLE.getDesc()));
        this.setUpstreamDataReason("该数据对应规则未启用");
        this.setStorage(false);
    }

    public void upstreamMarkErrorData(String errorReason) {
        this.setUpstreamDataStatus(Integer.valueOf(DataStatusEnum.ERROR_DATA.getDesc()));
        this.setUpstreamDataReason(errorReason);
        this.setStorage(false);
    }

    public void upstreamMarkAutoMerge() {
        this.setUpstreamDataStatus(Integer.valueOf(DataStatusEnum.AUTO_MERGE.getDesc()));
        this.setUpstreamDataReason("自动合重");
        this.setStorage(false);
    }

    public void upstreamMarkManualMerge() {
        this.setUpstreamDataStatus(Integer.valueOf(DataStatusEnum.MANUAL_MERGE.getDesc()));
        this.setUpstreamDataReason("手动合重");
        this.setStorage(false);
    }

    public void upstreamMarkNew() {
        this.setUpstreamDataStatus(Integer.valueOf(DataStatusEnum.NEW.getDesc()));
        this.setStorage(true);
    }

    public void upstreamMarkUpdate() {
        this.setUpstreamDataStatus(Integer.valueOf(DataStatusEnum.UPDATE.getDesc()));
        this.setStorage(true);
    }
    public void upstreamMarkNoChange(String reason) {
        this.setUpstreamDataStatus(Integer.valueOf(DataStatusEnum.NO_CHANGE.getDesc()));
        this.setUpstreamDataReason(reason);
        this.setStorage(true);
    }



}
