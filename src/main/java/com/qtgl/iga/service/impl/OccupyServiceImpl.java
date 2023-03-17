package com.qtgl.iga.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.qtgl.iga.bean.*;
import com.qtgl.iga.bo.*;
import com.qtgl.iga.config.PreViewOccupyThreadPool;
import com.qtgl.iga.dao.*;
import com.qtgl.iga.service.*;
import com.qtgl.iga.task.TaskConfig;
import com.qtgl.iga.utils.ClassCompareUtil;
import com.qtgl.iga.utils.DataBusUtil;
import com.qtgl.iga.utils.enumerate.ResultCode;
import com.qtgl.iga.utils.enums.TreeEnum;
import com.qtgl.iga.utils.exception.CustomException;
import com.qtgl.iga.vo.NodeRulesVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


@Service
@Slf4j
public class OccupyServiceImpl implements OccupyService {


    @Resource
    TenantDao tenantDao;
    @Resource
    NodeService nodeService;
    @Resource
    NodeRulesService rulesService;
    @Resource
    UpstreamTypeService upstreamTypeService;
    @Resource
    UpstreamService upstreamService;
    @Resource
    CardTypeDao cardTypeDao;
    @Resource
    PersonDao personDao;
    @Resource
    UserLogDao userLogDao;
    @Resource
    DeptDao deptDao;
    @Resource
    PostDao postDao;
    @Resource
    OccupyDao occupyDao;
    @Resource
    PreViewTaskService preViewTaskService;
    @Resource
    IncrementalTaskService incrementalTaskService;

    @Resource
    DataBusUtil dataBusUtil;
    @Resource
    NodeRulesCalculationServiceImpl calculationService;
    @Resource
    DynamicAttrService dynamicAttrService;
    @Resource
    DynamicValueService dynamicValueService;
    public static ConcurrentHashMap<String, List<JSONObject>> occupyErrorData = null;

    public static LocalDateTime DEFAULT_START_TIME = LocalDateTime.of(1970, 1, 1, 0, 0, 0);
    public static LocalDateTime DEFAULT_END_TIME = LocalDateTime.of(2100, 1, 1, 0, 0, 0);

    public static ConcurrentHashMap<String, Map<String, String>> occupyTypeFields = new ConcurrentHashMap<>();
    //类型
    private final String TYPE = "IDENTITY";


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
    public Map<String, List<OccupyDto>> buildOccupy(DomainInfo domain, TaskLog lastTaskLog, TaskLog currentTask, List<NodeRules> occupyRules) throws Exception {
        //错误数据容器初始化
        occupyErrorData = new ConcurrentHashMap<>();

        Tenant tenant = tenantDao.findByDomainName(domain.getDomainName());
        if (null == tenant) {
            throw new CustomException(ResultCode.FAILED, "租户不存在");
        }

        Map<String, Upstream> upstreamMap = new ConcurrentHashMap<>();
        List<NodeRulesVo> rules = new ArrayList<>();
        // 获取规则  (不为sub则获取所有规则)
        if (CollectionUtils.isEmpty(occupyRules)) {
            // 获取规则

            List<NodeDto> nodes = nodeService.findNodes(domain.getId(), 0, "occupy", true);
            if (null == nodes || nodes.size() <= 0) {
                log.error("无人员身份管理规则信息");
                return null;
            }
            List<NodeRulesVo> nodeRules = nodes.get(0).getNodeRules();
            //occupyRules = rulesService.getByNodeAndType(nodeId, 1, true, 0);
            // 获取所有规则 字段，用于更新验证
            if (null == nodeRules || nodeRules.size() == 0) {
                log.error("无人员身份管理规则信息");
                return null;
            }
            rules.addAll(nodeRules);

            //获取该租户下的当前类型的无效权威源
            ArrayList<Upstream> upstreams = upstreamService.findByDomainAndActiveIsFalse(domain.getId());
            if (!CollectionUtils.isEmpty(upstreams)) {
                upstreamMap = upstreams.stream().collect(Collectors.toMap((upstream -> upstream.getAppName() + "(" + upstream.getAppCode() + ")"), (upstream -> upstream)));
            }
        } else {
            //根据规则获取排除的权威源  及补充规则
            Set<String> strings = occupyRules.stream().collect(Collectors.groupingBy(NodeRules::getUpstreamTypesId)).keySet();

            List<Upstream> upstreams = upstreamService.findByUpstreamTypeIds(new ArrayList<>(strings), domain.getId());


            if (CollectionUtils.isEmpty(upstreams)) {
                log.error("当前sub 任务提供的规则有误请确认:{}", occupyRules);
                throw new CustomException(ResultCode.FAILED, "当前sub 任务提供的规则有误请确认");
            }
            List<String> ids = upstreams.stream().map(Upstream::getId).collect(Collectors.toList());

            //根据权威源和类型获取需要执行的规则
            rules = rulesService.findNodeRulesByUpStreamIdAndType(ids, "occupy", domain.getId(), 0);

            //获取除了该权威源以外的所有权威源(用于sub模式)
            ArrayList<Upstream> otherDomains = upstreamService.findByOtherUpstream(ids, domain.getId());
            if (!CollectionUtils.isEmpty(otherDomains)) {
                upstreamMap = otherDomains.stream().collect(Collectors.toMap((upstream -> upstream.getAppName() + "(" + upstream.getAppCode() + ")"), (upstream -> upstream)));
            }
        }


        // 存储最终需要操作的数据
        Map<String, List<OccupyDto>> result = new HashMap<>();
        //重复需要删除的sso身份数据
        ArrayList<OccupyDto> deleteFromSSO = new ArrayList<>();
        //上游数据,用于异常的数据展示
        Map<String, OccupyDto> occupyDtoFromUpstream = new HashMap<>();
        //增量日志容器
        List<IncrementalTask> incrementalTasks = new ArrayList<>();

        //扩展字段修改容器
        Map<String, DynamicValue> valueUpdateMap = new ConcurrentHashMap<>();
        List<DynamicValue> valueUpdate = new ArrayList<>();
        //扩展字段新增容器
        Map<String, DynamicValue> valueInsertMap = new ConcurrentHashMap<>();
        ArrayList<DynamicValue> valueInsert = new ArrayList<>();


        dataProcessing(rules, domain, tenant, result, deleteFromSSO, occupyDtoFromUpstream, incrementalTasks, currentTask, valueUpdateMap, valueInsertMap, upstreamMap);

        List<OccupyDto> occupiesFromSSO = occupyDao.findAll(tenant.getId(), null, null);

        // 验证监控规则
        calculationService.monitorRules(domain, lastTaskLog, occupiesFromSSO.size(), result.get("delete"), result.get("invalid"));
        //数据库重复身份删除
        if (!CollectionUtils.isEmpty(deleteFromSSO)) {
            if (result.containsKey("delete")) {
                result.get("delete").addAll(deleteFromSSO);
            } else {
                result.put("delete", new ArrayList<OccupyDto>() {{
                    this.addAll(deleteFromSSO);
                }});
            }
        }
        if (!CollectionUtils.isEmpty(valueInsertMap)) {
            valueInsert = new ArrayList<>(valueInsertMap.values());
        }
        if (!CollectionUtils.isEmpty(valueUpdateMap)) {
            valueUpdate = new ArrayList<>(valueUpdateMap.values());
        }

        try {
            occupyDao.saveToSso(result, tenant.getId(), valueUpdate, valueInsert);
            //if (!CollectionUtils.isEmpty(incrementalTasks)) {
            //    //添加增量日志
            //    incrementalTaskService.saveAll(incrementalTasks, domain);
            //}

        } catch (CustomException e) {
            if (!CollectionUtils.isEmpty(occupyDtoFromUpstream)) {
                TaskConfig.errorData.put(domain.getId(), JSONObject.toJSONString(occupyDtoFromUpstream));
            }
            throw new CustomException(ResultCode.FAILED, e.getErrorMsg());
        }

        errorDataProcessing(domain);


        //插入人员身份日志表
        ArrayList<OccupyDto> userLogs = new ArrayList<>();
        if (result.get("insert") != null) {
            userLogs.addAll(result.get("insert"));
        }
        if (result.get("update") != null) {
            userLogs.addAll(result.get("update"));
        }
        if (result.get("invalid") != null) {
            userLogs.addAll(result.get("invalid"));
        }
        // todo 无效后对甘特图的影响
        userLogDao.saveUserLog(userLogs, tenant.getId());
        return result;
    }

    private void errorDataProcessing(DomainInfo domain) {
        if (StringUtils.isNotBlank(TaskConfig.errorData.get(domain.getId()))) {
            String data = TaskConfig.errorData.get(domain.getId());
            JSONArray jsonArray = JSONObject.parseArray(data);

            if (null != jsonArray) {
                //
                if (!CollectionUtils.isEmpty(occupyErrorData.get(domain.getId()))) {
                    jsonArray.addAll(occupyErrorData.get(domain.getId()));
                }
            }
            TaskConfig.errorData.put(domain.getId(), JSONObject.toJSONString(jsonArray));

        } else {
            if (!CollectionUtils.isEmpty(occupyErrorData.get(domain.getId()))) {
                TaskConfig.errorData.put(domain.getId(), JSONObject.toJSONString(occupyErrorData.get(domain.getId())));
            }
        }
    }

    private List<OccupyDto> dataProcessing(List<NodeRulesVo> occupyRules, DomainInfo domain, Tenant tenant, Map<String, List<OccupyDto>> result, ArrayList<OccupyDto> deleteFromSSO, Map<String, OccupyDto> occupyDtoFromUpstream, List<IncrementalTask> incrementalTasks, TaskLog currentTask,
                                           Map<String, DynamicValue> valueUpdateMap, Map<String, DynamicValue> valueInsertMap, Map<String, Upstream> upstreamMap) {

        // 所有证件类型
        List<CardType> cardTypes = cardTypeDao.findAllUser(tenant.getId());
        Map<String, CardType> userCardTypeMap = cardTypes.stream().collect(Collectors.toMap(CardType::getCardTypeCode, CardType -> CardType));


        //List<CardType> cardTypes2 = cardTypeDao.findAllFromIdentity(tenant.getId());
        //Map<String, CardType> identityCardTypeMap = cardTypes2.stream().collect(Collectors.toMap(CardType::getCardTypeCode, CardType -> CardType));
        //获取扩展字段列表
        List<String> dynamicCodes = new ArrayList<>();

        List<DynamicValue> dynamicValues = new ArrayList<>();

        //扩展字段id与code对应map
        Map<String, String> attrMap = new ConcurrentHashMap<>();
        Map<String, String> attrReverseMap = new ConcurrentHashMap<>();

        List<DynamicAttr> dynamicAttrs = dynamicAttrService.findAllByType(TYPE, tenant.getId());
        log.info("获取到当前租户{}的映射字段集为{}", tenant.getId(), dynamicAttrs);
        if (!CollectionUtils.isEmpty(dynamicAttrs)) {
            dynamicCodes = dynamicAttrs.stream().map(DynamicAttr -> DynamicAttr.getCode()).collect(Collectors.toList());
            //获取扩展value
            List<String> attrIds = dynamicAttrs.stream().map(DynamicAttr -> DynamicAttr.getId()).collect(Collectors.toList());

            dynamicValues = dynamicValueService.findAllByAttrId(attrIds, tenant.getId());
        }

        //扩展字段值分组
        Map<String, List<DynamicValue>> valueMap = new ConcurrentHashMap<>();
        if (!CollectionUtils.isEmpty(dynamicValues)) {
            valueMap = dynamicValues.stream().filter(dynamicValue -> !StringUtils.isBlank(dynamicValue.getEntityId())).collect(Collectors.groupingBy(dynamicValue -> dynamicValue.getEntityId()));
        }
        List<String> finalDynamicCodes = dynamicCodes;
        Map<String, List<DynamicValue>> finalValueMap = valueMap;
        //扩展字段逻辑处理
        if (!CollectionUtils.isEmpty(dynamicAttrs)) {
            attrMap = dynamicAttrs.stream().collect(Collectors.toMap(DynamicAttr::getId, DynamicAttr::getCode));
            attrReverseMap = dynamicAttrs.stream().collect(Collectors.toMap(DynamicAttr::getCode, DynamicAttr::getId));
        }

        Map<String, String> finalAttrMap = attrMap;
        Map<String, String> finalAttrReverseMap = attrReverseMap;


        // 获取sso中所有人员，用于验证 身份信息是否合法
        List<Person> personFromSSO = personDao.getAll(tenant.getId());
        if (personFromSSO.size() <= 0) {
            throw new CustomException(ResultCode.FAILED, "数据库中无人员信息");
        }
        // 获取被合重过的人员信息
        List<Person> mergePerson = personDao.mergeCharacteristicPerson(tenant.getId());
        log.info("合重的人员size：" + mergePerson.size());


        // 获取sso中所有的有效的 组织机构 、 岗位信息
        List<TreeBean> deptFromSSO = deptDao.findActiveDataByTenantId(tenant.getId());
        if (null == deptFromSSO) {
            throw new CustomException(ResultCode.FAILED, "数据库中没有未删除且有效部门");
        }
        final Map<String, TreeBean> deptFromSSOMap = deptFromSSO.stream().collect(Collectors.toMap(dept -> (dept.getCode()), dept -> dept, (v1, v2) -> v2));
        List<TreeBean> postFromSSO = postDao.findActiveDataByTenantId(tenant.getId());
        if (null == postFromSSO) {
            throw new CustomException(ResultCode.FAILED, "数据库中没有未删除且有效岗位");
        }
        final Map<String, TreeBean> postFromSSOMap = postFromSSO.stream().collect(Collectors.toMap(post -> (post.getCode()), post -> post, (v1, v2) -> v2));


        // 获取sso中人员身份信息
        List<OccupyDto> occupiesFromSSO = occupyDao.findAll(tenant.getId(), null, null);
        log.info("数据库中人员身份数据获取完成:{}", occupiesFromSSO.size());
        //处理数据库重复身份数据
        ConcurrentHashMap<String, OccupyDto> concurrentHashMap = dealWithSsoOccupy(deleteFromSSO, occupiesFromSSO);
        occupiesFromSSO = new ArrayList<>(concurrentHashMap.values());
        final Map<String, OccupyDto> occupiesFromSSOIdentityMap = occupiesFromSSO.stream().filter(occupyDto ->
                StringUtils.isNotBlank(occupyDto.getIdentityCardType()) && StringUtils.isNotBlank(occupyDto.getIdentityCardNo()))
                .collect(Collectors.toMap(occupyDto -> (occupyDto.getIdentityCardType() + ":" + occupyDto.getIdentityCardNo()), occupyDto -> occupyDto, (v1, v2) -> v2));
        log.info("数据库中人员身份数据经过重复过滤后:{}", occupiesFromSSOIdentityMap.size());


        //获取租户下所有 "身份"权威源类型，根据类型的 人员匹配字段，定义的map，优化内存


        // key --> cardType:cardNo   value --> List<Person>
        Map<String, List<Person>> personFromSSOMap = null;
        // key --> accountNo  value --> List<Person>
        Map<String, List<Person>> personFromSSOMapByAccount = null;
        Map<String, List<Person>> personFromSSOMapByCardNo = null;
        Map<String, List<Person>> personFromSSOMapByPhone = null;
        Map<String, List<Person>> personFromSSOMapByEmail = null;
        Map<String, List<Person>> personFromSSOMapByOpenid = null;

        // 合重人员Map
        // key --> cardType:cardNo   value --> List<Person>
        Map<String, List<Person>> mergePersonFromSSOMap = null;
        // key --> accountNo  value --> List<Person>
        Map<String, List<Person>> mergePersonFromSSOMapByAccount = null;
        Map<String, List<Person>> mergePersonFromSSOMapByCardNo = null;
        Map<String, List<Person>> mergePersonFromSSOMapByPhone = null;
        Map<String, List<Person>> mergePersonFromSSOMapByEmail = null;

        // 开始遍历规则
        for (NodeRulesVo rules : occupyRules) {
            if (1 != rules.getType()) {
                continue;
            }
            if(rules.getIsIgnore()){
                //todo 忽略提示
                log.info("当前规则被忽略,跳过执行");
                continue;
            }
            UpstreamType upstreamType = upstreamTypeService.findById(rules.getUpstreamTypesId());
            String findPersonKey = upstreamType.getPersonCharacteristic();
            if (null == upstreamType) {
                log.error("人员身份对应拉取节点规则'{}'无有效权威源类型数据", rules);
                errorDataProcessing(domain);
                throw new CustomException(ResultCode.NO_UPSTREAM_TYPE, null, null, "人员身份", rules.getId());
            }
            if (StringUtils.isBlank(upstreamType.getPersonCharacteristic())) {
                log.error("人员身份对应拉取节点规则'{}'没有指定人员匹配模式", rules);
                errorDataProcessing(domain);
                throw new CustomException(ResultCode.NO_PERSON_CHARACTERISTIC, null, null, "人员身份", upstreamType.getId());
            }
            ArrayList<Upstream> upstreams = upstreamService.getUpstreams(upstreamType.getUpstreamId(), domain.getId());
            if (CollectionUtils.isEmpty(upstreams)) {
                log.error("人员身份对应拉取节点规则'{}'无权威源数据", rules.getId());
                errorDataProcessing(domain);
                throw new CustomException(ResultCode.NO_UPSTREAM, null, null, "人员身份", rules.getId());
            }
            //根据身份主体 处理sso数据
            switch (findPersonKey) {
                case "CARD_TYPE_NO":
                    if (null == personFromSSOMap) {
                        personFromSSOMap = personFromSSO.stream().filter(person -> !StringUtils.isBlank(person.getCardType()) && !StringUtils.isBlank(person.getCardNo()))
                                .collect(Collectors.groupingBy(person -> (person.getCardType() + ":" + person.getCardNo())));
                    }
                    if (null == mergePersonFromSSOMap) {
                        mergePersonFromSSOMap = mergePerson.stream().filter(person -> !StringUtils.isBlank(person.getCardType()) && !StringUtils.isBlank(person.getCardNo()))
                                .collect(Collectors.groupingBy(person -> (person.getCardType() + ":" + person.getCardNo())));
                    }
                    break;
                case "CARD_NO":
                    if (null == personFromSSOMapByCardNo) {
                        personFromSSOMapByCardNo = personFromSSO.stream().filter(person -> !StringUtils.isBlank(person.getCardNo()))
                                .collect(Collectors.groupingBy(person -> (person.getCardNo())));
                    }
                    if (null == mergePersonFromSSOMapByCardNo) {
                        mergePersonFromSSOMapByCardNo = mergePerson.stream().filter(person -> !StringUtils.isBlank(person.getCardNo()))
                                .collect(Collectors.groupingBy(person -> (person.getCardNo())));
                    }
                    break;
                case "USERNAME":
                    if (null == personFromSSOMapByAccount) {
                        personFromSSOMapByAccount = personFromSSO.stream().filter(person -> !StringUtils.isBlank(person.getAccountNo()))
                                .collect(Collectors.groupingBy(person -> (person.getAccountNo())));
                    }
                    if (null == mergePersonFromSSOMapByAccount) {
                        mergePersonFromSSOMapByAccount = mergePerson.stream().filter(person -> !StringUtils.isBlank(person.getAccountNo()))
                                .collect(Collectors.groupingBy(person -> (person.getAccountNo())));
                    }
                    break;
                case "EMAIL":
                    if (null == personFromSSOMapByEmail) {
                        personFromSSOMapByEmail = personFromSSO.stream().filter(person -> !StringUtils.isBlank(person.getEmail()))
                                .collect(Collectors.groupingBy(person -> (person.getEmail())));
                    }
                    if (null == mergePersonFromSSOMapByEmail) {
                        mergePersonFromSSOMapByEmail = mergePerson.stream().filter(person -> !StringUtils.isBlank(person.getEmail()))
                                .collect(Collectors.groupingBy(person -> (person.getEmail())));
                    }

                    break;
                case "CELLPHONE":
                    if (null == personFromSSOMapByEmail) {
                        personFromSSOMapByEmail = personFromSSO.stream().filter(person -> !StringUtils.isBlank(person.getEmail()))
                                .collect(Collectors.groupingBy(person -> (person.getEmail())));
                    }
                    if (null == mergePersonFromSSOMapByPhone) {
                        mergePersonFromSSOMapByPhone = mergePerson.stream().filter(person -> !StringUtils.isBlank(person.getCellphone()))
                                .collect(Collectors.groupingBy(person -> (person.getCellphone())));
                    }
                    break;
                case "OPENID":
                    if (null == personFromSSOMapByOpenid) {
                        personFromSSOMapByOpenid = personFromSSO.stream().filter(person -> !StringUtils.isBlank(person.getOpenId()))
                                .collect(Collectors.groupingBy(person -> (person.getOpenId())));
                    }
                    break;
            }


            final LocalDateTime now = LocalDateTime.now();
            JSONArray dataByBus;
            try {
                dataByBus = dataBusUtil.getDataByBus(upstreamType, domain.getDomainName());
            } catch (CustomException e) {
                if (new Long("1085").equals(e.getCode())) {
                    throw new CustomException(ResultCode.INVOKE_URL_ERROR, "请求资源地址失败,请检查权威源:" + upstreams.get(0).getAppName() + "(" + upstreams.get(0).getAppCode() + ")" + "下的权威源类型:" + upstreamType.getDescription());
                } else {
                    throw e;
                }
            } catch (Exception e) {
                log.error("人员身份类型中 : " + upstreamType.getUpstreamId() + "表达式异常");
                errorDataProcessing(domain);
                throw new CustomException(ResultCode.OCCUPY_ERROR, null, null, upstreamType.getDescription(), e.getMessage());
            }
            List<OccupyDto> occupies = new ArrayList<>();

            ArrayList<OccupyDto> resultOccupies = new ArrayList<>();
            if (null != dataByBus) {
                getOccupy(domain, occupyDtoFromUpstream, userCardTypeMap, finalDynamicCodes, deptFromSSOMap, postFromSSOMap, personFromSSOMap, personFromSSOMapByAccount, personFromSSOMapByCardNo, personFromSSOMapByPhone, personFromSSOMapByEmail, personFromSSOMapByOpenid, mergePersonFromSSOMap, mergePersonFromSSOMapByAccount, mergePersonFromSSOMapByCardNo, mergePersonFromSSOMapByPhone, mergePersonFromSSOMapByEmail, rules, upstreamType, findPersonKey, upstreams, now, dataByBus, occupies, resultOccupies);
                if (!CollectionUtils.isEmpty(occupyDtoFromUpstream)) {
                    //权威源类型为增量则添加对应的增量同步日志
                    if (null != currentTask && null != upstreamType.getIsIncremental() && upstreamType.getIsIncremental() && !CollectionUtils.isEmpty(resultOccupies)) {
                        addIncrementalTask(domain, currentTask, upstreamType, resultOccupies);
                    }
                } else {
                    log.error("权威源类型:{}提供人员身份数据不符合规范,本次同步跳过该权威源类型", upstreamType.getId());
                }
            }

        }
        log.info("所有人员身份数据获取完成:{}", occupyDtoFromUpstream.size());
        if (!CollectionUtils.isEmpty(occupyDtoFromUpstream)) {
            List<OccupyDto> occupyDtos = new ArrayList<>();

            if (!CollectionUtils.isEmpty(occupiesFromSSO)) {
                occupyDtos.addAll(occupiesFromSSO);
            }
            Map<String, OccupyDto> preViewOccupyMap = occupyDtos.stream().filter(occupyDto -> !StringUtils.isBlank(occupyDto.getOccupyId())).collect(Collectors.toMap(occupyDto -> (occupyDto.getOccupyId()), occupyDto -> occupyDto, (v1, v2) -> v2));
            //预置没有变化的人员   未删除的人员身份
            Map<String, OccupyDto> keepOccupyMap = occupyDtos.stream().filter(occupyDto -> !StringUtils.isBlank(occupyDto.getOccupyId()) && occupyDto.getDelMark() != 1).collect(Collectors.toMap(occupyDto -> (occupyDto.getOccupyId()), occupyDto -> occupyDto, (v1, v2) -> v2));


            Map<String, OccupyDto> occupiesFromSSOMap = occupiesFromSSO.stream().
                    collect(Collectors.toMap(occupy -> (occupy.getPersonId() + ":" + occupy.getPostCode() + ":" + occupy.getDeptCode()), occupy -> occupy, (v1, v2) -> v2));

            //当前时刻
            LocalDateTime now = LocalDateTime.now();
            occupiesFromSSOMap.forEach((key, occupyFromSSO) -> {
                calculate(occupyDtoFromUpstream, result, key, occupyFromSSO, upstreamMap, preViewOccupyMap, now, finalAttrMap, finalValueMap, valueUpdateMap, valueInsertMap, keepOccupyMap);
            });
            /**
             * 新增 上游提供start,end_time则使用作为最终有效期(如果当前时刻在最终有效期,且标识为状态为失效则为start_time-now()),否则为1970-2100
             */
            occupyDtoFromUpstream.forEach((key, val) -> {
                calculateInsert(tenant, result, occupyDtoFromUpstream, valueInsertMap, finalAttrReverseMap, occupiesFromSSOMap, now, key, val);
            });
            //处理人员身份预览数据
            occupyDtos = new ArrayList<>(preViewOccupyMap.values());
            if (!CollectionUtils.isEmpty(result.get("insert"))) {
                occupyDtos.addAll(result.get("insert"));
            }
            if (!CollectionUtils.isEmpty(keepOccupyMap)) {
                result.put("keep", new ArrayList<>(keepOccupyMap.values()));
            }
            return occupyDtos;
        } else {
            log.error("上游提供人员身份数据不符合规范,数据同步失败");
            errorDataProcessing(domain);
            throw new CustomException(ResultCode.FAILED, "上游提供人员身份数据不符合规范,数据同步失败");
        }
    }

    private void calculateInsert(Tenant tenant, Map<String, List<OccupyDto>> result, Map<String, OccupyDto> occupyDtoFromUpstream, Map<String, DynamicValue> valueInsertMap, Map<String, String> finalAttrReverseMap, Map<String, OccupyDto> occupiesFromSSOMap, LocalDateTime now, String key, OccupyDto val) {
        if (!occupiesFromSSOMap.containsKey(key) && (occupyDtoFromUpstream.get(key).getDelMark() != 1)) {
            if (val.getRuleStatus()) {
                val.setOccupyId(UUID.randomUUID().toString());

                val.setValidStartTime(null != val.getStartTime() ? val.getStartTime() : DEFAULT_START_TIME);
                val.setValidEndTime(null != val.getEndTime() ? val.getEndTime() : DEFAULT_END_TIME);
                ////如果当前时刻在最终有效期内,其余情况不做处理
                //if (!now.isBefore(val.getValidStartTime()) && !now.isAfter(val.getValidEndTime())) {
                //    //当前标识位为无效(active=0 失效 或者 del_mark=1 删除  或者 判断为孤儿)
                //    if (!(val.getActive() == 1 && val.getDelMark() == 0 && val.getOrphan() == 0)) {
                //        val.setValidEndTime(now);
                //    }
                //}

                ArrayList<DynamicValue> dynamicValues = new ArrayList<>();
                Map<String, String> dynamic = val.getDynamic();
                if (!CollectionUtils.isEmpty(dynamic)) {
                    for (Map.Entry<String, String> str : dynamic.entrySet()) {
                        DynamicValue dynamicValue = new DynamicValue();
                        dynamicValue.setId(UUID.randomUUID().toString());
                        dynamicValue.setValue(str.getValue());
                        dynamicValue.setEntityId(val.getOccupyId());
                        dynamicValue.setTenantId(tenant.getId());
                        dynamicValue.setAttrId(finalAttrReverseMap.get(str.getKey()));
                        valueInsertMap.put(dynamicValue.getAttrId()
                                + "-" + dynamicValue.getEntityId(), dynamicValue);
                        //扩展字段预览展示
                        dynamicValue.setKey(dynamicValue.getAttrId());
                        dynamicValue.setCode(str.getValue());
                        dynamicValues.add(dynamicValue);
                    }
                }
                val.setAttrsValues(dynamicValues);

                checkValidTime(val, now, false);
                if (result.containsKey("insert")) {
                    result.get("insert").add(val);
                } else {
                    result.put("insert", new ArrayList<OccupyDto>() {{
                        this.add(val);
                    }});
                }
                log.debug("人员身份对比后新增{}", val);
            } else {
                log.debug("人员身份{},对应规则未启用,本次跳过该数据", val);
            }
        }
    }

    private void addIncrementalTask(DomainInfo domain, TaskLog currentTask, UpstreamType upstreamType, ArrayList<OccupyDto> resultOccupies) {
        List<OccupyDto> collect1 = resultOccupies.stream().sorted(Comparator.comparing(OccupyDto::getUpdateTime).reversed()).collect(Collectors.toList());
        IncrementalTask incrementalTask = new IncrementalTask();
        incrementalTask.setId(UUID.randomUUID().toString());
        incrementalTask.setMainTaskId(currentTask.getId());
        incrementalTask.setType("occupy");
        log.info("类型:{},权威源类型:{},上游增量最大修改时间:{} -> {},当前时刻:{}", upstreamType.getSynType(), upstreamType.getId(), collect1.get(0).getUpdateTime(), collect1.get(0).getUpdateTime().toInstant(ZoneOffset.ofHours(+8)).toEpochMilli(), System.currentTimeMillis());
        long min = Math.min(collect1.get(0).getUpdateTime().toInstant(ZoneOffset.ofHours(+8)).toEpochMilli(), System.currentTimeMillis());
        incrementalTask.setTime(new Timestamp(min));
        incrementalTask.setUpstreamTypeId(collect1.get(0).getUpstreamType());
        //incrementalTasks.add(incrementalTask);
        incrementalTaskService.save(incrementalTask, domain);
    }

    private void getOccupy(DomainInfo domain, Map<String, OccupyDto> occupyDtoFromUpstream, Map<String, CardType> userCardTypeMap, List<String> finalDynamicCodes, Map<String, TreeBean> deptFromSSOMap, Map<String, TreeBean> postFromSSOMap, Map<String, List<Person>> personFromSSOMap, Map<String, List<Person>> personFromSSOMapByAccount, Map<String, List<Person>> personFromSSOMapByCardNo, Map<String, List<Person>> personFromSSOMapByPhone, Map<String, List<Person>> personFromSSOMapByEmail, Map<String, List<Person>> personFromSSOMapByOpenid, Map<String, List<Person>> mergePersonFromSSOMap, Map<String, List<Person>> mergePersonFromSSOMapByAccount, Map<String, List<Person>> mergePersonFromSSOMapByCardNo, Map<String, List<Person>> mergePersonFromSSOMapByPhone, Map<String, List<Person>> mergePersonFromSSOMapByEmail, NodeRules rules, UpstreamType upstreamType, String findPersonKey, ArrayList<Upstream> upstreams, LocalDateTime now, JSONArray dataByBus, List<OccupyDto> occupies, ArrayList<OccupyDto> resultOccupies) {
        for (Object o : dataByBus) {
            JSONObject occupyObj = JSON.parseObject(JSON.toJSONString(o));
            if (null != occupyObj.getTimestamp(TreeEnum.UPDATE_TIME.getCode())) {
                occupyObj.put(TreeEnum.UPDATE_TIME.getCode(), occupyObj.getTimestamp(TreeEnum.UPDATE_TIME.getCode()));
            }
            if (null != occupyObj.getTimestamp(TreeEnum.START_TIME.getCode())) {
                occupyObj.put(TreeEnum.START_TIME.getCode(), occupyObj.getTimestamp(TreeEnum.START_TIME.getCode()));
            }
            if (null != occupyObj.getTimestamp(TreeEnum.END_TIME.getCode())) {
                occupyObj.put(TreeEnum.END_TIME.getCode(), occupyObj.getTimestamp(TreeEnum.END_TIME.getCode()));
            }
            OccupyDto occupyDto = occupyObj.toJavaObject(OccupyDto.class);
            if (StringUtils.isBlank(occupyDto.getPostCode())) {
                log.error("人员身份信息岗位代码为空{}", occupyDto);
                extracted(domain, occupyDto, "人员身份信息岗位代码为空");
                continue;
            }
            if (StringUtils.isBlank(occupyDto.getDeptCode())) {
                log.error("人员身份部门代码为空{}", occupyDto);
                extracted(domain, occupyDto, "人员身份部门代码为空");
                continue;
            }
            if (null != occupyDto.getActive() && occupyDto.getActive() != 0 && occupyDto.getActive() != 1) {
                log.error("人员身份是否有效字段不合法{}", occupyDto.getActive());
                extracted(domain, occupyDto, "人员身份是否有效字段不合法");
                continue;
            }
            if (null != occupyDto.getDelMark() && occupyDto.getDelMark() != 0 && occupyDto.getDelMark() != 1) {
                log.error("人员身份是否删除字段不合法{}", occupyDto.getDelMark());
                extracted(domain, occupyDto, "人员身份是否删除字段不合法");
                continue;
            }

            //**************************************************************************************************************************
            //**************************************************************************************************************************
            // 20220922 删除【 old 人员标识 证件类型、证件号码   OR    用户名 accountNo  OR  身份标识  必提供一个】
              /*  if (StringUtils.isBlank(occupyDto.getPersonCardNo()) || StringUtils.isBlank(occupyDto.getPersonCardType())) {
                    if (StringUtils.isBlank(occupyDto.getAccountNo())) {
                        if (StringUtils.isBlank(occupyDto.getIdentityCardNo()) || StringUtils.isBlank(occupyDto.getIdentityCardType())) {
                            log.error("人员身份信息中人员标识为空{}", occupyDto);
                            extracted(domain, occupyDto, "人员身份信息中人员标识为空");
                            continue;
                        } else {
                            if (identityCardTypeMap.containsKey(occupyDto.getIdentityCardType())) {
                                String cardTypeReg = identityCardTypeMap.get(occupyDto.getIdentityCardType()).getCardTypeReg();
                                if (StringUtils.isNotBlank(cardTypeReg) && !Pattern.matches(cardTypeReg, occupyDto.getIdentityCardNo())) {
                                    log.error("人员身份信息中身份证件号码不符合规则{}", occupyDto);
                                    extracted(domain, occupyDto, "人员身份信息中身份证件号码不符合规则");
                                    continue;
                                }
                            } else {
                                log.error("人员身份信息中身份证件类型无效{}", occupyDto);
                                extracted(domain, occupyDto, "人员身份信息中身份证件类型无效");
                                continue;
                            }
                        }

                    }

                } else {
                    if (userCardTypeMap.containsKey(occupyDto.getPersonCardType())) {
                        String cardTypeReg = userCardTypeMap.get(occupyDto.getPersonCardType()).getCardTypeReg();
                        if (StringUtils.isNotBlank(cardTypeReg) && !Pattern.matches(cardTypeReg, occupyDto.getPersonCardNo())) {
                            log.error("人员身份信息中人员证件号码不符合规则{}", occupyDto);
                            extracted(domain, occupyDto, "人员身份信息中人员证件号码不符合规则");
                            continue;
                        }
                    } else {
                        log.error("人员身份信息中人员证件类型无效{}", occupyDto);
                        extracted(domain, occupyDto, "人员身份信息中人员证件类型无效");
                        continue;
                    }
                }*/
            //**************************************************************************************************************************
            //**************************************************************************************************************************
            // 20220922 新增【权威源指定 匹配人员字段信息：CARD_TYPE_NO:证件类型+证件号码 CARD_NO:仅证件号码 ACCOUNT_NO:用户名 EMAIL:邮箱 CELLPHONE:手机号 OPENID:openid】

            String personKey = "";
            List<Person> persons = new ArrayList<>();
            //处理扩展字段
            ConcurrentHashMap<String, String> map = null;
            if (!CollectionUtils.isEmpty(finalDynamicCodes)) {
                map = new ConcurrentHashMap<>();
                for (String dynamicCode : finalDynamicCodes) {
                    if (occupyObj.containsKey(dynamicCode)) {
                        if (StringUtils.isNotBlank(occupyObj.getString(dynamicCode))) {
                            map.put(dynamicCode, occupyObj.getString(dynamicCode));
                        }
                    }
                }
                log.info("处理{}的上游扩展字段值为{}", occupyObj, map);
                occupyDto.setDynamic(map);
            }
            switch (findPersonKey) {
                case "CARD_TYPE_NO":
                    if (StringUtils.isBlank(occupyDto.getPersonCardNo()) || StringUtils.isBlank(occupyDto.getPersonCardType())) {
                        log.error("【通过证件类型+证件号码匹配人员】证件类型或号码不能为空{}", occupyDto);
                        extracted(domain, occupyDto, "【通过证件类型+证件号码匹配人员】证件类型或号码不能为空");
                        continue;
                    }
                    if (userCardTypeMap.containsKey(occupyDto.getPersonCardType())) {
                        String cardTypeReg = userCardTypeMap.get(occupyDto.getPersonCardType()).getCardTypeReg();
                        if (StringUtils.isNotBlank(cardTypeReg) && !Pattern.matches(cardTypeReg, occupyDto.getPersonCardNo())) {
                            log.error("【通过证件类型+证件号码匹配人员】人员身份信息中人员证件号码不符合规则{}", occupyDto);
                            extracted(domain, occupyDto, "【通过证件类型+证件号码匹配人员】人员身份信息中人员证件号码不符合规则");
                            continue;
                        }
                    } else {
                        log.error("【通过证件类型+证件号码匹配人员】人员身份信息中人员证件类型无效{}", occupyDto);
                        extracted(domain, occupyDto, "【通过证件类型+证件号码匹配人员】人员身份信息中人员证件类型无效");
                        continue;
                    }
                    /*  去person Map 中 找人信息*/
                    personKey = occupyDto.getPersonCardType() + ":" + occupyDto.getPersonCardNo();
                    if (!personFromSSOMap.containsKey(personKey) && !mergePersonFromSSOMap.containsKey(personKey)) {
                        log.error("【通过证件类型+证件号码匹配人员】人员身份无法找到对应对人员信息{}", personKey);
                        extracted(domain, occupyDto, "【通过证件类型+证件号码匹配人员】人员身份无法找到对应对人员信息");
                        continue;
                    }

                    if (personFromSSOMap.containsKey(personKey)) {
                        persons = personFromSSOMap.get(personKey);
                        if (mergePersonFromSSOMap.containsKey(personKey)) {
                            persons.addAll(mergePersonFromSSOMap.get(personKey));
                        }
                    } else {
                        persons = mergePersonFromSSOMap.get(personKey);
                    }

                    if (persons.size() >= 0) {
                        for (Person person : persons) {
                            createOccupyDto(domain, occupyDtoFromUpstream, deptFromSSOMap, postFromSSOMap, rules, upstreamType, upstreams, now, occupies, resultOccupies, occupyDto, person);
                            log.debug("【通过证件类型+证件号码匹配人员】人员身份通过人员类型+证件获取到人员信息{}", personKey);
                        }
                    }

                    break;
                case "CARD_NO":
                    if (StringUtils.isBlank(occupyDto.getPersonCardNo())) {
                        log.error("【通过证件号码匹配人员】号码不能为空{}", occupyDto);
                        extracted(domain, occupyDto, "【通过证件号码匹配人员】号码不能为空");
                        continue;
                    }
                    /*  去person Map 中 找人信息*/
                    personKey = occupyDto.getPersonCardNo();
                    if (!personFromSSOMapByCardNo.containsKey(personKey) && !mergePersonFromSSOMapByCardNo.containsKey(personKey)) {
                        log.error("【通过证件号码匹配人员】人员身份无法找到对应对人员信息{}", personKey);
                        extracted(domain, occupyDto, "【通过证件号码匹配人员】人员身份无法找到对应对人员信息");
                        continue;
                    }

                    if (personFromSSOMapByCardNo.containsKey(personKey)) {
                        persons = personFromSSOMapByCardNo.get(personKey);
                        if (mergePersonFromSSOMapByCardNo.containsKey(personKey)) {
                            persons.addAll(mergePersonFromSSOMapByCardNo.get(personKey));
                        }
                    } else {
                        persons = mergePersonFromSSOMapByCardNo.get(personKey);
                    }
                    if (persons.size() >= 0) {
                        for (Person person : persons) {
                            createOccupyDto(domain, occupyDtoFromUpstream, deptFromSSOMap, postFromSSOMap, rules, upstreamType, upstreams, now, occupies, resultOccupies, occupyDto, person);
                            log.debug("【通过证件号码匹配人员】人员身份找找到人员信息{}", personKey);
                        }
                    }
                    ;
                    break;
                case "USERNAME":
                    if (StringUtils.isBlank(occupyDto.getAccountNo())) {
                        log.error("【通过用户名码匹配人员】用户名不能为空{}", occupyDto);
                        extracted(domain, occupyDto, "【通过用户名匹配人员】用户名不能为空");
                        continue;
                    }
                    /*  去person Map 中 找人信息*/
                    personKey = occupyDto.getAccountNo();
                    if (!personFromSSOMapByAccount.containsKey(personKey) && !mergePersonFromSSOMapByAccount.containsKey(personKey)) {
                        log.error("【通过用户名码匹配人员】人员身份无法找到对应对人员信息{}", personKey);
                        extracted(domain, occupyDto, "【通过用户名码匹配人员】人员身份无法找到对应对人员信息");
                        continue;
                    }
                    if (personFromSSOMapByAccount.containsKey(personKey)) {
                        persons = personFromSSOMapByAccount.get(personKey);
                        if (mergePersonFromSSOMapByAccount.containsKey(personKey)) {
                            persons.addAll(mergePersonFromSSOMapByAccount.get(personKey));
                        }
                    } else {
                        persons = mergePersonFromSSOMapByAccount.get(personKey);
                    }
                    if (persons.size() >= 0) {
                        for (Person person : persons) {
                            createOccupyDto(domain, occupyDtoFromUpstream, deptFromSSOMap, postFromSSOMap, rules, upstreamType, upstreams, now, occupies, resultOccupies, occupyDto, person);
                            log.debug("【通过用户名码匹配人员】人员身份找找到人员信息{}", personKey);
                        }
                    }
                    ;
                    break;
                case "EMAIL":
                    if (StringUtils.isBlank(occupyDto.getEmail())) {
                        log.error("【通过邮箱匹配人员】邮箱不能为空{}", occupyDto);
                        extracted(domain, occupyDto, "【通过邮箱匹配人员】邮箱不能为空");
                        continue;
                    }
                    /*  去person Map 中 找人信息*/
                    personKey = occupyDto.getAccountNo();
                    if (!personFromSSOMapByEmail.containsKey(personKey) && !mergePersonFromSSOMapByEmail.containsKey(personKey)) {
                        log.error("【通过邮箱匹配人员】人员身份无法找到对应对人员信息{}", personKey);
                        extracted(domain, occupyDto, "【通过邮箱匹配人员】人员身份无法找到对应对人员信息");
                        continue;
                    }
                    if (personFromSSOMapByEmail.containsKey(personKey)) {
                        persons = personFromSSOMapByEmail.get(personKey);
                        if (mergePersonFromSSOMapByEmail.containsKey(personKey)) {
                            persons.addAll(mergePersonFromSSOMapByEmail.get(personKey));
                        }
                    } else {
                        persons = mergePersonFromSSOMapByEmail.get(personKey);
                    }
                    if (persons.size() >= 0) {
                        for (Person person : persons) {
                            createOccupyDto(domain, occupyDtoFromUpstream, deptFromSSOMap, postFromSSOMap, rules, upstreamType, upstreams, now, occupies, resultOccupies, occupyDto, person);
                            log.debug("【通过邮箱匹配人员】人员身份找找到人员信息{}", personKey);
                        }
                    }
                    ;
                    break;
                case "CELLPHONE":
                    if (StringUtils.isBlank(occupyDto.getCellPhone())) {
                        log.error("【通过电话匹配人员】电话不能为空{}", occupyDto);
                        extracted(domain, occupyDto, "【通过电话匹配人员】电话不能为空");
                        continue;
                    }
                    /*  去person Map 中 找人信息*/
                    personKey = occupyDto.getAccountNo();
                    if (!personFromSSOMapByPhone.containsKey(personKey) && !mergePersonFromSSOMapByPhone.containsKey(personKey)) {
                        log.error("【通过电话匹配人员】人员身份无法找到对应对人员信息{}", personKey);
                        extracted(domain, occupyDto, "【通过电话匹配人员】人员身份无法找到对应对人员信息");
                        continue;
                    }
                    if (personFromSSOMapByPhone.containsKey(personKey)) {
                        persons = personFromSSOMapByPhone.get(personKey);
                        if (mergePersonFromSSOMapByPhone.containsKey(personKey)) {
                            persons.addAll(mergePersonFromSSOMapByPhone.get(personKey));
                        }
                    } else {
                        persons = mergePersonFromSSOMapByPhone.get(personKey);
                    }
                    if (persons.size() >= 0) {
                        for (Person person : persons) {
                            createOccupyDto(domain, occupyDtoFromUpstream, deptFromSSOMap, postFromSSOMap, rules, upstreamType, upstreams, now, occupies, resultOccupies, occupyDto, person);
                            log.debug("【通过电话匹配人员】人员身份找找到人员信息{}", personKey);
                        }
                    }
                    ;
                    break;
                case "OPENID":
                    if (StringUtils.isBlank(occupyDto.getOpenId())) {
                        log.error("【通过OPENID匹配人员】电话不能为空{}", occupyDto);
                        extracted(domain, occupyDto, "【通过电话匹配人员】电话不能为空");
                        continue;
                    }
                    /*  去person Map 中 找人信息*/
                    personKey = occupyDto.getOpenId();
                    if (!personFromSSOMapByOpenid.containsKey(personKey)) {
                        log.error("【通过OPENID匹配人员】人员身份无法找到对应对人员信息{}", personKey);
                        extracted(domain, occupyDto, "【通过OPENID匹配人员】人员身份无法找到对应对人员信息");
                        continue;
                    }
                    persons = personFromSSOMapByOpenid.get(personKey);
                    if (persons.size() >= 0) {
                        for (Person person : persons) {
                            createOccupyDto(domain, occupyDtoFromUpstream, deptFromSSOMap, postFromSSOMap, rules, upstreamType, upstreams, now, occupies, resultOccupies, occupyDto, person);
                            log.debug("【通过OPENID匹配人员】人员身份找找到人员信息{}", personKey);
                        }
                    }
                    break;
            }

        }
    }

    private ConcurrentHashMap<String, OccupyDto> dealWithSsoOccupy(ArrayList<OccupyDto> deleteFromSSO, List<OccupyDto> occupiesFromSSO) {
        ConcurrentHashMap<String, OccupyDto> concurrentHashMap = new ConcurrentHashMap<>();
        for (OccupyDto occupyDto : occupiesFromSSO) {
            String key = occupyDto.getPersonId() + ":" + occupyDto.getDeptCode() + ":" + occupyDto.getPostCode();
            //已有相同标识的身份
            if (concurrentHashMap.containsKey(key)) {
                OccupyDto occupyDtoFromMap = concurrentHashMap.get(key);
                //当前身份有效
                if (1 == occupyDto.getActive()) {
                    //对比map中身份是否有效
                    if (1 == occupyDtoFromMap.getActive()) {
                        // 均有效则对比是否有工号 usercode
                        if (StringUtils.isNotBlank(occupyDto.getIdentityCardNo())) {
                            if (StringUtils.isNotBlank(occupyDtoFromMap.getIdentityCardNo())) {
                                //均有工号则对比修改时间
                                if (occupyDto.getUpdateTime().isAfter(occupyDtoFromMap.getUpdateTime())) {
                                    //当前数据更新迟于map中
                                    deleteFromSSO.add(occupyDtoFromMap);
                                    concurrentHashMap.put(key, occupyDto);
                                } else {
                                    deleteFromSSO.add(occupyDtoFromMap);
                                }
                            } else {
                                //map中身份无工号
                                deleteFromSSO.add(occupyDtoFromMap);
                                concurrentHashMap.put(key, occupyDto);
                            }
                        } else {
                            //当前身份无工号
                            deleteFromSSO.add(occupyDto);
                        }
                    } else {
                        //map中身份无效
                        deleteFromSSO.add(occupyDtoFromMap);
                        concurrentHashMap.put(key, occupyDto);
                    }
                } else {
                    //当前身份无效
                    deleteFromSSO.add(occupyDto);
                }
            } else {
                concurrentHashMap.put(key, occupyDto);
            }
        }
        return concurrentHashMap;
    }

    private void createOccupyDto(DomainInfo domain, Map<String, OccupyDto> occupyDtoFromUpstream, Map<String, TreeBean> deptFromSSOMap,
                                 Map<String, TreeBean> postFromSSOMap, NodeRules rules, UpstreamType upstreamType, ArrayList<Upstream> upstreams, LocalDateTime now, List<OccupyDto> occupies,
                                 ArrayList<OccupyDto> resultOccupies, OccupyDto oldOccupyDto, Person person) {
        OccupyDto occupyDto = new OccupyDto();
        BeanUtils.copyProperties(oldOccupyDto, occupyDto);
        occupyDto.setOpenId(person.getOpenId());
        occupyDto.setSource(upstreams.get(0).getAppName() + "(" + upstreams.get(0).getAppCode() + ")");
        occupyDto.setPersonId(person.getId());
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
        if (null != upstreamType.getIsIncremental() && upstreamType.getIsIncremental()) {
            occupyDto.setDataSource("INC_PULL");
        } else {
            occupyDto.setDataSource("PULL");
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
        //规则是否启用标识
        occupyDto.setRuleStatus(rules.getActive());
        occupies.add(occupyDto);
        //以人员id岗位及部门code作为键进行身份去重
        String key = person.getId() + ":" + occupyDto.getPostCode() + ":" + occupyDto.getDeptCode();
        if (occupyDtoFromUpstream.containsKey(key)) {
            log.info("权威源人员身份数据合重:{}->{}", occupyDtoFromUpstream.get(key).toString(), occupyDto);
            if (occupyDto.getActive() == 1) {
                occupyDtoFromUpstream.put(key, occupyDto);
                resultOccupies.add(occupyDto);
                extracted(domain, occupyDtoFromUpstream.get(key), "权威源人员身份数据合重");
            } else {
                extracted(domain, occupyDto, "忽略无效数据");
            }
        } else {
            resultOccupies.add(occupyDto);
            occupyDtoFromUpstream.put(key, occupyDto);
        }
    }

    /**
     * 1.对比后删除  不关心start_time及end_time 仅修改删除标识及修改时间
     * 2.对比后失效:
     * start_time: 上游后续不提供start_time字段 , start_time->null  valid_start 不修改
     * 提供字段但值为null ,start_time->null  valid_start 不修改
     * 提供字段并且提供值,start_time->上游值  valid_start 上游值
     * <p>
     * end_time: 上游后续不提供end_time字段, end_time->null  valid_end->当前时刻
     * 提供字段但值为null ,end_time->null  valid_end->当前时刻
     * 提供字段并且提供值,end_time->上游值   valid_end->上游值
     * <p>
     * 3.对比后修改,
     * A: start_time: 上游后续不提供start_time字段 , start_time->null  valid_start 不修改
     * 提供字段但值为null ,start_time->null  valid_start 不修改
     * 提供字段并且提供值,start_time->上游值  valid_start 上游值
     * <p>
     * end_time: 上游后续不提供end_time字段, end_time->null  valid_end->2100
     * 提供字段但值为null ,end_time->null  valid_end->不修改
     * 提供字段并且提供值,end_time->上游值   valid_end->上游值
     * <p>
     * active(此处仅为0->1即失效恢复的情况):valid_start 当前时刻,valid_end 上游不提供字段或null 为2100其余情况取上游值
     * <p>
     * B:映射字段对比无差异,未提供active ,且sso为无效时,valid_start_time->now,若end_time为null则valid_end_time->2100
     * <p>
     * 4.对比后上游丢失失效  仅修改valid_end_time 为当前时刻
     *
     * @param occupyDtoFromUpstream
     * @param result
     * @param key
     * @param occupyFromSSO
     * @param upstreamMap
     * @param preViewOccupyMap
     * @param now
     */
    private void calculate(Map<String, OccupyDto> occupyDtoFromUpstream, Map<String, List<OccupyDto>> result, String key, OccupyDto occupyFromSSO, Map<String, Upstream> upstreamMap, Map<String, OccupyDto> preViewOccupyMap, LocalDateTime now, Map<String, String> attrMap, Map<String, List<DynamicValue>> valueMap, Map<String, DynamicValue> valueUpdateMap, Map<String, DynamicValue> valueInsertMap, Map<String, OccupyDto> keepOccupyMap) {
        // 对比出需要修改的occupy
        if (occupyDtoFromUpstream.containsKey(key) &&
                occupyDtoFromUpstream.get(key).getUpdateTime().isAfter(occupyFromSSO.getUpdateTime())) {
            OccupyDto newOccupy = occupyDtoFromUpstream.get(key);
            //当前数据来源规则为启用再进行处理
            if (newOccupy.getRuleStatus()) {

                //修改标识
                boolean updateFlag = false;
                //删除恢复标识
//                boolean delRecoverFlag = false;
                //del字段标识
                boolean delFlag = false;
                //失效标识
                boolean invalidFlag = false;
                //标识位标识
                boolean timeFlag = false;
                //恢复失效标识
                //   boolean invalidRecoverFlag = true;
                //是否处理扩展字段标识
                boolean dyFlag = true;

                if (!"PULL".equals(occupyFromSSO.getDataSource())) {
                    updateFlag = true;
                }
                occupyFromSSO.setSource(newOccupy.getSource());
                occupyFromSSO.setDataSource(newOccupy.getDataSource());
                //处理sso数据的active为null的情况
                if (null == occupyFromSSO.getActive() || "".equals(occupyFromSSO.getActive())) {
                    occupyFromSSO.setActive(1);
                }
                occupyFromSSO.setRuleStatus(newOccupy.getRuleStatus());
                List<UpstreamTypeField> fields = DataBusUtil.typeFields.get(newOccupy.getUpstreamType());
                if (!occupyTypeFields.containsKey(newOccupy.getUpstreamType())) {
                    occupyTypeFields.put(newOccupy.getUpstreamType(), fields.stream().collect(Collectors.toMap(UpstreamTypeField::getSourceField, UpstreamTypeField::getTargetField)));
                }
                //log.info("----------------人员身份数据对比:sso:{}  ->>  上游:{}", occupyFromSSO, newOccupy);
                if (!newOccupy.getOrphan().equals(occupyFromSSO.getOrphan())) {
                    updateFlag = true;
                    timeFlag = true;
                    //log.info("-------------------orphan:sso:{}->upstream:{}", occupyFromSSO.getOrphan(), newOccupy.getOrphan());
                }

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
                        if (sourceField.equals("active") && (Integer) oldValue == 0 && (Integer) newValue == 1) {
                            log.info("人员身份从失效恢复:{}", occupyFromSSO.getOccupyId());
                            timeFlag = true;
                            continue;
                        }
                        if (sourceField.equals("startTime") || sourceField.equals("endTime")) {
                            log.info("人员身份开始时间,结束时间发生修改:{}", occupyFromSSO.getOccupyId());
                            timeFlag = true;
                            continue;
                        }

                        ClassCompareUtil.setValue(occupyFromSSO, occupyFromSSO.getClass(), sourceField, oldValue, newValue);
                        log.info("人员身份信息更新{}:字段{}：{} --> {}", occupyFromSSO.getOccupyId(), sourceField, oldValue, newValue);

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
                Map<String, String> map = occupyTypeFields.get(newOccupy.getUpstreamType());

                if (delFlag) {
                    if ((null == occupyFromSSO.getRuleStatus() || occupyFromSSO.getRuleStatus()) && (CollectionUtils.isEmpty(upstreamMap) || !upstreamMap.containsKey(occupyFromSSO.getSource()))) {
                        occupyFromSSO.setDelMark(1);
                        occupyFromSSO.setUpdateTime(newOccupy.getUpdateTime());

                        if (result.containsKey("delete")) {
                            result.get("delete").add(occupyFromSSO);
                        } else {
                            result.put("delete", new ArrayList<OccupyDto>() {{
                                this.add(occupyFromSSO);
                            }});
                        }
                        //处理人员预览数据
                        //preViewOccupyMap.remove(occupyFromSSO.getOccupyId());
                        //处理 keep 人员身份数据
                        keepOccupyMap.remove(occupyFromSSO.getOccupyId());
                        log.info("人员身份信息删除{}", occupyFromSSO.getOccupyId());
                    } else {
                        log.info("人员身份信息删除{},但检测到对应权威源已无效或规则为启用,跳过该数据", occupyFromSSO.getOccupyId());
                    }
                }
                if (updateFlag && occupyFromSSO.getDelMark() != 1) {
                    occupyFromSSO.setUpdateTime(newOccupy.getUpdateTime());
                    // 区分出 更新数据  还是 无效数据（上游提供active字段 && 将active变为false）
                    if (invalidFlag) {
                        if ((null == occupyFromSSO.getRuleStatus() || occupyFromSSO.getRuleStatus()) && (CollectionUtils.isEmpty(upstreamMap) || !upstreamMap.containsKey(occupyFromSSO.getSource()))) {
                            //对比后置为失效
                            occupyFromSSO.setActive(0);
                            occupyFromSSO.setActiveTime(now);
                            //上有没有提供startTime
                            if (!map.containsKey("startTime")) {
                                occupyFromSSO.setStartTime(null);
                                //赋默认值
                                occupyFromSSO.setValidStartTime(DEFAULT_START_TIME);
                            } else {
                                occupyFromSSO.setStartTime(newOccupy.getStartTime());
                                occupyFromSSO.setValidStartTime(null != occupyFromSSO.getStartTime() ? occupyFromSSO.getStartTime() : DEFAULT_START_TIME);
                            }
                            //上有没有提供endTime
                            if (!map.containsKey("endTime")) {
                                occupyFromSSO.setEndTime(null);
                                occupyFromSSO.setValidEndTime(now);
                            } else {
                                occupyFromSSO.setEndTime(newOccupy.getEndTime());
                                if (null != newOccupy.getEndTime()) {
                                    occupyFromSSO.setValidEndTime(occupyFromSSO.getEndTime());
                                } else {
                                    occupyFromSSO.setValidEndTime(now);
                                }
                            }
                            checkValidTime(occupyFromSSO, now, true);
                            if (result.containsKey("invalid")) {
                                result.get("invalid").add(occupyFromSSO);
                            } else {
                                result.put("invalid", new ArrayList<OccupyDto>() {{
                                    this.add(occupyFromSSO);
                                }});
                            }
                            //处理人员预览数据
                            //preViewOccupyMap.remove(occupyFromSSO.getOccupyId());
                            //处理 keep 人员身份数据
                            keepOccupyMap.remove(occupyFromSSO.getOccupyId());
                            log.info("人员身份信息失效{}", occupyFromSSO.getOccupyId());
                        } else {
                            log.info("人员身份信息失效{},但检测到对应权威源已无效或规则未启用,跳过该数据", occupyFromSSO.getOccupyId());
                        }
                    } else {
                        log.info("----------------------人员身份对比完映射字段有区别,对标识字段进行对比");
                        //上游未提供active或  提供了active 将数据库数据由无效变为有效
                        //失效标识为false且sso的状态为无效
                        if (timeFlag) {
                            //上有没有提供startTime
                            if (!map.containsKey("startTime")) {
                                occupyFromSSO.setStartTime(null);
                                //赋默认值
                                occupyFromSSO.setValidStartTime(DEFAULT_START_TIME);
                            } else {
                                if (null != newOccupy.getStartTime()) {
                                    occupyFromSSO.setStartTime(newOccupy.getStartTime());
                                    occupyFromSSO.setValidStartTime(newOccupy.getStartTime());

                                } else {
                                    occupyFromSSO.setStartTime(null);
                                    //赋默认值
                                    occupyFromSSO.setValidStartTime(DEFAULT_START_TIME);
                                }
                            }
                            //上有没有提供endTime
                            if (!map.containsKey("endTime")) {
                                occupyFromSSO.setEndTime(null);
                                //赋默认值
                                occupyFromSSO.setValidEndTime(DEFAULT_END_TIME);
                            } else {
                                if (null != newOccupy.getEndTime()) {
                                    occupyFromSSO.setEndTime(newOccupy.getEndTime());
                                    occupyFromSSO.setValidEndTime(newOccupy.getEndTime());
                                } else {
                                    occupyFromSSO.setEndTime(null);
                                    //赋默认值
                                    occupyFromSSO.setValidEndTime(DEFAULT_END_TIME);
                                }
                            }
                            Boolean activeFlag = false;
                            //此处只有0->1 即失效恢复的情况  或因orphan引起的身份变更
                            if (!occupyFromSSO.getActive().equals(newOccupy.getActive())) {
                                activeFlag = true;
                                occupyFromSSO.setActive(newOccupy.getActive());
                                occupyFromSSO.setActiveTime(newOccupy.getUpdateTime());
                                //上游提供的有效期开始时间为过去时间或不提供start_time 则赋值为当前时刻
                                if (null == occupyFromSSO.getStartTime() || occupyFromSSO.getStartTime().isBefore(now)) {
                                    occupyFromSSO.setValidStartTime(now);
                                }
                                occupyFromSSO.setValidEndTime(null == occupyFromSSO.getEndTime() ? DEFAULT_END_TIME : occupyFromSSO.getEndTime());
                            }
                            if (!occupyFromSSO.getOrphan().equals(newOccupy.getOrphan())) {
                                occupyFromSSO.setOrphan(newOccupy.getOrphan());
                                if (0 == occupyFromSSO.getOrphan()) {
                                    activeFlag = true;
                                    //上游提供的有效期开始时间为过去时间则赋值为当前时刻
                                    if (null == occupyFromSSO.getStartTime() || occupyFromSSO.getStartTime().isBefore(now)) {
                                        occupyFromSSO.setValidStartTime(now);
                                    }
                                }
                                occupyFromSSO.setValidEndTime(null == occupyFromSSO.getEndTime() ? DEFAULT_END_TIME : occupyFromSSO.getEndTime());
                            }

                            checkValidTime(occupyFromSSO, now, activeFlag);
                        }
                        if (dyFlag) {
                            //上游的扩展字段
                            Map<String, String> dynamic = newOccupy.getDynamic();
                            List<DynamicValue> dyValuesFromSSO = null;
                            //数据库的扩展字段
                            if (!CollectionUtils.isEmpty(valueMap)) {
                                dyValuesFromSSO = valueMap.get(occupyFromSSO.getOccupyId());
                            }
                            dynamicProcessing(valueUpdateMap, valueInsertMap, attrMap, occupyFromSSO, dynamic, dyValuesFromSSO);
                            dyFlag = false;
                        }
                        ////log.info("人员身份修改了一条数据,修改地:1");
                        //if (result.containsKey("update")) {
                        //    result.get("update").add(occupyFromSSO);
                        //} else {
                        //    result.put("update", new ArrayList<OccupyDto>() {{
                        //        this.add(occupyFromSSO);
                        //    }});
                        //}
                        //log.info("人员身份对比后更新{} --> {}", occupyFromSSO, occupyDtoFromUpstream.get(key));
                        ////处理人员预览数据
                        //preViewOccupyMap.put(occupyFromSSO.getOccupyId(), occupyFromSSO);
                    }


                }
                // 对比后，权威源提供的"映射字段"数据和sso中没有差异。 （active字段不提供） 不提供的默认active为true 即为置为从失效恢复的情况
                if (!updateFlag && occupyFromSSO.getDelMark() != 1) {

                    if (!occupyFromSSO.getActive().equals(newOccupy.getActive())) {
                        occupyFromSSO.setActive(newOccupy.getActive());
                        occupyFromSSO.setActiveTime(newOccupy.getUpdateTime());
                        occupyFromSSO.setUpdateTime(newOccupy.getUpdateTime());
                        occupyFromSSO.setValidStartTime(now);
                        occupyFromSSO.setValidEndTime(null == occupyFromSSO.getEndTime() ? DEFAULT_END_TIME : occupyFromSSO.getEndTime());
                        checkValidTime(occupyFromSSO, now, true);

                        if (dyFlag) {
                            //上游的扩展字段
                            Map<String, String> dynamic = newOccupy.getDynamic();
                            List<DynamicValue> dyValuesFromSSO = null;
                            //数据库的扩展字段
                            if (!CollectionUtils.isEmpty(valueMap)) {
                                dyValuesFromSSO = valueMap.get(occupyFromSSO.getOccupyId());
                            }
                            dynamicProcessing(valueUpdateMap, valueInsertMap, attrMap, newOccupy, dynamic, dyValuesFromSSO);
                            dyFlag = false;
                        }
                        //if (result.containsKey("update")) {
                        //    result.get("update").add(occupyFromSSO);
                        //} else {
                        //    result.put("update", new ArrayList<OccupyDto>() {{
                        //        this.add(occupyFromSSO);
                        //    }});
                        //}
                        ////处理人员预览数据
                        //preViewOccupyMap.put(occupyFromSSO.getOccupyId(), occupyFromSSO);
                        //log.info("人员身份对比后更新{} --> {}", occupyFromSSO, occupyDtoFromUpstream.get(key));

                    }

                }


                if (!dyFlag) {
                    //log.info("人员身份修改了一条数据,修改地:1");
                    if (result.containsKey("update")) {
                        result.get("update").add(occupyFromSSO);
                    } else {
                        result.put("update", new ArrayList<OccupyDto>() {{
                            this.add(occupyFromSSO);
                        }});
                    }
                    log.info("人员身份对比后更新{} --> {}", occupyFromSSO, occupyDtoFromUpstream.get(key));
                    //处理人员预览数据
                    preViewOccupyMap.put(occupyFromSSO.getOccupyId(), occupyFromSSO);
                    //处理 keep 人员身份数据
                    keepOccupyMap.remove(occupyFromSSO.getOccupyId());
                }

                //处理扩展字段对比     修改标识为false则认为主体字段没有差异
                if (!updateFlag && dyFlag) {
                    //上游的扩展字段
                    Map<String, String> dynamic = newOccupy.getDynamic();
                    List<DynamicValue> dyValuesFromSSO = null;
                    //数据库的扩展字段
                    if (!CollectionUtils.isEmpty(valueMap)) {
                        dyValuesFromSSO = valueMap.get(occupyFromSSO.getOccupyId());
                    }
                    Boolean valueFlag = dynamicProcessing(valueUpdateMap, valueInsertMap, attrMap, occupyFromSSO, dynamic, dyValuesFromSSO);

                    if (valueFlag) {
                        //log.info("人员身份修改了一条数据,修改地:1");
                        if (result.containsKey("update")) {
                            result.get("update").add(occupyFromSSO);
                        } else {
                            result.put("update", new ArrayList<OccupyDto>() {{
                                this.add(occupyFromSSO);
                            }});
                        }
                        log.info("人员身份对比后  仅扩展字段有变更 更新{} --> {}", occupyFromSSO, occupyDtoFromUpstream.get(key));
                        //处理人员预览数据
                        preViewOccupyMap.put(occupyFromSSO.getOccupyId(), occupyFromSSO);
                        //处理 keep 人员身份数据
                        keepOccupyMap.remove(occupyFromSSO.getOccupyId());
                    }

                }

            } else {
                log.debug("该身份对应{}", occupyFromSSO.getOccupyId());
            }

            //如果权威源没有,sso有并且来源是pull则置为失效
        } else if (!occupyDtoFromUpstream.containsKey(key)
                && 1 != occupyFromSSO.getDelMark()
                && (null == occupyFromSSO.getActive() || occupyFromSSO.getActive() == 1)
                && "PULL".equalsIgnoreCase(occupyFromSSO.getDataSource())) {
            // 如果sso 有，上游源没有 &&  sso中数据不是删除 && sso数据不是无效
            if ((null == occupyFromSSO.getRuleStatus() || occupyFromSSO.getRuleStatus()) && (CollectionUtils.isEmpty(upstreamMap) || !upstreamMap.containsKey(occupyFromSSO.getSource()))) {
                //上游不再提供置为无效
                occupyFromSSO.setActive(0);
                occupyFromSSO.setActiveTime(now);
                occupyFromSSO.setUpdateTime(now);

                occupyFromSSO.setValidEndTime(LocalDateTime.now());

                if (result.containsKey("invalid")) {
                    result.get("invalid").add(occupyFromSSO);
                } else {
                    result.put("invalid", new ArrayList<OccupyDto>() {{
                        this.add(occupyFromSSO);
                    }});
                }
                //处理 keep 人员身份数据
                keepOccupyMap.remove(occupyFromSSO.getOccupyId());
                //处理人员预览数据
                //preViewOccupyMap.remove(occupyFromSSO.getOccupyId());
                log.info("人员身份对比后上游丢失{}", occupyFromSSO.getOccupyId());
            } else {
                log.info("人员身份对比后上游丢失{},但检测到对应权威源已无效或规则未启用,跳过该数据", occupyFromSSO.getOccupyId());
            }

        }
    }

    /**
     * 1.标识位有效 A:当前时刻不在最终有效期内
     * a:当前时刻小于 valid_start_time 即未来时间,不做修改
     * b:当前时刻大于 valid_end_time  则赋值为2100
     * B:当前时刻在最终有效期内 则不做处理
     * 2.标识为无效 A:当前时刻在最终有效期内  最终有效期结束时间置为当前时刻
     * B:当前时刻不在最终有效期内
     * a:当前时刻小于 valid_start_time 即未来时间,最终有效期开始时间早于当前时刻,不认上游给出的最终有效期开始时间,结束时间都赋值为当前时刻
     * b:当前时刻大于 valid_end_time  最终有效期结束时间早于当前时刻,不认上游给出的最终有效期结束时间,赋值为当前时刻
     *
     * @param occupyFromSSO
     * @param now
     * @param activeFlag    判断有效标识是否发生变更
     * @return
     */
    public static OccupyDto checkValidTime(OccupyDto occupyFromSSO, LocalDateTime now, Boolean activeFlag) {
        //修改
        //当前标识位为有效
        if (occupyFromSSO.getActive() == 1 && occupyFromSSO.getDelMark() == 0 && occupyFromSSO.getOrphan() == 0) {
            //标识位有效,当前时刻不在最终有效期内
            if (now.isBefore(occupyFromSSO.getValidStartTime()) || now.isAfter(occupyFromSSO.getValidEndTime())) {
                //结束时间为过去时间,将结束时间赋值为2100(有效期延长)
                if (!now.isBefore(occupyFromSSO.getValidEndTime())) {
                    occupyFromSSO.setValidEndTime(DEFAULT_END_TIME);
                }
            } else {
                //标识位有效,当前时刻在最终有效期内不需处理
            }
        } else {
            //当前标识位为无效(active=0 失效 或者 del_mark=1 删除  或者 判断为孤儿)

            // 标识位为无效,当前时刻在最终有效期内
            if (!now.isBefore(occupyFromSSO.getValidStartTime()) && !now.isAfter(occupyFromSSO.getValidEndTime())) {
                //因标识为导致的身份无效,修改最终有效期结束时间为当前时刻
                occupyFromSSO.setValidEndTime(now);
            } else {
                //标识位为无效,当前时刻不在最终有效期内(校验数据时效性)

                if (activeFlag) {
                    //最终有效期结束时间早于当前时刻,不认上游给出的最终有效期结束时间,赋值为当前时刻
                    if (occupyFromSSO.getValidEndTime().isBefore(now)) {
                        occupyFromSSO.setValidEndTime(now);
                    }
                }

                //最终有效期开始时间晚于当前时刻,不认上游给出的最终有效期开始时间,赋值为当前时刻(即无效的未来时间)
                if (!occupyFromSSO.getValidStartTime().isBefore(now)) {
                    occupyFromSSO.setValidStartTime(now);
                    occupyFromSSO.setValidEndTime(now);
                }
            }

        }


        return occupyFromSSO;
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

    //    处理异常数据
    private void extracted(DomainInfo domain, OccupyDto occupyDto, String reason) {
        JSONObject jsonObject = JSONObject.parseObject(JSONObject.toJSONString(occupyDto));
        if (occupyErrorData.containsKey(domain.getId())) {
            jsonObject.put("reason", reason);
            jsonObject.put("type", "occupy");
            occupyErrorData.get(domain.getId()).add(jsonObject);
        } else {
            jsonObject.put("reason", reason);
            jsonObject.put("type", "occupy");
            occupyErrorData.put(domain.getId(), new ArrayList<JSONObject>() {{
                this.add(jsonObject);
            }});
        }
        log.warn("租户{}人员身份同步中忽略一条数据{}", domain.getId(), jsonObject);
    }

    @Override
    public OccupyConnection findOccupies(Map<String, Object> arguments, DomainInfo domain) {
        List<OccupyEdge> upstreamDept = new ArrayList<>();
        String upstreamTypeId = (String) arguments.get("upstreamTypeId");
        Integer offset = (Integer) arguments.get("offset");
        Integer first = (Integer) arguments.get("first");
        UpstreamType upstreamType = upstreamTypeService.findById(upstreamTypeId);
        if (null != upstreamType && upstreamType.getIsPage()) {
            Map dataMap = null;
            try {
                dataMap = dataBusUtil.getDataByBus(upstreamType, offset, first, domain);
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
                    if (null != node1.getTimestamp(TreeEnum.UPDATE_TIME.getCode())) {
                        node1.put(TreeEnum.UPDATE_TIME.getCode(), node1.getTimestamp(TreeEnum.UPDATE_TIME.getCode()));
                    }
                    if (null != node1.getTimestamp(TreeEnum.START_TIME.getCode())) {
                        node1.put(TreeEnum.START_TIME.getCode(), node1.getTimestamp(TreeEnum.START_TIME.getCode()));
                    }
                    if (null != node1.getTimestamp(TreeEnum.END_TIME.getCode())) {
                        node1.put(TreeEnum.END_TIME.getCode(), node1.getTimestamp(TreeEnum.END_TIME.getCode()));
                    }
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

    @Override
    public OccupyConnection preViewOccupies(Map<String, Object> arguments, DomainInfo domain) throws Exception {
        Integer i = occupyDao.findOccupyTempCount(null, domain);

        //判断数据库是否有数据
        if (i <= 0) {
            this.reFreshOccupies(arguments, domain, null);
        }
        //根据条件查询
        List<OccupyDto> occupyDtos = occupyDao.findOccupyTemp(arguments, domain);

        OccupyConnection occupyConnection = new OccupyConnection();
        List<OccupyEdge> upstreamDept = new ArrayList<>();
        if (!CollectionUtils.isEmpty(occupyDtos)) {
            //Boolean active = (Boolean) arguments.get("active");
            ////是否有效过滤
            //if (null != active) {
            //    occupyDtos = occupyDtos.stream().filter(occupyDto -> active.equals((occupyDto.getActive() == 1 ? true : false))).collect(Collectors.toList());
            //}
            //Integer offset = (Integer) arguments.get("offset");
            //Integer first = (Integer) arguments.get("first");
            Integer occupyTempCount = occupyDao.findOccupyTempCount(arguments, domain);
            occupyConnection.setTotalCount(occupyTempCount);
            //if (null != offset && null != first) {
            //    occupyDtos = occupyDtos.stream().sorted(Comparator.comparing(OccupyDto::getUpdateTime).thenComparing(OccupyDto::getCreateTime)).skip(offset).limit(first).collect(Collectors.toList());
            //
            //}
            for (OccupyDto occupyDto : occupyDtos) {
                OccupyEdge occupyEdge = new OccupyEdge();
                occupyEdge.setNode(occupyDto);
                upstreamDept.add(occupyEdge);
            }
            occupyConnection.setEdges(upstreamDept);
        }
        return occupyConnection;
    }

    @Override
    public PreViewTask reFreshOccupies(Map<String, Object> arguments, DomainInfo domain, PreViewTask viewTask) {
        ////容器初始化
        //if (null == PersonServiceImpl.preViewTask) {
        //    PersonServiceImpl.preViewTask = new ConcurrentHashMap<>();
        //}
        if (null == viewTask) {
            viewTask = new PreViewTask();
            viewTask.setTaskId(UUID.randomUUID().toString());
            viewTask.setStatus("doing");
            viewTask.setDomain(domain.getId());
            viewTask.setType("occupy");
        }
        //查询进行中的刷新人员身份任务数
        Integer count = preViewTaskService.findByTypeAndStatus("occupy", "doing", domain);

        if (count <= 10) {
            viewTask = preViewTaskService.saveTask(viewTask);
        } else {
            //Optional<String> first = PersonServiceImpl.preViewTask.keySet().stream().findFirst();
            //String s = first.get();
            //if (null != PersonServiceImpl.preViewTask.get(s) && PersonServiceImpl.preViewTask.get(s).getStatus().equals("done")) {
            //    PersonServiceImpl.preViewTask.remove(s);
            //    PersonServiceImpl.preViewTask.put(viewResult.getTaskId(), viewResult);
            //} else {
            throw new CustomException(ResultCode.FAILED, "当前任务数量已达上限,无法创建新的刷新任务,请耐心等待");
            //}

        }

        if (PreViewOccupyThreadPool.executorServiceMap.containsKey(domain.getDomainName())) {
            ExecutorService executorService = PreViewOccupyThreadPool.executorServiceMap.get(domain.getDomainName());
            PreViewTask finalViewTask = viewTask;
            executorService.execute(() -> {

                executePreView(arguments, domain, finalViewTask);

            });
        } else {
            PreViewOccupyThreadPool.builderExecutor(domain.getDomainName());
            reFreshOccupies(arguments, domain, viewTask);
        }

        return viewTask;
    }

    private void executePreView(Map<String, Object> arguments, DomainInfo domain, PreViewTask viewTask) {
        //错误数据容器初始化
        occupyErrorData = new ConcurrentHashMap<>();

        Tenant tenant = tenantDao.findByDomainName(domain.getDomainName());
        if (null == tenant) {
            throw new CustomException(ResultCode.FAILED, "租户不存在");
        }

        // 存储最终需要操作的数据
        Map<String, List<OccupyDto>> result = new HashMap<>();
        //重复需要删除的sso身份数据
        ArrayList<OccupyDto> deleteFromSSO = new ArrayList<>();
        //上游数据,用于异常的数据展示
        Map<String, OccupyDto> occupyDtoFromUpstream = new HashMap<>();


        List<DynamicAttr> dynamicAttrs = dynamicAttrService.findAllByType(TYPE, tenant.getId());
        log.info("获取到当前租户{}的映射字段集为{}", tenant.getId(), dynamicAttrs);

        //扩展字段修改容器
        Map<String, DynamicValue> valueUpdateMap = new ConcurrentHashMap<>();
        //扩展字段新增容器
        Map<String, DynamicValue> valueInsertMap = new ConcurrentHashMap<>();

        List<OccupyDto> occupyDtos;
        try {


            List<NodeDto> nodes = nodeService.findNodes(arguments, domain.getId(), true);
            if (CollectionUtils.isEmpty(nodes)) {
                log.error("无人员身份管理规则信息");
                throw new CustomException(ResultCode.FAILED, "无人员身份管理规则信息");
            }
            List<NodeRulesVo> nodeRules = nodes.get(0).getNodeRules();

            //String nodeId = nodes.get(0).getId();
            //List<NodeRules> occupyRules = rulesService.getByNodeAndType(nodeId, 1, true, 0);
            // 获取所有规则 字段，用于更新验证
            if (CollectionUtils.isEmpty(nodeRules)) {
                log.error("无人员身份管理规则信息");
                throw new CustomException(ResultCode.FAILED, "无人员身份管理规则信息");
            }
            //获取该租户下的当前类型的无效权威源
            ArrayList<Upstream> upstreams = upstreamService.findByDomainAndActiveIsFalse(domain.getId());
            Map<String, Upstream> upstreamMap = new ConcurrentHashMap<>();
            if (!CollectionUtils.isEmpty(upstreams)) {
                upstreamMap = upstreams.stream().collect(Collectors.toMap((upstream -> upstream.getAppName() + "(" + upstream.getAppCode() + ")"), (upstream -> upstream)));
            }
            occupyDtos = dataProcessing(nodeRules, domain, tenant, result, deleteFromSSO, occupyDtoFromUpstream, null, null, valueUpdateMap, valueInsertMap, upstreamMap);
        } catch (Exception e) {
            e.printStackTrace();
            throw new CustomException(ResultCode.FAILED, e.getMessage());
        }
        //存储到临时表(首先清除上次遗留数据)
        occupyDao.removeData(domain);
        Integer i = occupyDao.findOccupyTempCount(null, domain);
        log.info("---------------租户:{},清除人员身份数据完毕:{}", domain.getId(), i);
        occupyDao.saveToTemp(occupyDtos, domain);
        if (null != viewTask) {
            viewTask.setStatus("done");
            viewTask.setUpdateTime(new Timestamp(System.currentTimeMillis()));
            preViewTaskService.saveTask(viewTask);
            log.info("人员身份刷新完毕,任务id为:{}", viewTask.getTaskId());
        }
    }


    public void outdated(Map<Object, Object> personFromSSOMap, Object personKey, Object personKeyByAccount) {
        //优先以证件类型+证件号码 查找身份对应人员是否存在
        /*if (!personFromSSOMap.containsKey(personKey)) {
            //其次以用户名 查找对应的人员是否存在
            if (!personFromSSOMapByAccount.containsKey(personKeyByAccount)) {
                if (!occupiesFromSSOIdentityMap.containsKey(personKeyByIdentity)) {
                    log.error("人员身份无法找到对应对人员信息{}-{}", personKey, personKeyByAccount);
                    extracted(domain, occupyDto, "人员身份无法找到对应对人员信息");
                    continue;
                } else {
                    // 通过 身份证件 找到了对应的身份信息，再根据身份关联信息 反推找到人员信息
                    personId = occupiesFromSSOIdentityMap.get(personKeyByIdentity).getPersonId();
                    occupyDto.setOpenId(occupiesFromSSOIdentityMap.get(personKeyByIdentity).getOpenId());
                    log.info("人员身份通过身份证件获取到人员信息{}", personKeyByIdentity);
                }

            } else {
                personId = personFromSSOMapByAccount.get(personKeyByAccount).getId();
                occupyDto.setOpenId(personFromSSOMapByAccount.get(personKeyByAccount).getOpenId());
                log.info("人员身份通过用户名获取到人员信息{}", personKeyByAccount);
            }

        } else {
            personId = personFromSSOMap.get(personKey).getId();
            occupyDto.setOpenId(personFromSSOMap.get(personKey).getOpenId());
            log.info("人员身份通过人员证件获取到人员信息{}", personKey);
        }*/
    }


    /**
     * @param valueUpdateMap  扩展字段修改map
     * @param valueInsertMap  扩展字段新增map
     * @param attrMap         扩展字段  id 与code  对应map
     * @param ssoBean         sso对象
     * @param dynamic         上游扩展字段
     * @param dyValuesFromSSO sso扩展字段值
     * @return
     */
    private Boolean dynamicProcessing(Map<String, DynamicValue> valueUpdateMap, Map<String, DynamicValue> valueInsertMap, Map<String, String> attrMap, OccupyDto ssoBean, Map<String, String> dynamic, List<DynamicValue> dyValuesFromSSO) {
        Boolean valueFlag = false;
        //扩展字段处理结果集
        ArrayList<DynamicValue> dynValues = new ArrayList<>();
        if (!CollectionUtils.isEmpty(dyValuesFromSSO)) {
            Map<String, DynamicValue> collect = dyValuesFromSSO.stream().collect(Collectors.toMap(DynamicValue::getAttrId, dynamicValue -> dynamicValue));
            for (Map.Entry<String, String> str : attrMap.entrySet()) {
                String o = dynamic.get(str.getValue());
                if (collect.containsKey(str.getKey())) {
                    DynamicValue dynamicValue = collect.get(str.getKey());
                    if (null != o && !o.equals(dynamicValue.getValue())) {
                        log.info("主体{}扩展字段不同{}->{},修改扩展字段", ssoBean.getOccupyId(), dynamicValue.getValue(), o);
                        dynamicValue.setValue(o);
                        //扩展字段预览展示
                        dynamicValue.setKey(dynamicValue.getAttrId());
                        dynamicValue.setCode(str.getValue());
                        dynValues.add(dynamicValue);
                        valueUpdateMap.put(dynamicValue.getAttrId() + "-" + dynamicValue.getEntityId(), dynamicValue);
                        valueFlag = true;
                    } else {
                        //相同则直接放入person
                        //扩展字段预览展示
                        dynamicValue.setKey(dynamicValue.getAttrId());
                        dynamicValue.setCode(str.getValue());
                        dynValues.add(dynamicValue);
                    }
                } else {
                    if (dynamic.containsKey(str.getValue())) {
                        //上游有  数据库没有则新增
                        DynamicValue dynamicValue = new DynamicValue();
                        dynamicValue.setId(UUID.randomUUID().toString());
                        dynamicValue.setValue(o);
                        dynamicValue.setEntityId(ssoBean.getOccupyId());
                        dynamicValue.setAttrId(str.getKey());
                        valueFlag = true;
                        log.info("主体{}扩展字段新增{}", ssoBean.getOccupyId(), o);
                        //扩展字段预览展示
                        dynamicValue.setKey(dynamicValue.getAttrId());
                        dynamicValue.setCode(str.getValue());
                        dynValues.add(dynamicValue);
                        valueInsertMap.put(dynamicValue.getAttrId() + "-" + dynamicValue.getEntityId(), dynamicValue);
                    }
                }
            }
        } else {
            for (Map.Entry<String, String> str : attrMap.entrySet()) {
                if (dynamic.containsKey(str.getValue())) {
                    String o = dynamic.get(str.getValue());
                    valueFlag = true;
                    //上游有  数据库没有则新增
                    DynamicValue dynamicValue = new DynamicValue();
                    dynamicValue.setId(UUID.randomUUID().toString());
                    dynamicValue.setValue(o);
                    dynamicValue.setEntityId(ssoBean.getOccupyId());
                    dynamicValue.setAttrId(str.getKey());
                    log.info("主体{}扩展字段新增{}", ssoBean.getOccupyId(), o);
                    valueInsertMap.put(dynamicValue.getAttrId() + "-" + dynamicValue.getEntityId(), dynamicValue);
                    //扩展字段预览展示
                    dynamicValue.setKey(dynamicValue.getAttrId());
                    dynamicValue.setCode(str.getValue());
                    dynValues.add(dynamicValue);
                }
            }
        }
        ssoBean.setAttrsValues(dynValues);
        return valueFlag;
    }


    @Override
    public PreViewTask testOccupyTask(DomainInfo domain, PreViewTask viewTask) {

        if (null == viewTask) {
            viewTask = new PreViewTask();
            viewTask.setTaskId(UUID.randomUUID().toString());
            viewTask.setStatus("doing");
            viewTask.setDomain(domain.getId());
            viewTask.setType("occupy");
        }

        viewTask = preViewTaskService.saveTask(viewTask);


        if (PreViewOccupyThreadPool.executorServiceMap.containsKey(domain.getDomainName())) {
            ExecutorService executorService = PreViewOccupyThreadPool.executorServiceMap.get(domain.getDomainName());
            PreViewTask finalViewTask = viewTask;
            try {
                executorService.execute(() -> {
                    dealTask(domain, finalViewTask);
                });
            } catch (RejectedExecutionException e) {
                viewTask.setStatus("failed");
                finalViewTask.setReason("当前正在人员身份测试同步中,请稍后再试");
                preViewTaskService.saveTask(viewTask);
                throw new CustomException(ResultCode.FAILED, "当前正在人员身份测试同步中,请稍后再试");
            } catch (Exception e) {
                viewTask.setStatus("failed");
                finalViewTask.setReason(e.getMessage());
                preViewTaskService.saveTask(viewTask);
                throw new CustomException(ResultCode.FAILED, e.getMessage());
            }
        } else {
            PreViewOccupyThreadPool.builderExecutor(domain.getDomainName());
            testOccupyTask(domain, viewTask);
        }

        return viewTask;
    }

    private void dealTask(DomainInfo domain, PreViewTask viewTask) {
        //错误数据容器初始化
        occupyErrorData = new ConcurrentHashMap<>();

        Tenant tenant = tenantDao.findByDomainName(domain.getDomainName());
        if (null == tenant) {
            throw new CustomException(ResultCode.FAILED, "租户不存在");
        }

        // 存储最终需要操作的数据
        Map<String, List<OccupyDto>> result = new HashMap<>();
        //重复需要删除的sso身份数据
        ArrayList<OccupyDto> deleteFromSSO = new ArrayList<>();
        //上游数据,用于异常的数据展示
        Map<String, OccupyDto> occupyDtoFromUpstream = new HashMap<>();
        //增量日志容器
        List<IncrementalTask> incrementalTasks = new ArrayList<>();


        //扩展字段修改容器
        Map<String, DynamicValue> valueUpdateMap = new ConcurrentHashMap<>();
        List<DynamicValue> valueUpdate = new ArrayList<>();
        //扩展字段新增容器
        Map<String, DynamicValue> valueInsertMap = new ConcurrentHashMap<>();
        ArrayList<DynamicValue> valueInsert = new ArrayList<>();


        // 获取规则
        List<NodeDto> nodes = nodeService.findNodes(domain.getId(), 0, "occupy", true);
        if (CollectionUtils.isEmpty(nodes)) {
            log.error("无人员身份管理规则信息");
            throw new CustomException(ResultCode.FAILED, "无人员身份管理规则信息");
        }
        List<NodeRulesVo> nodeRules = nodes.get(0).getNodeRules();

        //String nodeId = nodes.get(0).getId();
        //List<NodeRules> occupyRules = rulesService.getByNodeAndType(nodeId, 1, true, 0);
        // 获取所有规则 字段，用于更新验证
        if (CollectionUtils.isEmpty(nodeRules)) {
            log.error("无人员身份管理规则信息");
            throw new CustomException(ResultCode.FAILED, "无人员身份管理规则信息");
        }
        //获取该租户下的当前类型的无效权威源
        ArrayList<Upstream> upstreams = upstreamService.findByDomainAndActiveIsFalse(domain.getId());
        Map<String, Upstream> upstreamMap = new ConcurrentHashMap<>();
        if (!CollectionUtils.isEmpty(upstreams)) {
            upstreamMap = upstreams.stream().collect(Collectors.toMap((upstream -> upstream.getAppName() + "(" + upstream.getAppCode() + ")"), (upstream -> upstream)));
        }
        dataProcessing(nodeRules, domain, tenant, result, deleteFromSSO, occupyDtoFromUpstream, incrementalTasks, null, valueUpdateMap, valueInsertMap, upstreamMap);

        //数据库重复身份删除
        if (!CollectionUtils.isEmpty(deleteFromSSO)) {
            if (result.containsKey("delete")) {
                result.get("delete").addAll(deleteFromSSO);
            } else {
                result.put("delete", new ArrayList<OccupyDto>() {{
                    this.addAll(deleteFromSSO);
                }});
            }
        }
        if (!CollectionUtils.isEmpty(valueInsertMap)) {
            valueInsert = new ArrayList<>(valueInsertMap.values());
        }
        if (!CollectionUtils.isEmpty(valueUpdateMap)) {
            valueUpdate = new ArrayList<>(valueUpdateMap.values());
        }

        List<DynamicValue> dynamicValues = new ArrayList<>();
        List<DynamicAttr> dynamicAttrs = dynamicAttrService.findAllByType(TYPE, tenant.getId());
        if (!CollectionUtils.isEmpty(dynamicAttrs)) {
            dynamicValues = dynamicValueService.findAllAttrByType(tenant.getId(), TYPE);
        }
        occupyDao.saveToSsoTest(result, tenant.getId(), valueUpdate, valueInsert, dynamicAttrs, dynamicValues);

        if (null != viewTask) {
            //没有变化/新增/删除/修改/无效
            Integer keep = result.containsKey("keep") ? result.get("keep").size() : 0;
            Integer insert = (result.containsKey("insert") ? result.get("insert").size() : 0);
            Integer delete = result.containsKey("delete") ? result.get("delete").size() : 0;
            Integer update = (result.containsKey("update") ? result.get("update").size() : 0);
            Integer invalid = result.containsKey("invalid") ? result.get("invalid").size() : 0;
            String statistics = keep + "/" + insert + "/" + delete + "/" + update + "/" + invalid;
            viewTask.setStatistics(statistics);
            viewTask.setStatus("done");
            viewTask.setUpdateTime(new Timestamp(System.currentTimeMillis()));
            preViewTaskService.saveTask(viewTask);
            log.info("人员身份刷新完毕,任务id为:{}", viewTask.getTaskId());
        }
    }

    @Override
    public IgaOccupyConnection igaOccupy(Map<String, Object> arguments, DomainInfo domain) {
        //查询租户
        Tenant tenant = tenantDao.findByDomainName(domain.getDomainName());
        IgaOccupyConnection igaOccupyConnection = new IgaOccupyConnection();
        //根据条件查询符合条件的人员
        Map<String, Object> occupies = occupyDao.igaOccupy(arguments, tenant);
        //根据人员查询相应的身份
        List<IgaOccupy> list = (List<IgaOccupy>) occupies.get("list");
        Integer count = (Integer) occupies.get("count");
        if (!CollectionUtils.isEmpty(list)) {
            Map<String, IgaOccupy> collect = list.stream().collect(Collectors.toMap(IgaOccupy::getId, c -> c));
            List<OccupyDto> occupyDtos = occupyDao.findOccupyByIdentityId(collect.keySet(), arguments, tenant);

            //查询 扩展字段
            List<DynamicValue> dynamicValues = new ArrayList<>();

            List<DynamicAttr> dynamicAttrs = dynamicAttrService.findAllByTypeIGA(TYPE, tenant.getId());

            if (!CollectionUtils.isEmpty(dynamicAttrs)) {

                //获取扩展value
                List<String> attrIds = dynamicAttrs.stream().map(DynamicAttr::getId).collect(Collectors.toList());

                dynamicValues = dynamicValueService.findAllByAttrIdIGA(attrIds, tenant.getId());
            }
            //扩展字段值分组
            Map<String, List<DynamicValue>> valueMap = new ConcurrentHashMap<>();
            if (!CollectionUtils.isEmpty(dynamicValues)) {
                valueMap = dynamicValues.stream().filter(dynamicValue -> !StringUtils.isBlank(dynamicValue.getEntityId())).collect(Collectors.groupingBy(dynamicValue -> dynamicValue.getEntityId()));
            }
            Map<String, List<DynamicValue>> finalValueMap = valueMap;
            Map<String, List<OccupyDto>> occupyDtoMap = occupyDtos.stream().collect(Collectors.groupingBy(occupyDto -> occupyDto.getPersonId()));
            ArrayList<IgaOccupyEdge> igaOccupyEdges = new ArrayList<>();
            //装配
            for (IgaOccupy igaOccupy : list) {
                IgaOccupyEdge igaOccupyEdge = new IgaOccupyEdge();
                // 装配身份及扩展字段
                List<OccupyDto> occupyDtoList = occupyDtoMap.get(igaOccupy.getId());
                if (!CollectionUtils.isEmpty(occupyDtoList)) {
                    ArrayList<OccupyDto> occupyList = new ArrayList<>();
                    for (OccupyDto occupyDto : occupyDtoList) {
                        if (!CollectionUtils.isEmpty(finalValueMap.get(occupyDto.getOccupyId()))) {
                            List<DynamicValue> dynValues = finalValueMap.get(occupyDto.getOccupyId());
                            occupyDto.setAttrsValues(dynValues);
                        }
                        occupyList.add(occupyDto);
                    }
                    igaOccupy.setPositions(occupyDtoList);
                }
                igaOccupyEdge.setNode(igaOccupy);

                igaOccupyEdges.add(igaOccupyEdge);
            }
            igaOccupyConnection.setEdges(igaOccupyEdges);

        }
        igaOccupyConnection.setTotalCount(count);
        //查询上次同步的时间
        PreViewTask person = preViewTaskService.findByTypeAndUpdateTime("occupy", domain.getId());
        if (null != person) {
            igaOccupyConnection.setUpdateTime(person.getUpdateTime());
        }


        return igaOccupyConnection;
    }

    @Override
    public void saveToSso(Map<String, List<OccupyDto>> octResult, String tenantId, List<DynamicValue> valueUpdate, List<DynamicValue> valueInsert) {
        occupyDao.saveToSso(octResult, tenantId, null, null);
    }

    @Override
    public List<OccupyDto> findAll(String tenantId, String deptCode, String postCode) {
        return occupyDao.findAll(tenantId, deptCode, postCode);
    }
}
