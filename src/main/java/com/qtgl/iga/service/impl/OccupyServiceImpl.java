package com.qtgl.iga.service.impl;

import org.apache.commons.lang3.StringUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.qtgl.iga.bean.OccupyConnection;
import com.qtgl.iga.bean.OccupyDto;
import com.qtgl.iga.bean.OccupyEdge;
import com.qtgl.iga.bean.TreeBean;
import com.qtgl.iga.bo.*;
import com.qtgl.iga.dao.*;
import com.qtgl.iga.service.OccupyService;
import com.qtgl.iga.task.TaskConfig;
import com.qtgl.iga.utils.ClassCompareUtil;
import com.qtgl.iga.utils.DataBusUtil;
import com.qtgl.iga.utils.enumerate.ResultCode;
import com.qtgl.iga.utils.exception.CustomException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


@Service
@Slf4j
public class OccupyServiceImpl implements OccupyService {


    @Autowired
    TenantDao tenantDao;
    @Autowired
    NodeDao nodeDao;
    @Autowired
    NodeRulesDao rulesDao;
    @Autowired
    UpstreamTypeDao upstreamTypeDao;
    @Autowired
    UpstreamDao upstreamDao;
    @Autowired
    CardTypeDao cardTypeDao;
    @Autowired
    PersonDao personDao;
    @Autowired
    UserLogDao userLogDao;
    @Autowired
    DeptDao deptDao;
    @Autowired
    PostDao postDao;
    @Autowired
    OccupyDao occupyDao;

    @Autowired
    DataBusUtil dataBusUtil;
    @Autowired
    NodeRulesCalculationServiceImpl calculationService;


    /**
     * 1：根据规则获取所有的 人员身份数据
     * 2：人员身份根据人员进行分组
     * 2.1 判断数据是否有效  人员标识（证件类型+证件号码 OR  用户名 ）、岗位标识、部门标识不能为空
     * 2.2 判断人员标识 是否存在在sso中的人员表中
     * 2.3 相同身份数据进行合重
     * 2.4 校验孤儿数据
     * <p>
     * 3：根据人员和数据库中身份进行对比
     * A：新增 insert  上游提供、sso数据库中没有
     * B：修改 update  上游和sso对比后字段值有差异
     * C：删除 delete 上游提供了del_mark
     * D: 无效   上游曾经提供后，不再提供 OR 上游提供了active
     *
     * @param domain
     * @return
     */
    @Override
    public Map<String, List<OccupyDto>> buildOccupy(DomainInfo domain, TaskLog lastTaskLog) throws Exception {

        Tenant tenant = tenantDao.findByDomainName(domain.getDomainName());
        if (null == tenant) {
            throw new CustomException(ResultCode.FAILED, "租户不存在");
        }
        // 所有证件类型
        List<CardType> cardTypes = cardTypeDao.findAll(tenant.getId());
        Map<String, CardType> cardTypeMap = cardTypes.stream().collect(Collectors.toMap(CardType::getCardTypeCode, CardType -> CardType));


        // 获取规则
        Map arguments = new ConcurrentHashMap();
        arguments.put("type", "occupy");
        arguments.put("status", 0);
        List<Node> nodes = nodeDao.findNodes(arguments, domain.getId());
        if (null == nodes || nodes.size() <= 0) {
            throw new CustomException(ResultCode.FAILED, "无人员身份管理规则信息");
        }
        String nodeId = nodes.get(0).getId();

        List<NodeRules> occupyRules = rulesDao.getByNodeAndType(nodeId, 1, true, 0);

        // 获取sso中所有人员，用于验证 身份信息是否合法
        List<Person> personFromSSO = personDao.getAll(tenant.getId());
        if (personFromSSO.size() <= 0) {
            throw new CustomException(ResultCode.FAILED, "没有未删除的可用人员");
        }
        Map<String, Person> personFromSSOMap = personFromSSO.stream().filter(person -> !StringUtils.isBlank(person.getCardType()) && !StringUtils.isBlank(person.getCardNo())).collect(Collectors.toMap(person -> (person.getCardType() + ":" + person.getCardNo()), person -> person, (v1, v2) -> v2));
        Map<String, Person> personFromSSOMapByAccount = personFromSSO.stream().filter(person -> !StringUtils.isBlank(person.getAccountNo())).collect(Collectors.toMap(person -> (person.getAccountNo()), person -> person, (v1, v2) -> v2));
        // 获取sso中所有的有效的 组织机构 、 岗位信息
        List<TreeBean> deptFromSSO = deptDao.findActiveDataByTenantId(tenant.getId());
        if (null == deptFromSSO) {
            throw new CustomException(ResultCode.FAILED, "没有未删除且有效部门");
        }
        final Map<String, TreeBean> deptFromSSOMap = deptFromSSO.stream().collect(Collectors.toMap(dept -> (dept.getCode()), dept -> dept, (v1, v2) -> v2));
        List<TreeBean> postFromSSO = postDao.findActiveDataByTenantId(tenant.getId());
        if (null == postFromSSO) {
            throw new CustomException(ResultCode.FAILED, "没有未删除且有效岗位");
        }
        final Map<String, TreeBean> postFromSSOMap = postFromSSO.stream().collect(Collectors.toMap(post -> (post.getCode()), post -> post, (v1, v2) -> v2));


        // 获取所有规则 字段，用于更新验证
        Map<String, OccupyDto> occupyDtoFromUpstream = new HashMap<>();
        occupyRules.forEach(rules -> {
            UpstreamType upstreamType = upstreamTypeDao.findById(rules.getUpstreamTypesId());
            ArrayList<Upstream> upstreams = upstreamDao.getUpstreams(upstreamType.getUpstreamId(), domain.getId());
            final LocalDateTime now = LocalDateTime.now();
            JSONArray dataByBus = null;
            try {
                dataByBus = dataBusUtil.getDataByBus(upstreamType, domain.getDomainName());
            } catch (CustomException e) {
                throw e;
            } catch (Exception e) {
                log.error("人员身份类型中 : " + upstreamType.getUpstreamId() + "表达式异常");
                throw new CustomException(ResultCode.OCCUPY_ERROR, null, null, upstreamType.getDescription(), e.getMessage());
            }
            final List<OccupyDto> occupies = dataByBus.toJavaList(OccupyDto.class);
            TaskConfig.errorData.put(domain.getId(), JSON.toJSONString(JSON.toJSON(occupies)));
            for (OccupyDto occupyDto : occupies) {

                // 人员标识 证件类型、证件号码   OR    用户名 accountNo  必提供一个
                if (StringUtils.isBlank(occupyDto.getPersonCardNo()) && StringUtils.isBlank(occupyDto.getPersonCardType())) {
                    if (StringUtils.isBlank(occupyDto.getAccountNo())) {
                        log.error("人员身份信息中人员标识为空{}", occupyDto);
                        continue;
                    }

                }
                if (!StringUtils.isBlank(occupyDto.getPersonCardType()) && cardTypeMap.containsKey(occupyDto.getPersonCardType())) {
                    String cardTypeReg = cardTypeMap.get(occupyDto.getPersonCardType()).getCardTypeReg();
                    if (null != cardTypeReg && !Pattern.matches(cardTypeReg, occupyDto.getPersonCardNo())) {
                        log.error("人员身份信息中人员证件号码不符合规则{}", occupyDto);
                        continue;
                    }
                } else if (!StringUtils.isBlank(occupyDto.getPersonCardType()) && !cardTypeMap.containsKey(occupyDto.getPersonCardType())) {
                    log.error("人员身份信息中人员证件类型无效{}", occupyDto);
                    continue;
                }
                if (StringUtils.isBlank(occupyDto.getPostCode())) {
                    log.error("人员身份信息岗位代码为空{}", occupyDto);
                    continue;
                }
                if (StringUtils.isBlank(occupyDto.getDeptCode())) {
                    log.error("人员身份部门代码为空{}", occupyDto);
                    continue;
                }
                if (null != occupyDto.getActive() && occupyDto.getActive() != 0 && occupyDto.getActive() != 1) {
                    log.error("人员身份是否有效字段不合法{}", occupyDto.getActive());
                    continue;
                }
                if (null != occupyDto.getDelMark() && occupyDto.getDelMark() != 0 && occupyDto.getDelMark() != 1) {
                    log.error("人员身份是否删除字段不合法{}", occupyDto.getDelMark());
                    continue;
                }
                occupyDto.setSource(upstreams.get(0).getAppName() + "(" + upstreams.get(0).getAppCode() + ")");
                String personKey = occupyDto.getPersonCardType() + ":" + occupyDto.getPersonCardNo();
                String personKeyByAccount = occupyDto.getAccountNo();


                String personId = "";
                //优先以证件类型+证件号码 查找身份对应人员是否存在
                if (!personFromSSOMap.containsKey(personKey)) {
                    //其次以用户名 查找对应的人员是否存在
                    if (!personFromSSOMapByAccount.containsKey(personKeyByAccount)) {
                        log.error("人员身份无法找到对应对人员信息{}-{}", personKey, personKeyByAccount);
                        continue;
                    }
                    personId = personFromSSOMapByAccount.get(personKeyByAccount).getId();
                    occupyDto.setOpenId(personFromSSOMapByAccount.get(personKeyByAccount).getOpenId());
                    log.info("人员身份通过用户名获取到人员信息{}", personKeyByAccount);
                } else {
                    personId = personFromSSOMap.get(personKey).getId();
                    occupyDto.setOpenId(personFromSSOMap.get(personKey).getOpenId());
                    log.info("人员身份通过证件信息获取到人员信息{}", personKey);
                }
                occupyDto.setPersonId(personId);
                occupyDto.setCreateTime(now);
                occupyDto.setUpstreamType(upstreamType.getId());
                //给上游不提供的数据赋予默认值
                if (null == occupyDto.getUpdateTime()) {
                    occupyDto.setUpdateTime(now);
                }
                if (null == occupyDto.getDelMark()) {
                    occupyDto.setDelMark(0);
                }
                if (null == occupyDto.getActive()) {
                    occupyDto.setActive(1);
                }
                //赋予activeTime默认值
                occupyDto.setActiveTime(LocalDateTime.now());

                // 验证是否为 孤儿数据
                occupyDto.setOrphan(0);// 非孤儿
                if (!deptFromSSOMap.containsKey(occupyDto.getDeptCode())) {
                    occupyDto.setOrphan(1); // 因部门数据导致孤儿
                    if (!postFromSSOMap.containsKey(occupyDto.getPostCode())) {
                        occupyDto.setOrphan(3); // 因岗位+部门数据导致孤儿
                    }
                } else if (!postFromSSOMap.containsKey(occupyDto.getPostCode())) {
                    occupyDto.setOrphan(2); // 因 岗位数据导致孤儿
                }
                //以人员id岗位及部门code作为键进行身份去重
                String key = personId + ":" + occupyDto.getPostCode() + ":" + occupyDto.getDeptCode();
                if (occupyDtoFromUpstream.containsKey(key)) {
                    log.info("权威源人员身份数据合重:{}->{}", occupyDtoFromUpstream.get(key).toString(), occupyDto);
                    if (occupyDto.getActive() == 1) {
                        occupyDtoFromUpstream.put(key, occupyDto);
                    }
                } else {
                    occupyDtoFromUpstream.put(key, occupyDto);
                }

            }

        });
        log.info("所有人员身份数据获取完成:{}", occupyDtoFromUpstream.size());
        if (null != occupyDtoFromUpstream && occupyDtoFromUpstream.size() > 0) {
            TaskConfig.errorData.put(domain.getId(), "");
            //final List<OccupyDto> occupyDtos = (List<OccupyDto>) occupyDtoFromUpstream.values();
            // 获取sso中人员身份信息
            final List<OccupyDto> occupiesFromSSO = occupyDao.findAll(tenant.getId(), null, null);
            log.info("数据库中人员身份数据获取完成:{}", occupiesFromSSO.size());
            Map<String, OccupyDto> occupiesFromSSOMap = occupiesFromSSO.stream().
                    collect(Collectors.toMap(occupy -> (occupy.getPersonId() + ":" + occupy.getPostCode() + ":" + occupy.getDeptCode()), occupy -> occupy, (v1, v2) -> v2));
            Map<String, List<OccupyDto>> result = new HashMap<>();
            occupiesFromSSOMap.forEach((key, occupyFromSSO) -> {
                calculate(occupyDtoFromUpstream, result, key, occupyFromSSO);
            });

            occupyDtoFromUpstream.forEach((key, val) -> {
                if (!occupiesFromSSOMap.containsKey(key) && (occupyDtoFromUpstream.get(key).getDelMark() != 1)) {
                    val.setOccupyId(UUID.randomUUID().toString());
                    val.setStartTime(null != val.getStartTime() ? val.getStartTime() : LocalDateTime.of(1970, 1, 1, 0, 0, 0));
                    val.setEndTime(null != val.getEndTime() ? val.getEndTime() : LocalDateTime.of(2100, 1, 1, 0, 0, 0));
                    val.setValidStartTime(val.getStartTime());
                    val.setValidEndTime(val.getEndTime());
                    // 如果新增的数据 active=0 失效 或者 del_mark=1 删除  或者 判断为孤儿
                    //   都将 最终有效期设置为 失效
                    if (val.getActive() == 0 || val.getDelMark() == 1 || val.getOrphan() != 0) {
                        val.setValidStartTime(LocalDateTime.of(1970, 1, 1, 0, 0, 0));
                        val.setValidEndTime(LocalDateTime.of(1970, 1, 1, 0, 0, 0));
                    }
                    if (result.containsKey("insert")) {
                        result.get("insert").add(val);
                    } else {
                        result.put("insert", new ArrayList<OccupyDto>() {{
                            this.add(val);
                        }});
                    }
                    log.debug("人员身份对比后新增{}", val);
                }
            });

            // 验证监控规则
            calculationService.monitorRules(domain, lastTaskLog, occupiesFromSSO.size(), result.get("delete"));


            occupyDao.saveToSso(result, tenant.getId());
            //插入人员身份日志表
            ArrayList<OccupyDto> userLogs = new ArrayList<>();
            if (result.get("insert") != null) {
                userLogs.addAll(result.get("insert"));
            }
            if (result.get("update") != null) {
                userLogs.addAll(result.get("update"));
            }
            // todo 无效后对甘特图的影响
            userLogDao.saveUserLog(userLogs, tenant.getId());

            return result;
        } else {
            log.error("上游提供人员身份数据不符合规范,数据同步失败");
            throw new CustomException(ResultCode.FAILED, "上游提供人员身份数据不符合规范,数据同步失败");
        }
    }

    private void calculate(Map<String, OccupyDto> occupyDtoFromUpstream, Map<String, List<OccupyDto>> result, String key, OccupyDto occupyFromSSO) {
        // 对比出需要修改的occupy
        if (occupyDtoFromUpstream.containsKey(key) &&
                occupyDtoFromUpstream.get(key).getCreateTime().isAfter(occupyFromSSO.getUpdateTime())) {
            //修改标识
            boolean updateFlag = false;
            //删除恢复标识
//                boolean delRecoverFlag = false;
            //del字段标识
            boolean delFlag = false;
            //失效标识
            boolean invalidFlag = false;
            //恢复失效标识
            //   boolean invalidRecoverFlag = true;

            OccupyDto newOccupy = occupyDtoFromUpstream.get(key);
            List<UpstreamTypeField> fields = DataBusUtil.typeFields.get(newOccupy.getUpstreamType());

            // 如果字段上游不提供，则不进行更新
            // 字段值没有发生改变，不进行更新
            if (null != fields && fields.size() > 0) {
                for (UpstreamTypeField field : fields) {
                    String sourceField = field.getSourceField();
                    if ("personCardType".equals(sourceField) || "personCardNo".equals(sourceField)) {
                        continue;
                    }
                    Object newValue = ClassCompareUtil.getGetMethod(newOccupy, sourceField);
                    Object oldValue = ClassCompareUtil.getGetMethod(occupyFromSSO, sourceField);
                    if (null == oldValue && null == newValue) {
                        continue;
                    }
                    if (null != oldValue && oldValue.equals(newValue)) {
                        // 新值 = 老值 跳过
                        continue;
                    }
               /* if (sourceField.equals("delMark") && (Integer) oldValue == 1 && (Integer) newValue == 0) {
                    // 从删除中恢复  【计数算作新增】
                    delRecoverFlag = true;
                    log.info("人员身份信息{}从删除恢复 -> {}", occupyFromSSO.getOccupyId(), newOccupy.getSource());
                    continue;
                }*/
                    if (sourceField.equals("delMark") && (Integer) oldValue == 0 && (Integer) newValue == 1) {
                        // 上游真实推送了删除标记
                        delFlag = true;
                        log.info("人员身份信息{}删除 -> {}", occupyFromSSO.getOccupyId(), newOccupy.getSource());
                        continue;
                    }

                    updateFlag = true;
                    if (sourceField.equals("active") && (Integer) oldValue == 1 && (Integer) newValue == 0) {
                        invalidFlag = true;
                        // continue;

                    }


                    ClassCompareUtil.setValue(occupyFromSSO, occupyFromSSO.getClass(), sourceField, oldValue, newValue);
                    log.info("人员身份信息更新{}:字段{}：{} -> {}", occupyFromSSO.getOccupyId(), sourceField, oldValue, newValue);

                }
            }
        /*//上游没有提供delMark字段手动恢复
        if (delRecoverFlag) {
            occupyFromSSO.setDelMark(0);
            setValidTime(occupyFromSSO);
            if (result.containsKey("recover")) {
                result.get("recover").add(occupyFromSSO);
            } else {
                result.put("recover", new ArrayList<OccupyDto>() {{
                    this.add(occupyFromSSO);
                }});
            }
            log.info("人员身份信息{}从删除恢复", occupyFromSSO.getOccupyId());
        }
        //上游提供了删除字段 并且最新为删除
        }*/
            if (delFlag) {
                occupyFromSSO.setDelMark(1);
                occupyFromSSO.setUpdateTime(newOccupy.getUpdateTime());
                occupyFromSSO.setValidStartTime(LocalDateTime.of(1970, 1, 1, 0, 0, 0));
                occupyFromSSO.setValidEndTime(LocalDateTime.of(1970, 1, 1, 0, 0, 0));
                if (result.containsKey("delete")) {
                    result.get("delete").add(occupyFromSSO);
                } else {
                    result.put("delete", new ArrayList<OccupyDto>() {{
                        this.add(occupyFromSSO);
                    }});
                }
            }
            if (updateFlag && occupyFromSSO.getDelMark() != 1) {
                occupyFromSSO.setUpdateTime(newOccupy.getUpdateTime());
                // 区分出 更新数据  还是 无效数据（上游提供active字段 && 将active变为false）
                if (invalidFlag) {
                    occupyFromSSO.setActive(0);
                    occupyFromSSO.setActiveTime(newOccupy.getUpdateTime());
                    occupyFromSSO.setValidStartTime(LocalDateTime.of(1970, 1, 1, 0, 0, 0));
                    occupyFromSSO.setValidEndTime(LocalDateTime.of(1970, 1, 1, 0, 0, 0));
                    if (result.containsKey("invalid")) {
                        result.get("invalid").add(occupyFromSSO);
                    } else {
                        result.put("invalid", new ArrayList<OccupyDto>() {{
                            this.add(occupyFromSSO);
                        }});
                    }
                } else {
                    //失效恢复标识为true且sso的状态为无效

                    if (occupyFromSSO.getActive() != newOccupy.getActive()) {
                        occupyFromSSO.setActive(newOccupy.getActive());
                        occupyFromSSO.setActiveTime(newOccupy.getUpdateTime());
                    }
                    setValidTime(occupyFromSSO);
                    if (result.containsKey("update")) {
                        result.get("update").add(occupyFromSSO);
                    } else {
                        result.put("update", new ArrayList<OccupyDto>() {{
                            this.add(occupyFromSSO);
                        }});
                    }
                }


            }
            // 对比后，权威源提供的"映射字段"数据和sso中没有差异。 （active字段不提供）
            if (!updateFlag && occupyFromSSO.getDelMark() != 1) {
                //
                if (!occupyFromSSO.getActive().equals(newOccupy.getActive())) {
                    occupyFromSSO.setActive(newOccupy.getActive());
                    occupyFromSSO.setActiveTime(newOccupy.getUpdateTime());
                    occupyFromSSO.setUpdateTime(newOccupy.getUpdateTime());
                    if (result.containsKey("update")) {
                        result.get("update").add(occupyFromSSO);
                    } else {
                        result.put("update", new ArrayList<OccupyDto>() {{
                            this.add(occupyFromSSO);
                        }});
                    }

                }

            }
            log.debug("人员身份对比后更新{}-{}", occupyFromSSO, occupyDtoFromUpstream.get(key));
            //如果权威源没有,sso有并且来源是pull则置为失效
        } else if (!occupyDtoFromUpstream.containsKey(key)
                && 1 != occupyFromSSO.getDelMark()
                && (null == occupyFromSSO.getActive() || occupyFromSSO.getActive() == 1)
                && "PULL".equalsIgnoreCase(occupyFromSSO.getDataSource())) {
            // 如果sso 有，上游源没有 &&  sso中数据不是删除 && sso数据不是无效
            LocalDateTime now = LocalDateTime.now();
            occupyFromSSO.setActive(0);
            occupyFromSSO.setActiveTime(now);
            occupyFromSSO.setUpdateTime(now);
            occupyFromSSO.setValidStartTime(LocalDateTime.of(1970, 1, 1, 0, 0, 0));
            occupyFromSSO.setValidEndTime(LocalDateTime.of(1970, 1, 1, 0, 0, 0));
            if (result.containsKey("invalid")) {
                result.get("invalid").add(occupyFromSSO);
            } else {
                result.put("invalid", new ArrayList<OccupyDto>() {{
                    this.add(occupyFromSSO);
                }});
            }
            log.debug("人员身份对比后上游丢失{}", occupyFromSSO);
        }
    }

    /**
     * 如果数据 active=0 失效 或者 del_mark=1 删除 或者 当下不在开始 结束时间内 或者 判断为孤儿
     * 都将 最终有效期设置为 失效
     */
    private void setValidTime(OccupyDto occupyFromSSO) {
        occupyFromSSO.setValidStartTime(null != occupyFromSSO.getStartTime() ? occupyFromSSO.getStartTime() : LocalDateTime.of(1970, 1, 1, 0, 0, 0));
        occupyFromSSO.setValidEndTime(null != occupyFromSSO.getEndTime() ? occupyFromSSO.getEndTime() : LocalDateTime.of(2100, 1, 1, 0, 0, 0));
        if (occupyFromSSO.getActive() == 0 || occupyFromSSO.getDelMark() == 1 || occupyFromSSO.getOrphan() != 0) {
            occupyFromSSO.setValidStartTime(LocalDateTime.of(1970, 1, 1, 0, 0, 0));
            occupyFromSSO.setValidEndTime(LocalDateTime.of(1970, 1, 1, 0, 0, 0));
        }
    }

    @Override
    public OccupyConnection findOccupies(Map<String, Object> arguments, DomainInfo domain) {
        List<OccupyEdge> upstreamDept = new ArrayList<>();
        String upstreamTypeId = (String) arguments.get("upstreamTypeId");
        Integer offset = (Integer) arguments.get("offset");
        Integer first = (Integer) arguments.get("first");
        UpstreamType upstreamType = upstreamTypeDao.findById(upstreamTypeId);
        if (null != upstreamType && upstreamType.getIsPage()) {
            Map dataMap = null;
            try {
                dataMap = dataBusUtil.getDataByBus(upstreamType, offset, first);
            } catch (CustomException e) {
                throw e;
            } catch (Exception e) {
                log.error("人员身份治理中类型:{} 中 {} ", upstreamType.getDescription(), e.getMessage());
                throw new CustomException(ResultCode.OCCUPY_ERROR, null, null, upstreamType.getDescription(), e.getMessage());
            }

            Map deptMap = (Map) dataMap.get(upstreamType.getSynType());
            JSONArray deptArray = (JSONArray) JSONArray.toJSON(deptMap.get("edges"));
            Integer totalCount = (Integer) deptMap.get("totalCount");
            if (null != deptArray) {
                for (Object deptOb : deptArray) {
                    JSONObject nodeJson = (JSONObject) deptOb;
                    JSONObject node1 = nodeJson.getJSONObject("node");
                    OccupyDto occupy = node1.toJavaObject(OccupyDto.class);
                    OccupyEdge occupyEdge = new OccupyEdge();
                    occupyEdge.setNode(occupy);
                    upstreamDept.add(occupyEdge);
                }
            }
            OccupyConnection occupyConnection = new OccupyConnection();
            occupyConnection.setEdges(upstreamDept);
            occupyConnection.setTotalCount(totalCount);


            return occupyConnection;
        } else {
            log.error("数据类型不合法,请检查");
            throw new CustomException(ResultCode.FAILED, "数据类型不合法,请检查");
        }
    }
}
