package com.qtgl.iga.bean;

import com.qtgl.iga.bo.DynamicValue;
import com.qtgl.iga.utils.enums.DataStatusEnum;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * @author 1
 */
@Data
public class OccupyDto implements Cloneable {

    private String personId;
    private String occupyId;
    private String postCode;
    private String deptCode;
    private String personCardType;
    private String personCardNo;
    /**
     * 身份证件类型
     */
    private String identityCardType;
    /**
     * 身份证件编号
     */
    private String identityCardNo;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer index;
    private String source;
    private String dataSource;
    private String createSource;
    private String createDataSource;
    private LocalDateTime updateTime;
    private LocalDateTime createTime;
    private Integer delMark;
    // 上游删除
    private Integer active;
    private LocalDateTime activeTime;


    private String upstreamType;
    private String openId;
    //用户名
    private String accountNo;
    // 手机号
    private String cellPhone;
    //邮箱
    private String email;

    private String name;


    /**
     * 身份最终有效时间
     */
    private LocalDateTime validStartTime;
    private LocalDateTime validEndTime;
    /**
     * 上游岗位、部门导致身份失效
     */
    private Integer orphan;
    /**
     * 逻辑处理字段 规则是否启用
     */
    private Boolean ruleStatus;
    private Map<String, String> dynamic;

    //扩展字段值
    private List<DynamicValue> attrsValues;
    /**
     * 同步后状态 0 无变化 1 新增 2 修改 3 删除 4 失效
     */
    private Integer syncState;




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
        OccupyDto occupyDto = null;
        try {
            occupyDto = (OccupyDto) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
        return occupyDto;
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
