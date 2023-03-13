package com.qtgl.iga.service.impl;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.qtgl.iga.bean.PersonConnection;
import com.qtgl.iga.bean.PersonEdge;
import com.qtgl.iga.bo.*;
import com.qtgl.iga.config.AvatarTaskThreadPool;
import com.qtgl.iga.config.PreViewPersonThreadPool;
import com.qtgl.iga.dao.*;
import com.qtgl.iga.dao.impl.DynamicAttrDaoImpl;
import com.qtgl.iga.dao.impl.DynamicValueDaoImpl;
import com.qtgl.iga.service.*;
import com.qtgl.iga.task.TaskConfig;
import com.qtgl.iga.utils.ClassCompareUtil;
import com.qtgl.iga.utils.DataBusUtil;
import com.qtgl.iga.utils.FileUtil;
import com.qtgl.iga.utils.SuffixUtil;
import com.qtgl.iga.utils.enumerate.ResultCode;
import com.qtgl.iga.utils.enums.TreeEnum;
import com.qtgl.iga.utils.exception.CustomException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
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
public class PersonServiceImpl implements PersonService {


    @Autowired
    CardTypeDao cardTypeDao;
    @Autowired
    TenantDao tenantDao;
    @Autowired
    NodeDao nodeDao;
    @Autowired
    NodeRulesService rulesService;
    @Autowired
    UpstreamTypeDao upstreamTypeDao;
    @Autowired
    UpstreamService upstreamService;
    @Autowired
    DataBusUtil dataBusUtil;
    @Autowired
    PersonDao personDao;
    @Autowired
    NodeRulesCalculationServiceImpl calculationService;
    @Autowired
    DynamicAttrDaoImpl dynamicAttrDao;
    @Autowired
    DynamicValueDaoImpl dynamicValueDao;
    @Autowired
    ConfigService configService;
    @Autowired
    PreViewTaskService preViewTaskService;
    @Autowired
    IncrementalTaskService incrementalTaskService;
    @Resource
    AvatarDao avatarDao;
    @Resource
    FileUtil fileUtil;

    public static ConcurrentHashMap<String, List<JSONObject>> personErrorData = null;
    //
    public static ConcurrentHashMap<String, List<Person>> personPreViewData = null;
    //类型
    private final String TYPE = "USER";
    //加密方式
    private String pwdConfig = "";
    //用户名标识
    private final String ACCOUNT_NO = "USERNAME";
    //证件类型+证件号码
    private final String CARD_TYPE_NO = "CARD_TYPE_NO";
    //仅证件号码
    private final String CARD_NO = "CARD_NO";
    //邮箱
    private final String EMAIL = "EMAIL";
    //手机号
    private final String CELLPHONE = "CELLPHONE";

    /**
     * <p>
     * * 1：根据规则获取所有的 人员数据
     * <p>
     * MapA(123、12X) ： 证件类型+证件号  MapB(X24、XX5)：不提供证件类型+证件号，但提供用户名   MapC(123、X24、XX5)： A提供了用户名的+B
     * 2：验证提供的 证件类型+证件号码  是否 合法（不为空，符合正则）
     * 2.1  合法
     * 2.1.1 合重：
     * A、当前数据是否同时提供了用户名，
     * A-2、判断MapC中是否有相同的用户名，如果有 则（同用户名但证件类型+证件号不一致【一个人多证件，后续可支持，有效覆盖无效数据&后覆盖前】 同时 删除被覆盖数据的MapA or MapB），覆盖同时MapA+1
     * B、没有提供用户名，判断MapA是否有重复数据(谁覆盖谁：有效覆盖无效数据||后覆盖前，被覆盖的数据用户名需要为空，否则认标有用户名的有效数据)。 MapA+1 or (Map-1,MapA+1)。被覆盖的数据如果包含用户名，则删除MapC中记录
     * <p>
     * <p>
     * 2.2 不合法 （提供全不全、不符合正则）
     * 2.2.1 不提供用户名  【记录日志，丢弃数据】
     * 2.2.2 提供了用户名 合重
     * A、判断MapC中是否有重复数据（有效覆盖无效，在都不提供证件类型+证件号前提下  后覆盖前 同时 删除被覆盖数据的MapA or MapB）覆盖同时MapB+1
     * B、如果MapC中无重复 则 MapB+1、MapC+1
     * <p>
     * *
     * * 3：根据上游人员和数据库中人员进行对比
     * * A：新增  上游提供、sso数据库中没有
     * * B：修改  上游和sso对比后字段值有差异
     * * C：删除  上游提供了del_mark
     * * D: 无效  上游曾经提供后，不再提供 OR 上游提供了active
     * * E: 恢复  之前被标记为失效后再通过推送了相同的数据
     * Doing： 一对多的证件。 一个人可以有多个证件；身份证、护照等
     * TODO： 如果有手动合重规则，运算手动合重规则【待确认】
     */

    @Override
    public Map<String, List<Person>> buildPerson(DomainInfo domain, TaskLog taskLog, TaskLog currentTask, List<NodeRules> userRules) throws Exception {
        //错误数据置空
        TaskConfig.errorData.put(domain.getId(), "");
        personErrorData = new ConcurrentHashMap<>();
        Tenant tenant = tenantDao.findByDomainName(domain.getDomainName());
        if (null == tenant) {
            throw new CustomException(ResultCode.FAILED, "租户不存在");
        }

        Map<String, Upstream> upstreamMap = new ConcurrentHashMap<>();
        // 获取规则  (不为sub则获取所有规则)
        if (CollectionUtils.isEmpty(userRules)) {
            Map<String, Object> arguments = new ConcurrentHashMap<>();
            arguments.put("type", "person");
            arguments.put("status", 0);

            List<Node> nodes = nodeDao.findNodes(arguments, domain.getId());
            if (null == nodes || nodes.size() <= 0) {
                log.error("无人员管理规则信息");
                return null;
            }
            String nodeId = nodes.get(0).getId();
            userRules = rulesService.getByNodeAndType(nodeId, 1, null, 0);
            if (null == userRules || userRules.size() == 0) {
                log.error("无人员管理规则信息");
                return null;
            }
            //获取该租户下的当前类型的无效有效权威源
            ArrayList<Upstream> invalidUpstreams = upstreamService.findByDomainAndActiveIsFalse(domain.getId());
            if (!CollectionUtils.isEmpty(invalidUpstreams)) {
                upstreamMap = invalidUpstreams.stream().collect(Collectors.toMap((upstream -> upstream.getAppName() + "(" + upstream.getAppCode() + ")"), (upstream -> upstream)));
            }
        } else {
            //根据规则获取排除的权威源  及补充规则
            Set<String> strings = userRules.stream().collect(Collectors.groupingBy(NodeRules::getUpstreamTypesId)).keySet();

            List<Upstream> upstreams = upstreamService.findByUpstreamTypeIds(new ArrayList<>(strings), domain.getId());


            if (CollectionUtils.isEmpty(upstreams)) {
                log.error("当前sub 任务提供的规则有误请确认:{}", userRules);
                throw new CustomException(ResultCode.FAILED, "当前sub 任务提供的规则有误请确认");
            }
            List<String> ids = upstreams.stream().map(Upstream::getId).collect(Collectors.toList());

            //根据权威源和类型获取需要执行的规则
            userRules = rulesService.findNodeRulesByUpStreamIdAndType(ids, "person", domain.getId(), 0);
            //获取除了该租户以外的所有权威源(用于sub模式)
            ArrayList<Upstream> otherDomains = upstreamService.findByOtherUpstream(new ArrayList<>(), domain.getId());
            if (!CollectionUtils.isEmpty(otherDomains)) {
                upstreamMap = otherDomains.stream().collect(Collectors.toMap((upstream -> upstream.getAppName() + "(" + upstream.getAppCode() + ")"), (upstream -> upstream)));
            }
        }


        //获取密码加密方式
        pwdConfig = configService.getPasswordConfigByTenantIdAndStatusAndPluginNameAndDelMarkIsFalse(tenant.getId(), "ENABLED", "CommonPlugin");

        //扩展字段修改容器
        Map<String, DynamicValue> valueUpdateMap = new ConcurrentHashMap<>();
        List<DynamicValue> valueUpdate = new ArrayList<>();
        //扩展字段新增容器
        Map<String, DynamicValue> valueInsertMap = new ConcurrentHashMap<>();
        ArrayList<DynamicValue> valueInsert = new ArrayList<>();

        // 存储最终需要操作的数据
        Map<String, List<Person>> result = new ConcurrentHashMap<>();
        // 存储最终需要操作的头像数据
        Map<String, List<Avatar>> avatarResult = new ConcurrentHashMap<>();
        //存储头像需要处理的规则
        List<NodeRules> avatarRules = new ArrayList<>();

        dataProcessing(userRules, domain, tenant, valueUpdateMap, valueInsertMap, result, currentTask, avatarResult, upstreamMap, avatarRules);
        // 验证监控规则
        dealMonitorRule(domain, taskLog, tenant, result);

        //处理特征表
        List<Certificate> certificates = null;
        if (!CollectionUtils.isEmpty(result) && !CollectionUtils.isEmpty(result.get("merge"))) {
            certificates = getCertificates(tenant, result);
        }

        if (!CollectionUtils.isEmpty(valueInsertMap)) {
            valueInsert = new ArrayList<>(valueInsertMap.values());
        }
        if (!CollectionUtils.isEmpty(valueUpdateMap)) {
            valueUpdate = new ArrayList<>(valueUpdateMap.values());
        }
        //插入数据库
        personDao.saveToSso(result, tenant.getId(), valueUpdate, valueInsert, certificates);

        //判断是否需要处理头像
        List<Avatar> insertAvatars = avatarResult.get("insert");
        List<Avatar> updateAvatars = avatarResult.get("update");
        if (!CollectionUtils.isEmpty(insertAvatars) || !CollectionUtils.isEmpty(updateAvatars)) {
            executeAvatarTask(domain, tenant, avatarResult, avatarRules);
        }


        return result;
    }

    //验证监控规则
    private void dealMonitorRule(DomainInfo domain, TaskLog taskLog, Tenant tenant, Map<String, List<Person>> result) throws Exception {
        List<Person> personFromSSOList = personDao.getAll(tenant.getId());
        log.info("--------------------开始验证人员监控规则");
        calculationService.monitorRules(domain, taskLog, personFromSSOList.size(), result.get("delete"), result.get("invalid"));
        log.info("--------------------验证人员监控规则结束");
        if (!CollectionUtils.isEmpty(personErrorData.get(domain.getId()))) {
            TaskConfig.errorData.put(domain.getId(), JSONObject.toJSONString(personErrorData.get(domain.getId())));
        }
    }

    //处理合重特征
    private List<Certificate> getCertificates(Tenant tenant, Map<String, List<Person>> result) {
        ArrayList<Certificate> certificates = new ArrayList<>();
        //获取被合重的特征
        List<Certificate> allCard = personDao.getAllCard(tenant.getId());
        Map<String, List<Certificate>> cardMap = null;
        if (!CollectionUtils.isEmpty(allCard)) {
            cardMap = allCard.stream().collect(Collectors.groupingBy(Certificate::getFromIdentityId));
        }
        if (!CollectionUtils.isEmpty(cardMap)) {
            final LocalDateTime now = LocalDateTime.now();
            for (Person person : result.get("merge")) {
                if (cardMap.containsKey(person.getId())) {
                    List<Certificate> oldCertificates = cardMap.get(person.getId());
                    for (Certificate oldCertificate : oldCertificates) {
                        if (ACCOUNT_NO.equals(oldCertificate.getCardType())) {
                            if (StringUtils.isNotBlank(person.getAccountNo()) && !oldCertificate.getCardNo().equals(person.getAccountNo())) {
                                oldCertificate.setCardNo(person.getAccountNo());
                                oldCertificate.setUpdateTime(now);
                                certificates.add(oldCertificate);
                            }

                        } else if (CARD_TYPE_NO.equals(oldCertificate.getCardType())) {
                            //证件类型+证件号码
                            if (StringUtils.isNotBlank(person.getCardNo()) && StringUtils.isNotBlank(person.getCardType()) && (!oldCertificate.getCardNo().equals(person.getCardNo()) || !oldCertificate.getCardType().equals(person.getCardType()))) {
                                oldCertificate.setCardNo(person.getCardNo());
                                oldCertificate.setCardType(person.getCardType());
                                oldCertificate.setUpdateTime(now);
                                certificates.add(oldCertificate);
                            }

                        } else if (CARD_NO.equals(oldCertificate.getCardType())) {
                            //仅证件号码
                            if (StringUtils.isNotBlank(person.getCardNo()) && !oldCertificate.getCardNo().equals(person.getCardNo())) {
                                oldCertificate.setCardNo(person.getCardNo());
                                oldCertificate.setUpdateTime(now);
                                certificates.add(oldCertificate);
                            }

                        } else if (EMAIL.equals(oldCertificate.getCardType())) {
                            //邮箱
                            if (StringUtils.isNotBlank(person.getEmail()) && !oldCertificate.getCardNo().equals(person.getEmail())) {
                                oldCertificate.setCardNo(person.getEmail());
                                oldCertificate.setUpdateTime(now);
                                certificates.add(oldCertificate);
                            }

                        } else if (CELLPHONE.equals(oldCertificate.getCardType())) {
                            //手机号
                            if (StringUtils.isNotBlank(person.getCellphone()) && !oldCertificate.getCardNo().equals(person.getCellphone())) {
                                oldCertificate.setCardNo(person.getCellphone());
                                oldCertificate.setUpdateTime(now);
                                certificates.add(oldCertificate);
                            }

                        }
                    }

                }
            }
        }

        return certificates;
    }

    private void executeAvatarTask(DomainInfo domain, Tenant tenant, Map<String, List<Avatar>> avatarResult, List<NodeRules> userRules) {
        //线程处理头像  tenant入参
        ExecutorService executorService = AvatarTaskThreadPool.executorServiceMap.computeIfAbsent(domain.getDomainName(), AvatarTaskThreadPool::builderExecutor);

        executorService.execute(() -> {
            log.info("---------------------开始处理头像");
            Integer first = 5;
            //Integer offset = 0;
            //有删除则先删除头像
            if (avatarResult.containsKey("delete")) {
                avatarDao.saveToSso(avatarResult, tenant.getId());
            }


            // 所有证件类型
            List<CardType> cardTypes = cardTypeDao.findAllUser(tenant.getId());
            Map<String, CardType> cardTypeMap = cardTypes.stream().collect(Collectors.toMap(CardType::getCardTypeCode, CardType -> CardType));

            List<Avatar> insertAvatars = avatarResult.get("insert");
            List<Avatar> updateAvatars = avatarResult.get("update");
            ArrayList<Avatar> resultAvatars = new ArrayList<>();
            if (!CollectionUtils.isEmpty(insertAvatars)) {
                resultAvatars.addAll(insertAvatars);
            }
            if (!CollectionUtils.isEmpty(updateAvatars)) {
                resultAvatars.addAll(updateAvatars);
            }
            Map<String, List<Avatar>> personCharacteristicResultAvatarMap = new ConcurrentHashMap<>();
            if (!CollectionUtils.isEmpty(resultAvatars)) {
                personCharacteristicResultAvatarMap.putAll(resultAvatars.stream().collect(Collectors.groupingBy(Avatar::getPersonCharacteristic)));
                //获取上游人员及头像信息
                for (NodeRules rules : userRules) {
                    // 通过规则获取数据
                    UpstreamType upstreamType = upstreamTypeDao.findById(rules.getUpstreamTypesId());
                    if (null == upstreamType) {
                        log.error("人员对应拉取节点规则'{}'无有效权威源类型数据", rules);
                        throw new CustomException(ResultCode.NO_UPSTREAM_TYPE, null, null, "人员", rules.getId());
                    }
                    ArrayList<Upstream> upstreams = upstreamService.getUpstreams(upstreamType.getUpstreamId(), domain.getId());
                    if (CollectionUtils.isEmpty(upstreams)) {
                        log.error("人员对应拉取节点规则'{}'无权威源数据", rules.getId());
                        throw new CustomException(ResultCode.NO_UPSTREAM, null, null, "人员", rules.getId());
                    }
                    String personCharacteristic = upstreamType.getPersonCharacteristic();
                    if (personCharacteristicResultAvatarMap.containsKey(personCharacteristic)) {
                        List<Avatar> avatars = personCharacteristicResultAvatarMap.get(personCharacteristic);
                        Map<String, Avatar> collect = new HashMap<>();
                        if (ACCOUNT_NO.equals(personCharacteristic)) {
                            collect = avatars.stream().filter(avatar -> !StringUtils.isBlank(avatar.getAccountNo())).collect(Collectors.toMap(Avatar::getAccountNo, avatar -> avatar, (v1, v2) -> v2));
                        } else if (CARD_NO.equals(personCharacteristic)) {
                            collect = avatars.stream().filter(avatar -> !StringUtils.isBlank(avatar.getCardNo())).collect(Collectors.toMap(Avatar::getCardNo, avatar -> avatar, (v1, v2) -> v2));

                        } else if (CARD_TYPE_NO.equals(personCharacteristic)) {
                            collect = avatars.stream().filter(avatar -> !StringUtils.isBlank(avatar.getCardNo()) && !StringUtils.isBlank(avatar.getCardType())).collect(Collectors.toMap(avatar -> avatar.getCardType() + "_" + avatar.getCardNo(), avatar -> avatar, (v1, v2) -> v2));

                        } else if (EMAIL.equals(personCharacteristic)) {
                            collect = avatars.stream().filter(avatar -> !StringUtils.isBlank(avatar.getEmail())).collect(Collectors.toMap(Avatar::getEmail, avatar -> avatar, (v1, v2) -> v2));

                        } else if (CELLPHONE.equals(personCharacteristic)) {
                            collect = avatars.stream().filter(avatar -> !StringUtils.isBlank(avatar.getCellphone())).collect(Collectors.toMap(Avatar::getCellphone, avatar -> avatar, (v1, v2) -> v2));
                        } else {
                            continue;
                        }
                        Integer totalCount = getDataTotalCountByBus(upstreamType, domain.getDomainName(), upstreams);
                        double ceil = Math.ceil((totalCount + 0.0) / first);
                        //获取总数

                        for (int i = 0; i < ceil; i++) {
                            this.dealWithAvatar(domain, upstreamType, upstreams, collect, cardTypeMap, tenant, i * first, first);
                        }
                    }


                }
                log.info("---------------------处理头像结束");
            } else {
                log.info("本次同步无头像变更");
            }


        });
    }

    private Integer getDataTotalCountByBus(UpstreamType upstreamType, String domainName, ArrayList<Upstream> upstreams) {

        try {
            Integer dataTotalCountByBus = dataBusUtil.getDataTotalCountByBus(upstreamType, domainName);
            return dataTotalCountByBus;
        } catch (CustomException e) {
            e.printStackTrace();
            if (new Long("1085").equals(e.getCode())) {
                throw new CustomException(ResultCode.INVOKE_URL_ERROR, "请求资源地址失败,请检查权威源:" + upstreams.get(0).getAppName() + "(" + upstreams.get(0).getAppCode() + ")" + "下的权威源类型:" + upstreamType.getDescription());
            } else {
                throw e;
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("人员治理中类型 : " + upstreamType.getUpstreamId() + "表达式异常");
            throw new CustomException(ResultCode.PERSON_ERROR, null, null, upstreamType.getDescription(), e.getMessage());
        }
    }

    private void dealWithAvatar(DomainInfo domain, UpstreamType upstreamType, ArrayList<Upstream> upstreams, Map<String, Avatar> avatarMap, Map<String, CardType> cardTypeMap, Tenant tenant, Integer offset, Integer first) {
        String personCharacteristic = upstreamType.getPersonCharacteristic();
        ArrayList<Avatar> avatarList = new ArrayList<>();
        JSONArray dataByBus;
        dataByBus = getDataFromUpstream(domain, upstreamType, upstreams, offset, first);
        //key - value  特征->人员
        Map<String, Person> personFromUpstreamByPersonCharacteristic = new ConcurrentHashMap<>();
        if (!CollectionUtils.isEmpty(dataByBus)) {
            getPersons(domain, cardTypeMap, null, null, null, personFromUpstreamByPersonCharacteristic, upstreamType, null, dataByBus, personCharacteristic);
        }
        if (!CollectionUtils.isEmpty(personFromUpstreamByPersonCharacteristic)) {
            //根据主体对比
            for (Person value : personFromUpstreamByPersonCharacteristic.values()) {

                Avatar avatar = new Avatar();
                if (ACCOUNT_NO.equals(personCharacteristic) && avatarMap.containsKey(value.getAccountNo())) {
                    avatar = avatarMap.get(value.getAccountNo());
                } else if (CARD_NO.equals(personCharacteristic) && avatarMap.containsKey(value.getCardNo())) {
                    avatar = avatarMap.get(value.getCardNo());

                } else if (CARD_TYPE_NO.equals(personCharacteristic) && avatarMap.containsKey(value.getCardType() + "_" + value.getCardNo())) {
                    avatar = avatarMap.get(value.getCardType() + "_" + value.getCardNo());

                } else if (EMAIL.equals(personCharacteristic) && avatarMap.containsKey(value.getEmail())) {
                    avatar = avatarMap.get(value.getEmail());

                } else if (CELLPHONE.equals(personCharacteristic) && avatarMap.containsKey(value.getCellphone())) {
                    avatar = avatarMap.get(value.getCellphone());

                } else {
                    continue;
                }
                if (null != value.getAvatar()) {
                    avatar.setAvatar(value.getAvatar());
                    Integer count = 0;
                    String url = getUrl(domain, avatar, count);
                    if (url == null) {
                        log.error("人员:{}头像上传三次失败,本次同步跳过该头像上传", avatar.getIdentityId());
                        continue;
                    }

                    avatar.setAvatarUrl(url);
                }
                avatarList.add(avatar);

            }

        }
        avatarDao.saveToSso(avatarList, tenant.getId());
    }

    private JSONArray getDataFromUpstream(DomainInfo domain, UpstreamType upstreamType, ArrayList<Upstream> upstreams, Integer offset, Integer first) {
        JSONArray dataByBus;
        try {
            dataByBus = dataBusUtil.getAvatarDataByBus(upstreamType, domain.getDomainName(), offset, first);
        } catch (CustomException e) {
            e.printStackTrace();
            if (new Long("1085").equals(e.getCode())) {
                throw new CustomException(ResultCode.INVOKE_URL_ERROR, "请求资源地址失败,请检查权威源:" + upstreams.get(0).getAppName() + "(" + upstreams.get(0).getAppCode() + ")" + "下的权威源类型:" + upstreamType.getDescription());
            } else {
                throw e;
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("人员治理中类型 : " + upstreamType.getUpstreamId() + "表达式异常");
            throw new CustomException(ResultCode.PERSON_ERROR, null, null, upstreamType.getDescription(), e.getMessage());
        }
        return dataByBus;
    }


    //上传头像
    private String getUrl(DomainInfo domain, Avatar avatar, Integer count) {
        String url = null;
        try {

            String suffix = SuffixUtil.getByteSuffix(avatar.getAvatar());
            url = fileUtil.putFileByGql(avatar.getAvatar(), avatar.getIdentityId() + "_avatar." + suffix, domain);
        } catch (Exception e) {
            e.printStackTrace();
            if (count < 2) {
                count++;
                getUrl(domain, avatar, count);
            }
        }
        return url;
    }

    private List<Person> dataProcessing(List<NodeRules> userRules, DomainInfo domain, Tenant tenant, Map<String, DynamicValue> valueUpdateMap, Map<String, DynamicValue> valueInsertMap, Map<String, List<Person>> result, TaskLog currentTask, Map<String, List<Avatar>> avatarResult, Map<String, Upstream> upstreamMap, List<NodeRules> avatarRules) {
        final LocalDateTime now = LocalDateTime.now();
        List<Person> people = new ArrayList<>();

        //扩展字段id与code对应map
        Map<String, String> attrMap = new ConcurrentHashMap<>();
        Map<String, String> attrReverseMap = new ConcurrentHashMap<>();


        //获取扩展字段列表
        List<String> dynamicCodes = new ArrayList<>();

        List<DynamicValue> dynamicValues = new ArrayList<>();

        List<DynamicAttr> dynamicAttrs = dynamicAttrDao.findAllByType(TYPE, tenant.getId());
        log.info("获取到当前租户{}的映射字段集为{}", tenant.getId(), dynamicAttrs);
        if (!CollectionUtils.isEmpty(dynamicAttrs)) {
            dynamicCodes = dynamicAttrs.stream().map(DynamicAttr::getCode).collect(Collectors.toList());
            //获取扩展value
            List<String> attrIds = dynamicAttrs.stream().map(DynamicAttr::getId).collect(Collectors.toList());

            dynamicValues = dynamicValueDao.findAllByAttrId(attrIds, tenant.getId());
        }

        //扩展字段值分组
        Map<String, List<DynamicValue>> valueMap = new ConcurrentHashMap<>();
        if (!CollectionUtils.isEmpty(dynamicValues)) {
            valueMap = dynamicValues.stream().filter(dynamicValue -> !StringUtils.isBlank(dynamicValue.getEntityId())).collect(Collectors.groupingBy(DynamicValue::getEntityId));
        }
        List<String> finalDynamicCodes = dynamicCodes;
        Map<String, List<DynamicValue>> finalValueMap = valueMap;
        // 所有证件类型
        List<CardType> cardTypes = cardTypeDao.findAllUser(tenant.getId());
        Map<String, CardType> cardTypeMap = cardTypes.stream().collect(Collectors.toMap(CardType::getCardTypeCode, CardType -> CardType));


        //获取sso 删除的人员数据
        List<Person> personDelMarkFromSSOList = personDao.getDelMarkPeople(tenant.getId());
        if (!CollectionUtils.isEmpty(personDelMarkFromSSOList)) {
            people.addAll(personDelMarkFromSSOList);
        }

        // 获取 sso数据 (后续覆盖后覆盖前,)
        List<Person> personFromSSOList = personDao.getAll(tenant.getId());
        if (!CollectionUtils.isEmpty(personFromSSOList)) {
            people.addAll(personFromSSOList);
        }
        //获取  sso被合重的人员数据
        List<Person> distinctPerson = personDao.findDistinctPerson(tenant.getId());
        Map<String, Person> distinctPersonMap = new ConcurrentHashMap<>();
        if (!CollectionUtils.isEmpty(distinctPerson)) {
            distinctPersonMap = distinctPerson.stream().filter(person -> !StringUtils.isBlank(person.getId())).collect(Collectors.toMap(Person::getId, person -> person, (v1, v2) -> v2));
            people.addAll(distinctPerson);
        }

        //  获取所有头像根据人员id分组
        List<Avatar> avatars = avatarDao.findAll(tenant.getId());
        Map<String, Avatar> avatarMap = new ConcurrentHashMap<>();
        if (!CollectionUtils.isEmpty(avatars)) {
            avatarMap = avatars.stream().collect(Collectors.toMap(Avatar::getIdentityId, avatar -> avatar));
        }


        //预置sso扩展字段原始值容器
        Map<String, String> dynamicSSOValues = new ConcurrentHashMap<>();

        Map<String, Person> preViewPersonMap = people.stream().filter(person -> !StringUtils.isBlank(person.getId())).collect(Collectors.toMap(Person::getId, person -> person, (v1, v2) -> v2));
        //预置对比丢失的失效人员
        Map<String, Person> invalidPersonMap = people.stream().filter(person -> !StringUtils.isBlank(person.getId()) && null != person.getValidEndTime() && null != person.getValidStartTime() && now.isBefore(person.getValidEndTime()) && now.isAfter(person.getValidStartTime())).collect(Collectors.toMap(Person::getId, person -> person, (v1, v2) -> v2));
        //预置没有变化的人员   未删除的人员
        Map<String, Person> keepPersonMap = people.stream().filter(person -> !StringUtils.isBlank(person.getId()) && person.getDelMark() != 1).collect(Collectors.toMap(Person::getId, person -> person, (v1, v2) -> v2));

        //预存  sso 已有操作数据的原始备份   key  id ->  person
        Map<String, Person> backUpPersonMap = new ConcurrentHashMap<>();
        //临时存储修改数据容器
        Map<String, Map<String, Person>> tempResult = new HashMap<>();

        //临时存储头像修改数据容器
        Map<String, Map<String, Avatar>> avatarTempResult = new HashMap<>();

        //扩展字段逻辑处理
        if (!CollectionUtils.isEmpty(dynamicAttrs)) {
            attrMap = dynamicAttrs.stream().collect(Collectors.toMap(DynamicAttr::getId, DynamicAttr::getCode));
            attrReverseMap = dynamicAttrs.stream().collect(Collectors.toMap(DynamicAttr::getCode, DynamicAttr::getId));
        }

        Map<String, String> finalAttrMap = attrMap;
        Map<String, String> finalAttrReverseMap = attrReverseMap;

        Map<String, Person> finalDistinctPersonMap = distinctPersonMap;
        Map<String, Avatar> finalAvatarMap = avatarMap;
        userRules.forEach(rules -> {

            //根据人员主体合重的容器
            Map<String, Person> personFromUpstreamByPersonCharacteristic = new ConcurrentHashMap<>();

            // 通过规则获取数据
            UpstreamType upstreamType = upstreamTypeDao.findById(rules.getUpstreamTypesId());
            if (null == upstreamType) {
                log.error("人员对应拉取节点规则'{}'无有效权威源类型数据", rules);
                throw new CustomException(ResultCode.NO_UPSTREAM_TYPE, null, null, "人员", rules.getId());
            }
            ArrayList<Upstream> upstreams = upstreamService.getUpstreams(upstreamType.getUpstreamId(), domain.getId());
            if (CollectionUtils.isEmpty(upstreams)) {
                log.error("人员对应拉取节点规则'{}'无权威源数据", rules.getId());
                throw new CustomException(ResultCode.NO_UPSTREAM, null, null, "人员", rules.getId());
            }

            String source = upstreams.get(0).getAppName() + "(" + upstreams.get(0).getAppCode() + ")";
            JSONArray dataByBus;
            try {
                dataByBus = dataBusUtil.getDataByBus(upstreamType, domain.getDomainName());
            } catch (CustomException e) {
                e.printStackTrace();
                if (new Long("1085").equals(e.getCode())) {
                    throw new CustomException(ResultCode.INVOKE_URL_ERROR, "请求资源地址失败,请检查权威源:" + upstreams.get(0).getAppName() + "(" + upstreams.get(0).getAppCode() + ")" + "下的权威源类型:" + upstreamType.getDescription());
                } else {
                    throw e;
                }
            } catch (Exception e) {
                e.printStackTrace();
                log.error("人员治理中类型 : " + upstreamType.getUpstreamId() + "表达式异常");
                throw new CustomException(ResultCode.PERSON_ERROR, null, null, upstreamType.getDescription(), e.getMessage());
            }
            //获取人员类型合重主体
            String personCharacteristic = upstreamType.getPersonCharacteristic();
            //遍历权威源数据进行数据规范化
            if (null != dataByBus) {
                //校验上游数据并对关键字段赋默认值
                getPersons(domain, cardTypeMap, finalDynamicCodes, now, rules, personFromUpstreamByPersonCharacteristic, upstreamType, source, dataByBus, personCharacteristic);


                if (!CollectionUtils.isEmpty(personFromUpstreamByPersonCharacteristic)) {
                    //权威源类型为增量则添加对应的增量同步日志
                    if (null != currentTask && null != upstreamType.getIsIncremental() && upstreamType.getIsIncremental()) {
                        addIncrementalTask(domain, currentTask, upstreamType, new ArrayList<>(personFromUpstreamByPersonCharacteristic.values()));
                    }

                    log.info("权威源类型:{}所有人员数据获取完成:{}", upstreamType.getId(), personFromUpstreamByPersonCharacteristic.size());

                    ////单个权威源类型本次同步变化数量容器(预留)
                    //ConcurrentHashMap<String, ConcurrentHashMap<String, List<Person>>> upstreamCountMap = new ConcurrentHashMap<>();
                    //对比处理
                    dealWithData(valueUpdateMap, valueInsertMap, result, avatarResult, finalValueMap, dynamicSSOValues, preViewPersonMap, invalidPersonMap, keepPersonMap, backUpPersonMap, tempResult, avatarTempResult, finalAttrMap, upstreamMap, finalDistinctPersonMap, finalAvatarMap, personFromUpstreamByPersonCharacteristic, personCharacteristic, upstreamType.getId(), avatarRules, rules);

                    if (!CollectionUtils.isEmpty(result.get("insert"))) {
                        Map<String, Person> insert = result.get("insert").stream().filter(person -> !StringUtils.isBlank(person.getId())).collect(Collectors.toMap(Person::getId, person -> person, (v1, v2) -> v2));
                        preViewPersonMap.putAll(insert);
                    }

                } else {
                    log.error("权威源类型:{}提供人员数据不符合规范,本次同步跳过该权威源类型", upstreamType.getId());
                }
            }
        });
        //处理纯新增的扩展字段
        if (result.containsKey("insert") && !CollectionUtils.isEmpty(result.get("insert"))) {
            dealDynamicValueInsert(tenant, valueInsertMap, result, finalAttrReverseMap);
        }
        log.info("人员处理结束:扩展字段处理需要修改{},需要新增{}", CollectionUtils.isEmpty(valueUpdateMap) ? 0 : valueUpdateMap.size(), CollectionUtils.isEmpty(valueInsertMap) ? 0 : valueInsertMap.size());
        log.debug("人员处理结束:扩展字段处理需要修改{},需要新增{}", valueInsertMap, valueInsertMap);
        //处理 丢失 失效的数据
        if (!CollectionUtils.isEmpty(invalidPersonMap)) {
            dealInvalidPerson(result, now, distinctPersonMap, preViewPersonMap, invalidPersonMap, upstreamMap);
        }
        // 处理result
        if (!CollectionUtils.isEmpty(keepPersonMap)) {
            if (result.containsKey("keep")) {
                result.get("keep").addAll(new ArrayList<>(keepPersonMap.values()));
            } else {
                result.put("keep", new ArrayList<>(keepPersonMap.values()));
            }

        }
        //处理头像结果集
        if (!CollectionUtils.isEmpty(avatarTempResult)) {
            if (!CollectionUtils.isEmpty(avatarTempResult.get("update"))) {
                if (avatarResult.containsKey("update")) {
                    avatarResult.get("update").addAll(new ArrayList<>(avatarTempResult.get("update").values()));
                } else {
                    avatarResult.put("update", new ArrayList<>(avatarTempResult.get("update").values()));
                }

            }
            if (!CollectionUtils.isEmpty(avatarTempResult.get("insert"))) {
                if (avatarResult.containsKey("insert")) {
                    avatarResult.get("insert").addAll(new ArrayList<>(avatarTempResult.get("insert").values()));
                } else {
                    avatarResult.put("insert", new ArrayList<>(avatarTempResult.get("insert").values()));
                }

            }
            if (!CollectionUtils.isEmpty(avatarTempResult.get("delete"))) {
                if (avatarResult.containsKey("delete")) {
                    avatarResult.get("delete").addAll(new ArrayList<>(avatarTempResult.get("delete").values()));
                } else {
                    avatarResult.put("delete", new ArrayList<>(avatarTempResult.get("delete").values()));
                }

            }
        }

        //处理扩展字段
        if (!CollectionUtils.isEmpty(tempResult)) {
            if (!CollectionUtils.isEmpty(tempResult.get("update"))) {
                if (result.containsKey("update")) {
                    result.get("update").addAll(new ArrayList<>(tempResult.get("update").values()));
                } else {
                    result.put("update", new ArrayList<>(tempResult.get("update").values()));
                }

            }

            if (!CollectionUtils.isEmpty(tempResult.get("invalid"))) {
                if (result.containsKey("invalid")) {
                    result.get("invalid").addAll(new ArrayList<>(tempResult.get("invalid").values()));
                } else {
                    result.put("invalid", new ArrayList<>(tempResult.get("invalid").values()));
                }

            }
            if (!CollectionUtils.isEmpty(tempResult.get("delete"))) {
                if (result.containsKey("delete")) {
                    result.get("delete").addAll(new ArrayList<>(tempResult.get("delete").values()));
                } else {
                    result.put("delete", new ArrayList<>(tempResult.get("delete").values()));
                }

            }

        }

        //处理人员预览数据
        people = new ArrayList<>(preViewPersonMap.values());
        people = people.stream().filter(person -> ((null == person.getDelMark() || person.getDelMark().equals(0)) && person.getActive().equals(1))).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(result.get("insert"))) {
            people.addAll(result.get("insert"));
        }
        return people;
    }

    private void addIncrementalTask(DomainInfo domain, TaskLog currentTask, UpstreamType upstreamType, List<Person> personUpstreamList) {
        List<Person> collect1 = personUpstreamList.stream().sorted(Comparator.comparing(Person::getUpdateTime).reversed()).collect(Collectors.toList());
        IncrementalTask incrementalTask = new IncrementalTask();
        incrementalTask.setId(UUID.randomUUID().toString());
        incrementalTask.setMainTaskId(currentTask.getId());
        incrementalTask.setType("person");
        incrementalTask.setMainTaskId(currentTask.getId());
        log.info("类型:{},权威源类型:{},上游增量最大修改时间:{} -> {},当前时刻:{}", upstreamType.getSynType(), upstreamType.getId(), collect1.get(0).getUpdateTime(), collect1.get(0).getUpdateTime().toInstant(ZoneOffset.ofHours(+8)).toEpochMilli(), System.currentTimeMillis());
        long min = Math.min(collect1.get(0).getUpdateTime().toInstant(ZoneOffset.ofHours(+8)).toEpochMilli(), System.currentTimeMillis());
        incrementalTask.setTime(new Timestamp(min));
        incrementalTask.setUpstreamTypeId(collect1.get(0).getUpstreamType());
        incrementalTaskService.save(incrementalTask, domain);
    }

    /**
     * @param valueUpdateMap                           扩展字段最终修改集
     * @param valueInsertMap                           扩展字段最终新增集
     * @param result                                   最终结果集
     * @param avatarResult                             头像最终结果集
     * @param finalValueMap                            sso扩展字段values
     * @param dynamicSSOValues                         扩展字段临时结果集(用作值回滚)
     * @param preViewPersonMap                         预览人员map
     * @param invalidPersonMap                         失效人员map
     * @param keepPersonMap                            不变的人员map(测试同步使用)
     * @param backUpPersonMap                          人员sso数据存储(用作值回滚)
     * @param tempResult                               临时结果集
     * @param avatarTempResult                         头像临时结果集
     * @param finalAttrMap                             扩展字段
     * @param finalUpstreamMap                         需要忽略的权威源
     * @param finalDistinctPersonMap                   手动合重人员map(用于逻辑兼容)
     * @param finalAvatarMap                           头像map
     * @param personFromUpstreamByPersonCharacteristic 上游数据
     * @param personCharacteristic                     人员特征
     * @param upstreamTypeId                           当前权威源类型id
     */
    private void dealWithData(Map<String, DynamicValue> valueUpdateMap, Map<String, DynamicValue> valueInsertMap, Map<String, List<Person>> result, Map<String, List<Avatar>> avatarResult, Map<String, List<DynamicValue>> finalValueMap, Map<String, String> dynamicSSOValues, Map<String, Person> preViewPersonMap, Map<String, Person> invalidPersonMap, Map<String, Person> keepPersonMap, Map<String, Person> backUpPersonMap, Map<String, Map<String, Person>> tempResult, Map<String, Map<String, Avatar>> avatarTempResult, Map<String, String> finalAttrMap, Map<String, Upstream> finalUpstreamMap, Map<String, Person> finalDistinctPersonMap, Map<String, Avatar> finalAvatarMap, Map<String, Person> personFromUpstreamByPersonCharacteristic, String personCharacteristic, String upstreamTypeId, List<NodeRules> avatarRules, NodeRules rules) {
        //当前权威源类型映射字段
        List<UpstreamTypeField> fields = DataBusUtil.typeFields.get(upstreamTypeId);
        Map<String, UpstreamTypeField> fieldsMap = fields.stream().collect(Collectors.toMap(UpstreamTypeField::getSourceField, sourceFiled -> sourceFiled, (v1, v2) -> v2));
        if (fieldsMap.containsKey("avatar")) {
            avatarRules.add(rules);
        }
        if (ACCOUNT_NO.equals(personCharacteristic)) {
            //todo 性能待优化
            // 用户名
            Map<String, Person> personFromSSOMapByAccount = new ArrayList<>(preViewPersonMap.values()).stream().filter(person ->
                    !StringUtils.isBlank(person.getAccountNo())).sorted(Comparator.comparing(Person::getUpdateTime)).collect(Collectors.toMap(Person::getAccountNo, person -> person, (v1, v2) -> v2));
            personFromSSOMapByAccount.forEach((key, personFromSSO) -> {
                calculate(personFromUpstreamByPersonCharacteristic, result, key, personFromSSO, finalAttrMap, finalValueMap, valueUpdateMap, valueInsertMap, finalUpstreamMap, preViewPersonMap, finalDistinctPersonMap, invalidPersonMap, tempResult, backUpPersonMap, fields, fieldsMap, dynamicSSOValues, keepPersonMap, finalAvatarMap, avatarTempResult, personCharacteristic);
            });
            personFromUpstreamByPersonCharacteristic.forEach((key, val) -> {
                calculateInsert(personFromSSOMapByAccount, result, key, val, avatarResult, personCharacteristic);
            });
        } else if (CARD_TYPE_NO.equals(personCharacteristic)) {
            // 证件类型+证件号码
            // 覆盖逻辑调整
            log.info("---------------------------------start2:" + System.currentTimeMillis());
            Map<String, Person> personFromSSOMapByCardTypeAndNo = new ArrayList<>(preViewPersonMap.values()).stream()
                    .filter(person -> !StringUtils.isBlank(person.getCardType()) && !StringUtils.isBlank(person.getCardNo())).sorted(Comparator.comparing(Person::getUpdateTime))
                    .collect(Collectors.toMap(person -> (person.getCardType() + ":" + person.getCardNo()), person -> person, (v1, v2) -> v2));
            log.info("---------------------------------end2:" + System.currentTimeMillis());

            personFromSSOMapByCardTypeAndNo.forEach((key, personFromSSO) -> {
                calculate(personFromUpstreamByPersonCharacteristic, result, key, personFromSSO, finalAttrMap, finalValueMap, valueUpdateMap, valueInsertMap, finalUpstreamMap, preViewPersonMap, finalDistinctPersonMap, invalidPersonMap, tempResult, backUpPersonMap, fields, fieldsMap, dynamicSSOValues, keepPersonMap, finalAvatarMap, avatarTempResult, personCharacteristic);
            });
            personFromUpstreamByPersonCharacteristic.forEach((key, val) -> {
                calculateInsert(personFromSSOMapByCardTypeAndNo, result, key, val, avatarResult, personCharacteristic);
            });
        } else if (CARD_NO.equals(personCharacteristic)) {
            // 证件号码
            Map<String, Person> personFromSSOMapByCardNo = new ArrayList<>(preViewPersonMap.values()).stream()
                    .filter(person -> !StringUtils.isBlank(person.getCardNo())).sorted(Comparator.comparing(Person::getUpdateTime))
                    .collect(Collectors.toMap(Person::getCardNo, person -> person, (v1, v2) -> v2));

            personFromSSOMapByCardNo.forEach((key, personFromSSO) -> {
                calculate(personFromUpstreamByPersonCharacteristic, result, key, personFromSSO, finalAttrMap, finalValueMap, valueUpdateMap, valueInsertMap, finalUpstreamMap, preViewPersonMap, finalDistinctPersonMap, invalidPersonMap, tempResult, backUpPersonMap, fields, fieldsMap, dynamicSSOValues, keepPersonMap, finalAvatarMap, avatarTempResult, personCharacteristic);
            });
            personFromUpstreamByPersonCharacteristic.forEach((key, val) -> {
                calculateInsert(personFromSSOMapByCardNo, result, key, val, avatarResult, personCharacteristic);
            });

        } else if (EMAIL.equals(personCharacteristic)) {
            // 邮箱
            Map<String, Person> personFromSSOMapByEmail = new ArrayList<>(preViewPersonMap.values()).stream()
                    .filter(person -> !StringUtils.isBlank(person.getEmail())).sorted(Comparator.comparing(Person::getUpdateTime))
                    .collect(Collectors.toMap(Person::getEmail, person -> person, (v1, v2) -> v2));

            personFromSSOMapByEmail.forEach((key, personFromSSO) -> {
                calculate(personFromUpstreamByPersonCharacteristic, result, key, personFromSSO, finalAttrMap, finalValueMap, valueUpdateMap, valueInsertMap, finalUpstreamMap, preViewPersonMap, finalDistinctPersonMap, invalidPersonMap, tempResult, backUpPersonMap, fields, fieldsMap, dynamicSSOValues, keepPersonMap, finalAvatarMap, avatarTempResult, personCharacteristic);
            });
            personFromUpstreamByPersonCharacteristic.forEach((key, val) -> {
                calculateInsert(personFromSSOMapByEmail, result, key, val, avatarResult, personCharacteristic);
            });
        } else if (CELLPHONE.equals(personCharacteristic)) {
            // 手机号
            Map<String, Person> personFromSSOMapByCellphone = new ArrayList<>(preViewPersonMap.values()).stream()
                    .filter(person -> !StringUtils.isBlank(person.getCellphone())).sorted(Comparator.comparing(Person::getUpdateTime))
                    .collect(Collectors.toMap(Person::getCellphone, person -> person, (v1, v2) -> v2));

            personFromSSOMapByCellphone.forEach((key, personFromSSO) -> {
                calculate(personFromUpstreamByPersonCharacteristic, result, key, personFromSSO, finalAttrMap, finalValueMap, valueUpdateMap, valueInsertMap, finalUpstreamMap, preViewPersonMap, finalDistinctPersonMap, invalidPersonMap, tempResult, backUpPersonMap, fields, fieldsMap, dynamicSSOValues, keepPersonMap, finalAvatarMap, avatarTempResult, personCharacteristic);
            });
            personFromUpstreamByPersonCharacteristic.forEach((key, val) -> {
                calculateInsert(personFromSSOMapByCellphone, result, key, val, avatarResult, personCharacteristic);
            });
        }
    }

    private void dealDynamicValueInsert(Tenant tenant, Map<String, DynamicValue> valueInsertMap, Map<String, List<Person>> result, Map<String, String> finalAttrReverseMap) {
        List<Person> insert = result.get("insert");
        for (Person key : insert) {
            ArrayList<DynamicValue> dynamicValueList = new ArrayList<>();
            Map<String, String> dynamic = key.getDynamic();
            if (!CollectionUtils.isEmpty(dynamic)) {
                for (Map.Entry<String, String> str : dynamic.entrySet()) {
                    DynamicValue dynamicValue = new DynamicValue();
                    dynamicValue.setId(UUID.randomUUID().toString());
                    dynamicValue.setValue(str.getValue());
                    dynamicValue.setEntityId(key.getId());
                    dynamicValue.setTenantId(tenant.getId());
                    dynamicValue.setAttrId(finalAttrReverseMap.get(str.getKey()));
                    valueInsertMap.put(dynamicValue.getAttrId() + "-" + dynamicValue.getEntityId(), dynamicValue);
                    //扩展字段预览展示
                    dynamicValue.setKey(dynamicValue.getAttrId());
                    dynamicValue.setCode(str.getValue());
                    dynamicValueList.add(dynamicValue);
                }
            }
            key.setAttrsValues(dynamicValueList);
        }
    }

    private void dealInvalidPerson(Map<String, List<Person>> result, LocalDateTime now, Map<String, Person> distinctPersonMap, Map<String, Person> preViewPersonMap, Map<String, Person> invalidPersonMap, Map<String, Upstream> upstreamMap) {
        ArrayList<Person> invalidPeople = new ArrayList<>(invalidPersonMap.values());
        for (Person invalidPerson : invalidPeople) {

            if (1 != invalidPerson.getDelMark()
                    && (null == invalidPerson.getActive() || invalidPerson.getActive() == 1)
                    && "PULL".equalsIgnoreCase(invalidPerson.getDataSource())) {
                if ((null == invalidPerson.getRuleStatus() || invalidPerson.getRuleStatus()) && (CollectionUtils.isEmpty(upstreamMap) || !upstreamMap.containsKey(invalidPerson.getSource()))) {
                    invalidPerson.setActive(0);
                    invalidPerson.setActiveTime(now);
                    invalidPerson.setUpdateTime(now);
                    invalidPerson.setValidStartTime(OccupyServiceImpl.DEFAULT_START_TIME);
                    invalidPerson.setValidEndTime(OccupyServiceImpl.DEFAULT_START_TIME);
                    if (!CollectionUtils.isEmpty(distinctPersonMap)) {
                        if (distinctPersonMap.containsKey(invalidPerson.getId())) {
                            invalidPerson.setDelMark(1);
                            if (result.containsKey("merge")) {
                                result.get("merge").add(invalidPerson);
                            } else {
                                result.put("merge", new ArrayList<Person>() {{
                                    this.add(invalidPerson);
                                }});
                            }
                        }
                    }
                    if (result.containsKey("invalid")) {
                        result.get("invalid").add(invalidPerson);
                    } else {
                        result.put("invalid", new ArrayList<Person>() {{
                            this.add(invalidPerson);
                        }});
                    }
                    //处理人员预览数据
                    preViewPersonMap.put(invalidPerson.getId(), invalidPerson);

                    log.info("人员对比后上游丢失{}", invalidPerson);
                } else {
                    log.info("人员对比后应置为失效{},但检测到对应权威源已无效或规则未启用,跳过该数据", invalidPerson.getId());
                }
            }


        }
    }

    /**
     * @param domain                                   租户
     * @param cardTypeMap                              用来校验人员证件
     * @param finalDynamicCodes                        扩展字段
     * @param now                                      当前时刻
     * @param rules                                    规则(用来判断规则是否启用)
     * @param personFromUpstreamByPersonCharacteristic key特征 -> 上游数据
     * @param upstreamType                             权威源类型(用来判断是否为增量)
     * @param source                                   来源
     * @param dataByBus                                上游数据
     * @param personCharacteristic                     根据人员特征合重后的上游数据
     */
    private Map<String, Person> getPersons(DomainInfo domain, Map<String, CardType> cardTypeMap, List<String> finalDynamicCodes, LocalDateTime now, NodeRules rules, Map<String, Person> personFromUpstreamByPersonCharacteristic, UpstreamType upstreamType, String source, JSONArray dataByBus, String personCharacteristic) {
        for (Object o : dataByBus) {

            JSONObject personObj = JSON.parseObject(JSON.toJSONString(o));
            if (null != personObj.getTimestamp(TreeEnum.UPDATE_TIME.getCode())) {
                personObj.put(TreeEnum.UPDATE_TIME.getCode(), personObj.getTimestamp(TreeEnum.UPDATE_TIME.getCode()));
            }
            if (null != personObj.getTimestamp(TreeEnum.BIRTHDAY.getCode())) {
                personObj.put(TreeEnum.BIRTHDAY.getCode(), personObj.getTimestamp(TreeEnum.BIRTHDAY.getCode()));
            }
            Person personUpstream = personObj.toJavaObject(Person.class);


            if (null != personUpstream.getActive() && personUpstream.getActive() != 0 && personUpstream.getActive() != 1) {
                extracted(domain, personUpstream, "人员是否有效字段不合法");
                log.error("人员是否有效字段不合法:{}", personUpstream.getActive());
                continue;
            }
            if (null != personUpstream.getDelMark() && personUpstream.getDelMark() != 0 && personUpstream.getDelMark() != 1) {
                extracted(domain, personUpstream, "人员是否删除字段不合法");
                log.error("人员是否删除字段不合法:{}", personUpstream.getDelMark());
                continue;
            }
            // 人员标识 证件类型、证件号码   OR    用户名 accountNo  必提供一个
            if (StringUtils.isBlank(personUpstream.getCardNo()) && StringUtils.isBlank(personUpstream.getCardType())) {
                if (StringUtils.isBlank(personUpstream.getAccountNo())) {
                    extracted(domain, personUpstream, "未提供标识信息:证件类型、证件号码或者用户名");
                    log.error("{}未提供标识信息:证件类型、证件号码或者用户名", personUpstream.getName());
                    continue;
                }

            }
            // 如果提供证件类型,提供的证件号码为空
            if (StringUtils.isBlank(personUpstream.getCardNo()) && !StringUtils.isBlank(personUpstream.getCardType())) {
                extracted(domain, personUpstream, "提供证件类型但对应的证件号码为空");
                log.error("{}-{}提供证件类型但对应的证件号码为空", personUpstream.getCardNo(), personUpstream.getAccountNo());
                continue;
            }
            if (!StringUtils.isBlank(personUpstream.getCardType()) && cardTypeMap.containsKey(personUpstream.getCardType())) {
                String cardTypeReg = cardTypeMap.get(personUpstream.getCardType()).getCardTypeReg();
                if (StringUtils.isNotBlank(cardTypeReg) && !Pattern.matches(cardTypeReg, personUpstream.getCardNo())) {
                    extracted(domain, personUpstream, "证件号码不符合规则");
                    log.error("证件号码不符合规则:{}", personUpstream.getCardNo());
                    continue;
                }
            } else if (!StringUtils.isBlank(personUpstream.getCardType()) && !cardTypeMap.containsKey(personUpstream.getCardType())) {
                extracted(domain, personUpstream, "证件类型无效");
                log.error("证件类型无效:{}", personUpstream.getCardType());
                continue;
            }

            if (StringUtils.isBlank(personUpstream.getName())) {
                extracted(domain, personUpstream, "姓名为空");
                log.error("{}-{}姓名为空", personUpstream.getCardNo(), personUpstream.getAccountNo());
                continue;
            }


            if (null != upstreamType.getIsIncremental() && upstreamType.getIsIncremental()) {
                personUpstream.setDataSource("INC_PULL");
            } else {
                personUpstream.setDataSource("PULL");
            }

            if (null == personUpstream.getUpdateTime()) {
                personUpstream.setUpdateTime(now);
            }
            //如果未提供del,赋予默认值未删除
            if (null == personUpstream.getDelMark()) {
                personUpstream.setDelMark(0);
            }
            //如果未提供active,赋予默认值有效
            if (null == personUpstream.getActive()) {
                personUpstream.setActive(1);
            }
            //赋予activeTime默认值
            personUpstream.setActiveTime(LocalDateTime.now());
            personUpstream.setSource(source);
            personUpstream.setUpstreamType(upstreamType.getId());
            personUpstream.setCreateTime(now);
            //处理扩展字段
            ConcurrentHashMap<String, String> map;
            if (!CollectionUtils.isEmpty(finalDynamicCodes)) {
                map = new ConcurrentHashMap<>();
                for (String dynamicCode : finalDynamicCodes) {
                    if (personObj.containsKey(dynamicCode)) {
                        if (StringUtils.isNotBlank(personObj.getString(dynamicCode))) {
                            map.put(dynamicCode, personObj.getString(dynamicCode));
                        }
                    }
                }
                log.info("处理{}的上游扩展字段值为{}", personObj, map);
                personUpstream.setDynamic(map);
            }
            if (null != rules) {
                personUpstream.setRuleStatus(rules.getActive());
            }
            dealWithUpstreamDataByPersonCharacteristic(domain, personUpstream, personCharacteristic, personFromUpstreamByPersonCharacteristic);
        }
        return personFromUpstreamByPersonCharacteristic;
    }

    private void dealWithUpstreamDataByPersonCharacteristic(DomainInfo domain, Person personUpstream, String personCharacteristic, Map<String, Person> personFromUpstreamByPersonCharacteristic) {
        if (ACCOUNT_NO.equals(personCharacteristic)) {
            //用户名处理
            if (StringUtils.isNotEmpty(personUpstream.getAccountNo())) {

                //并且合重容器中包含该用户名
                if (personFromUpstreamByPersonCharacteristic.containsKey(personUpstream.getAccountNo())) {
                    if (personUpstream.getActive() == 1) {
                        Person person = personFromUpstreamByPersonCharacteristic.get(personUpstream.getAccountNo());
                        if (person.getUpdateTime().isBefore(personUpstream.getUpdateTime())) {
                            log.error("数据丢弃:{},原因 : 数据覆盖", person);
                            extracted(domain, person, "数据覆盖");
                            personFromUpstreamByPersonCharacteristic.put(personUpstream.getAccountNo(), personUpstream);
                        }
                    } else {
                        log.error("数据丢弃:{},原因 : 该标识用户已有数据,忽略无效数据", personUpstream);
                        extracted(domain, personUpstream, "该标识用户已有数据,忽略无效数据");
                    }
                } else {
                    //合重容器中没有对应用户名标识数据,则添加进容器
                    personFromUpstreamByPersonCharacteristic.put(personUpstream.getAccountNo(), personUpstream);
                }

            } else {
                log.error("数据丢弃:{},原因 : 数据没有身份标识,该人员数据指定的身份标识字段为:{}", personUpstream, personCharacteristic);
                extracted(domain, personUpstream, "数据没有身份标识");
            }
        } else if (CARD_TYPE_NO.equals(personCharacteristic)) {
            //证件类型+证件号码
            if (StringUtils.isNotBlank(personUpstream.getCardNo()) && StringUtils.isNotBlank(personUpstream.getCardType())) {

                //合重
                if (personFromUpstreamByPersonCharacteristic.containsKey(personUpstream.getCardType() + ":" + personUpstream.getCardNo())) {
                    //有效则进行覆盖
                    if (personUpstream.getActive() == 1) {
                        Person person = personFromUpstreamByPersonCharacteristic.get(personUpstream.getCardType() + ":" + personUpstream.getCardNo());
                        if (person.getUpdateTime().isBefore(personUpstream.getUpdateTime())) {
                            log.error("数据丢弃:{},原因 : 数据覆盖", person);
                            extracted(domain, person, "数据覆盖");
                            personFromUpstreamByPersonCharacteristic.put(personUpstream.getCardType() + ":" + personUpstream.getCardNo(), personUpstream);
                        }


                    } else {
                        log.error("数据丢弃:{},原因 : 重复标识的无效数据", personUpstream);
                        extracted(domain, personUpstream, "该标识用户已有数据,忽略无效数据");
                    }
                } else {
                    //没有重复的直接放入容器
                    personFromUpstreamByPersonCharacteristic.put(personUpstream.getCardType() + ":" + personUpstream.getCardNo(), personUpstream);
                }
            } else {
                log.error("数据丢弃:{},原因 : 数据没有身份标识,该人员数据指定的身份标识字段为:{}", personUpstream, personCharacteristic);
                extracted(domain, personUpstream, "数据没有身份标识");
            }
        } else if (CARD_NO.equals(personCharacteristic)) {
            //仅证件号码
            if (StringUtils.isNotEmpty(personUpstream.getCardNo())) {

                //并且合重容器中包含该证件号码
                if (personFromUpstreamByPersonCharacteristic.containsKey(personUpstream.getCardNo())) {
                    if (personUpstream.getActive() == 1) {
                        Person person = personFromUpstreamByPersonCharacteristic.get(personUpstream.getCardNo());
                        if (person.getUpdateTime().isBefore(personUpstream.getUpdateTime())) {
                            log.error("数据丢弃:{},原因 : 数据覆盖", person);
                            extracted(domain, person, "数据覆盖");
                            personFromUpstreamByPersonCharacteristic.put(personUpstream.getCardNo(), personUpstream);
                        }

                    } else {
                        log.error("数据丢弃:{},原因 : 该标识用户已有数据,忽略无效数据", personUpstream);
                        extracted(domain, personUpstream, "该标识用户已有数据,忽略无效数据");
                    }
                } else {
                    //合重容器中没有对应证件号码标识数据,则添加进容器
                    personFromUpstreamByPersonCharacteristic.put(personUpstream.getCardNo(), personUpstream);
                }

            } else {
                log.error("数据丢弃:{},原因 : 数据没有身份标识,该人员数据指定的身份标识字段为:{}", personUpstream, personCharacteristic);
                extracted(domain, personUpstream, "数据没有身份标识");
            }
        } else if (EMAIL.equals(personCharacteristic)) {
            //邮箱
            if (StringUtils.isNotEmpty(personUpstream.getEmail())) {

                //并且合重容器中包含该邮箱
                if (personFromUpstreamByPersonCharacteristic.containsKey(personUpstream.getEmail())) {
                    if (personUpstream.getActive() == 1) {
                        Person person = personFromUpstreamByPersonCharacteristic.get(personUpstream.getEmail());
                        if (person.getUpdateTime().isBefore(personUpstream.getUpdateTime())) {
                            log.error("数据丢弃:{},原因 : 数据覆盖", person);
                            extracted(domain, person, "数据覆盖");
                            personFromUpstreamByPersonCharacteristic.put(personUpstream.getEmail(), personUpstream);
                        }
                    } else {
                        log.error("数据丢弃:{},原因 : 该标识用户已有数据,忽略无效数据", personUpstream);
                        extracted(domain, personUpstream, "该标识用户已有数据,忽略无效数据");
                    }
                } else {
                    //合重容器中没有对应邮箱标识数据,则添加进容器
                    personFromUpstreamByPersonCharacteristic.put(personUpstream.getEmail(), personUpstream);
                }

            } else {
                log.error("数据丢弃:{},原因 : 数据没有身份标识,该人员数据指定的身份标识字段为:{}", personUpstream, personCharacteristic);
                extracted(domain, personUpstream, "数据没有身份标识");
            }
        } else if (CELLPHONE.equals(personCharacteristic)) {
            //手机号
            if (StringUtils.isNotEmpty(personUpstream.getCellphone())) {

                //并且合重容器中包含该手机号
                if (personFromUpstreamByPersonCharacteristic.containsKey(personUpstream.getCellphone())) {
                    if (personUpstream.getActive() == 1) {
                        Person person = personFromUpstreamByPersonCharacteristic.get(personUpstream.getCellphone());
                        if (person.getUpdateTime().isBefore(personUpstream.getUpdateTime())) {
                            log.error("数据丢弃:{},原因 : 数据覆盖", person);
                            extracted(domain, person, "数据覆盖");
                            personFromUpstreamByPersonCharacteristic.put(personUpstream.getCellphone(), personUpstream);
                        }


                    } else {
                        log.error("数据丢弃:{},原因 : 该标识用户已有数据,忽略无效数据", personUpstream);
                        extracted(domain, personUpstream, "该标识用户已有数据,忽略无效数据");
                    }
                } else {
                    //合重容器中没有对应手机号标识数据,则添加进容器
                    personFromUpstreamByPersonCharacteristic.put(personUpstream.getCellphone(), personUpstream);
                }

            } else {
                log.error("数据丢弃:{},原因 : 数据没有身份标识,该人员数据指定的身份标识字段为:{}", personUpstream, personCharacteristic);
                extracted(domain, personUpstream, "数据没有身份标识");
            }
        }
    }

    //处理异常数据
    private void extracted(DomainInfo domain, Person personUpstream, String reason) {
        JSONObject jsonObject = JSONObject.parseObject(JSONObject.toJSONString(personUpstream));
        if (personErrorData.containsKey(domain.getId())) {
            jsonObject.put("reason", reason);
            jsonObject.put("type", "person");
            personErrorData.get(domain.getId()).add(jsonObject);
        } else {
            jsonObject.put("reason", reason);
            jsonObject.put("type", "person");
            personErrorData.put(domain.getId(), new ArrayList<JSONObject>() {{
                this.add(jsonObject);
            }});
        }
        log.warn("租户{}人员同步中忽略一条数据{}", domain.getId(), jsonObject);
    }

    /**
     * @param personFromSSOMap
     * @param result
     * @param key
     * @param val
     * @param avatarResult
     * @param personCharacteristic
     */
    private void calculateInsert(Map<String, Person> personFromSSOMap, Map<String, List<Person>> result, String key, Person val, Map<String, List<Avatar>> avatarResult, String personCharacteristic) {
        //sso没有并且未删除标记的数据才进行新增
        if (!personFromSSOMap.containsKey(key) && (val.getDelMark() == 0)) {
            if (val.getRuleStatus()) {
                //新增逻辑字段赋默认值
                String id = UUID.randomUUID().toString();
                val.setId(id);
                val.setOpenId(RandomStringUtils.randomAlphabetic(20));
                val.setValidStartTime(OccupyServiceImpl.DEFAULT_START_TIME);
                val.setValidEndTime(OccupyServiceImpl.DEFAULT_END_TIME);

                // 如果新增的数据 active=0 失效 或者 del_mark=1 删除  或者 判断为孤儿
                //   都将 最终有效期设置为 失效
                if (val.getActive() == 0 || val.getDelMark() == 1) {
                    val.setValidStartTime(OccupyServiceImpl.DEFAULT_START_TIME);
                    val.setValidEndTime(OccupyServiceImpl.DEFAULT_START_TIME);
                }
                // 对新增的用户 判断是否提供 password字段
                if (!StringUtils.isBlank(val.getPassword())) {
                    String password = val.getPassword();
                    //todo加密方式调整
                    password = getPasswordByConfig(pwdConfig, password);
                    val.setPassword(password);
                    if (result.containsKey("password")) {
                        result.get("password").add(val);
                    } else {
                        result.put("password", new ArrayList<Person>() {{
                            this.add(val);
                        }});
                    }
                }
                // 对新增用户判断是否提供 冻结时间
                if (null == val.getFreezeTime()) {
                    //如果上游不提供,则为当前时刻减一天(避免时区问题)
                    val.setFreezeTime(val.getCreateTime().minusDays(1));
                }
                //根据主键判断新增还是修改
                if (id.equals(val.getId())) {
                    if (result.containsKey("insert")) {
                        result.get("insert").add(val);
                    } else {
                        result.put("insert", new ArrayList<Person>() {{
                            this.add(val);
                        }});
                    }
                    //处理头像
                    if (null != val.getAvatar() || null != val.getAvatarUrl()) {
                        Avatar avatar = new Avatar();
                        avatar.setCardNo(val.getCardNo());
                        avatar.setCardType(val.getCardType());
                        avatar.setCellphone(val.getCellphone());
                        avatar.setEmail(val.getEmail());
                        avatar.setAccountNo(val.getAccountNo());
                        avatar.setPersonCharacteristic(personCharacteristic);

                        //avatar.setId(UUID.randomUUID().toString());
                        //avatar.setAvatar(val.getAvatar().getBytes(StandardCharsets.UTF_8));
                        avatar.setAvatarUrl(val.getAvatarUrl());
                        avatar.setAvatarHashCode(val.getAvatarHashCode());
                        avatar.setAvatarUpdateTime(val.getAvatarUpdateTime());
                        avatar.setIdentityId(val.getId());
                        if (avatarResult.containsKey("insert")) {
                            avatarResult.get("insert").add(avatar);
                        } else {
                            avatarResult.put("insert", new ArrayList<Avatar>() {{
                                this.add(avatar);
                            }});
                        }
                    }
                    log.debug("人员对比后新增{}", val);
                } else {
                    if (result.containsKey("update")) {
                        result.get("update").add(val);
                    } else {
                        result.put("update", new ArrayList<Person>() {{
                            this.add(val);
                        }});
                    }
                    log.debug("人员对比后修改{}", val);
                }


            } else {
                log.debug("人员对比后应新增{},但其对应规则未启用,本次跳过该数据", val);
            }
        }
    }

    /**
     * @param personFromUpstream   上游数据
     * @param result               最终结果集
     * @param key
     * @param personFromSSO        sso数据
     * @param attrMap              扩展字段
     * @param valueMap             扩展字段值
     * @param valueUpdateMap       扩展字段需要修改的value
     * @param valueInsertMap       扩展字段需要新增的value
     * @param upstreamMap          需要忽略的权威源
     * @param preViewPersonMap     预览人员map
     * @param distinctPersonMap    手动合重人员map
     * @param invalidPersonMap     失效人员map
     * @param tempResult           临时结果集  防止多源头提供同一数据多次处理的情况
     * @param backUpPersonMap      配合临时结果集回滚当前标识人员为数据库数据
     * @param fields               映射字段
     * @param fieldMap
     * @param dynamicSSOValues     扩展字段临时结果集
     * @param keepPersonMap        不变的人员map
     * @param avatarMap            头像map
     * @param avatarTempResult     头像临时结果集
     * @param personCharacteristic 冗余头像人员特征方便对比
     */
    private void calculate(Map<String, Person> personFromUpstream, Map<String, List<Person>> result, String key, Person personFromSSO, Map<String, String> attrMap, Map<String, List<DynamicValue>> valueMap, Map<String, DynamicValue> valueUpdateMap, Map<String, DynamicValue> valueInsertMap, Map<String, Upstream> upstreamMap, Map<String, Person> preViewPersonMap, Map<String, Person> distinctPersonMap, Map<String, Person> invalidPersonMap, Map<String, Map<String, Person>> tempResult, Map<String, Person> backUpPersonMap, List<UpstreamTypeField> fields, Map<String, UpstreamTypeField> fieldMap, Map<String, String> dynamicSSOValues, Map<String, Person> keepPersonMap, Map<String, Avatar> avatarMap, Map<String, Map<String, Avatar>> avatarTempResult, String personCharacteristic) {
        // 对比出需要修改的person
        if (personFromUpstream.containsKey(key)) {
            //上游包含该数据则将该数据从失效map中移除
            invalidPersonMap.remove(personFromSSO.getId());
            Person newPerson = personFromUpstream.get(key);

            if (!personFromUpstream.get(key).getUpdateTime().isBefore(personFromSSO.getUpdateTime())) {


                if (backUpPersonMap.containsKey(personFromSSO.getId())) {
                    personFromSSO = backUpPersonMap.get(personFromSSO.getId());
                }

                //判断是否都为已删除的数据并且不是合重导致的删除(是则跳过)
                if (1 == newPerson.getDelMark() && 1 == personFromSSO.getDelMark() && (CollectionUtils.isEmpty(distinctPersonMap) || (!CollectionUtils.isEmpty(distinctPersonMap) && !distinctPersonMap.containsKey(newPerson.getId())))) {
                    log.error("人员上游: {}-> sso:{}上游和sso都为删除数据,同步跳过该数据", newPerson, personFromSSO);

                } else {
                    //保证多源提供同一数据时,每次同步仅有一条操作记录
                    if (backUpPersonMap.containsKey(personFromSSO.getId())) {
                        //personFromSSO = backUpPersonMap.get(personFromSSO.getId());

                        //同时清空之前的对应操作
                        //如果之前源头对比有操作,则将其移除以保证单条数据同步时仅有一次有效操作
                        if (tempResult.containsKey("invalid")) {
                            tempResult.get("invalid").remove(personFromSSO.getId());
                        }
                        if (tempResult.containsKey("update")) {
                            log.info("人员同步中获取的{}->{},被后续数据覆盖", personFromSSO.getId(), personFromSSO);
                            tempResult.get("update").remove(personFromSSO.getId());
                        }
                        if (tempResult.containsKey("delete")) {
                            tempResult.get("delete").remove(personFromSSO.getId());
                        }
                        //人员头像
                        if (avatarTempResult.containsKey("update")) {
                            avatarTempResult.get("update").remove(personFromSSO.getId());
                        }
                        if (avatarTempResult.containsKey("delete")) {
                            avatarTempResult.get("delete").remove(personFromSSO.getId());
                        }

                    } else {

                        //Person clone = (Person) personFromSSO.clone();
                        backUpPersonMap.put(personFromSSO.getId(), personFromSSO);
                    }


                    //当前数据来源规则为启用再进行处理
                    if (newPerson.getRuleStatus()) {
                        ////修改是否合法标识
                        //boolean licitFlag = true;
                        //修改标识
                        boolean updateFlag = false;
                        //del字段标识
                        boolean delFlag = false;
                        //失效标识
                        boolean invalidFlag = false;
                        //密码设置
                        boolean passwordFlag = false;
                        //恢复失效标识
                        // boolean invalidRecoverFlag = true;
                        //是否处理扩展字段标识
                        boolean dyFlag = true;

                        //使用sso的对象,将需要修改的值赋值
                        if (!"PULL".equals(personFromSSO.getDataSource()) && !"INC_PULL".equals(personFromSSO.getDataSource())) {
                            updateFlag = true;
                        }
                        //personFromSSO.setDataSource(newPerson.getDataSource());
                        //personFromSSO.setSource(newPerson.getSource());
                        //personFromSSO.setUpstreamType(newPerson.getUpstreamType());
                        //规则启用标识传递
                        //personFromSSO.setRuleStatus(newPerson.getRuleStatus());
                        //处理sso数据的active为null的情况
                        if (null == personFromSSO.getActive() || "".equals(personFromSSO.getActive())) {
                            personFromSSO.setActive(1);
                        }
                        //List<UpstreamTypeField> fields = DataBusUtil.typeFields.get(newPerson.getUpstreamType());
                        //旧值对比覆盖
                        Map<String, Map<String, Object>> oldValueMap = ClassCompareUtil.compareObject(personFromSSO, newPerson);
                        if (!CollectionUtils.isEmpty(oldValueMap)) {
                            for (String k : oldValueMap.keySet()) {
                                if (fieldMap.containsKey(k)) {
                                    continue;
                                }
                                if (k.equals("updateTime") || k.equals("ruleStatus") || k.equals("attrsValues") || k.equals("createSource") || k.equals("createDataSource")
                                        || k.equals("dynamic") || k.equals("password") || k.equals("freezeTime") || k.equals("validEndTime") || k.equals("validStartTime")
                                        || k.equals("upstreamType") || k.equals("delMark") || k.equals("createTime") || k.equals("activeTime") || k.equals("source")
                                        || k.equals("dataSource") || k.equals("tenantId") || k.equals("active")) {
                                    continue;
                                }

                                ClassCompareUtil.setValue(newPerson, personFromSSO.getClass(), k, oldValueMap.get(k).get("newValue"), oldValueMap.get(k).get("oldValue"));

                            }
                        }
                        // 如果字段上游不提供，则不进行更新
                        //    字段值没有发生改变，不进行更新
                        if (null != fields && fields.size() > 0) {
                            for (UpstreamTypeField field : fields) {
                                String sourceField = field.getSourceField();
                                Object newValue = ClassCompareUtil.getGetMethod(newPerson, sourceField);
                                Object oldValue = ClassCompareUtil.getGetMethod(personFromSSO, sourceField);
                                //跳过头像相关字段
                                if (sourceField.equalsIgnoreCase("avatar") || sourceField.equalsIgnoreCase("avatarUrl") ||
                                        sourceField.equalsIgnoreCase("avatarHashCode") || sourceField.equalsIgnoreCase("avatarUpdateTime")) {
                                    continue;
                                }
                                //对于密码字段不处理
                                if (sourceField.equalsIgnoreCase("password")) {
                                    if (null == oldValue && null != newValue) {
                                        // 加密方式调整
                                        String password = getPasswordByConfig(pwdConfig, newValue);
                                        //String password = "{MD5}" + Base64.encodeBase64String(Hex.decodeHex(DigestUtils.md5DigestAsHex(((String) newValue).getBytes()).toCharArray()));
                                        //personFromSSO.setPassword(password);
                                        newPerson.setPassword(password);
                                        if (result.containsKey("password")) {
                                            result.get("password").add(newPerson);
                                        } else {
                                            ArrayList<Person> people = new ArrayList<>();
                                            people.add(newPerson);
                                            result.put("password", people);
                                        }
                                    }
                                    continue;
                                }
                                if (null == oldValue && null == newValue) {
                                    continue;
                                }
                                if (null != oldValue && oldValue.equals(newValue)) {
                                    continue;
                                }
//                        if (sourceField.equals("delMark") && (Integer) oldValue == 1 && (Integer) newValue == 0) {
//                            delFlag = true;
//                            log.info("人员信息{}从删除恢复", personFromSSOList.getId());
//                        }
//
//                    if (sourceField.equalsIgnoreCase("accountNo") && null != newValue && StringUtils.isNotEmpty(newValue.toString()) && personFromSSOMapByAccountAll.containsKey(newValue)) {
//                        Person person = personFromSSOMapByAccountAll.get(newValue);
//                        if (!newPerson.getCardType().equals(person.getCardType()) || !newPerson.getCardNo().equals(person.getCardNo())) {
//
//                            extracted(domainInfo, person, "该修改使用户名下有不同证件类型的数据,请检查");
//                            extracted(domainInfo, newPerson, "该修改使用户名下有不同证件类型的数据,请检查");
//                            //有效且同一用户名对应不同证件类型,目前不支持
//                            log.error("该修改使用户名{}下有不同证件类型的数据{}{},请检查", person.getAccountNo(), person, newPerson);
//                            licitFlag = false;
//                            personFromUpstream.remove(key);
//                            personFromSSOMapCopy.remove(key);
//                            break;
//                        }
//                    }

                                //合重删除特殊处理
                                if (sourceField.equals("delMark") && (Integer) oldValue == 1 && (Integer) newValue == 0) {
                                    if (!CollectionUtils.isEmpty(distinctPersonMap) && distinctPersonMap.containsKey(newPerson.getId())) {
                                        //临时赋值
                                        newPerson.setDelMark(0);
                                        continue;
                                    }
                                }
                                if (sourceField.equalsIgnoreCase("delMark") && (Integer) oldValue == 0 && (Integer) newValue == 1) {
                                    delFlag = true;
                                    log.info("人员信息{}删除", newPerson.getId());
                                    continue;
                                }

                                updateFlag = true;
                                if (sourceField.equalsIgnoreCase("active") && (Integer) oldValue == 1 && (Integer) newValue == 0) {
                                    invalidFlag = true;
                                    log.info("人员信息{}失效", newPerson.getId());
                                    // continue;
                                }
                                if (sourceField.equalsIgnoreCase("active") && (Integer) oldValue == 0 && (Integer) newValue == 1) {

                                    log.info("人员信息{}从失效恢复", newPerson.getId());
                                    continue;
                                }
                                if (sourceField.equalsIgnoreCase("password") && null != newValue) {
                                    //   if (StringUtils.isBlank((String) oldValue) && !StringUtils.isBlank((String) newValue)) {
                                    //加密方式调整
                                    String password = getPasswordByConfig(pwdConfig, newValue);

                                    //String password = "{MD5}" + Base64.encodeBase64String(Hex.decodeHex(DigestUtils.md5DigestAsHex(((String) newValue).getBytes()).toCharArray()));
                                    passwordFlag = true;
                                    newPerson.setPassword(password);
                                    continue;
                                    // }
                                }
                  /*  if (sourceField.equalsIgnoreCase("active") && (Integer) oldValue == 0 && (Integer) newValue == 1) {
                        invalidRecoverFlag = false;
                    }*/

                                //ClassCompareUtil.setValue(personFromSSO, personFromSSO.getClass(), sourceField, oldValue, newValue);
                                log.info("人员信息更新{}:字段{}：{} -> {}", newPerson.getId(), sourceField, oldValue, newValue);

                            }
                        }

                        //if (licitFlag) {
                        if (delFlag) {
                            if ((null == personFromSSO.getRuleStatus() || personFromSSO.getRuleStatus()) && (CollectionUtils.isEmpty(upstreamMap) || !upstreamMap.containsKey(personFromSSO.getSource()))) {
                                //personFromSSO.setDelMark(1);
                                //personFromSSO.setUpdateTime(newPerson.getUpdateTime());
                                //personFromSSO.setValidStartTime(OccupyServiceImpl.DEFAULT_START_TIME);
                                //personFromSSO.setValidEndTime(OccupyServiceImpl.DEFAULT_START_TIME);
                                newPerson.setDelMark(1);
                                newPerson.setValidStartTime(OccupyServiceImpl.DEFAULT_START_TIME);
                                newPerson.setValidEndTime(OccupyServiceImpl.DEFAULT_START_TIME);
                                //处理合重数据
                                if (!CollectionUtils.isEmpty(distinctPersonMap)) {
                                    if (distinctPersonMap.containsKey(newPerson.getId())) {
                                        if (result.containsKey("merge")) {
                                            result.get("merge").add(newPerson);
                                        } else {
                                            ArrayList<Person> people = new ArrayList<>();
                                            people.add(newPerson);
                                            result.put("merge", people);
                                        }
                                    }
                                }
                                //if (result.containsKey("delete")) {
                                //    result.get("delete").add(newPerson);
                                //} else {
                                //    ArrayList<Person> people = new ArrayList<>();
                                //    people.add(newPerson);
                                //    result.put("delete", people);
                                //}
                                if (tempResult.containsKey("delete")) {
                                    tempResult.get("delete").put(newPerson.getId(), newPerson);
                                } else {
                                    ConcurrentHashMap<String, Person> hashMap = new ConcurrentHashMap<>();
                                    hashMap.put(newPerson.getId(), newPerson);
                                    tempResult.put("delete", hashMap);
                                }
                                //处理人员预览数据
                                //preViewPersonMap.remove(personFromSSO.getId());
                                //处理keep人员 数据
                                keepPersonMap.remove(personFromSSO.getId());
                                log.info("人员信息删除{}", newPerson.getId());
                            } else {
                                log.info("人员对比后应删除{},但检测到对应权威源已无效或规则未启用,跳过该数据", newPerson.getId());
                            }
                        }
                        if (updateFlag && personFromSSO.getDelMark() != 1) {
                            //if (updateFlag) {
                            //personFromSSO.setSource(newPerson.getSource());
                            //personFromSSO.setUpdateTime(newPerson.getUpdateTime());
                            // 需要设置人员密码
                            if (passwordFlag) {
                                if (result.containsKey("password")) {
                                    result.get("password").add(newPerson);
                                } else {
                                    ArrayList<Person> people = new ArrayList<>();
                                    people.add(newPerson);
                                    result.put("password", people);
                                }
                            }
                            //失效
                            if (invalidFlag) {
                                if ((null == personFromSSO.getRuleStatus() || personFromSSO.getRuleStatus()) && (CollectionUtils.isEmpty(upstreamMap) || !upstreamMap.containsKey(personFromSSO.getSource()))) {
                                    //personFromSSO.setActive(0);
                                    //personFromSSO.setActiveTime(newPerson.getUpdateTime());
                                    //personFromSSO.setValidStartTime(OccupyServiceImpl.DEFAULT_START_TIME);
                                    //personFromSSO.setValidEndTime(OccupyServiceImpl.DEFAULT_START_TIME);
                                    newPerson.setActive(0);
                                    newPerson.setValidStartTime(OccupyServiceImpl.DEFAULT_START_TIME);
                                    newPerson.setValidEndTime(OccupyServiceImpl.DEFAULT_START_TIME);
                                    if (!CollectionUtils.isEmpty(distinctPersonMap)) {
                                        if (distinctPersonMap.containsKey(newPerson.getId())) {
                                            newPerson.setDelMark(1);
                                            if (result.containsKey("merge")) {
                                                result.get("merge").add(newPerson);
                                            } else {
                                                ArrayList<Person> people = new ArrayList<>();
                                                people.add(newPerson);
                                                result.put("merge", people);
                                            }
                                        }
                                    }
                                    //if (result.containsKey("invalid")) {
                                    //    result.get("invalid").add(personFromSSO);
                                    //} else {
                                    //    result.put("invalid", new ArrayList<Person>() {{
                                    //        this.add(personFromSSO);
                                    //    }});
                                    //}

                                    if (tempResult.containsKey("invalid")) {
                                        tempResult.get("invalid").put(newPerson.getId(), newPerson);
                                    } else {
                                        ConcurrentHashMap<String, Person> hashMap = new ConcurrentHashMap<>();
                                        hashMap.put(newPerson.getId(), newPerson);
                                        tempResult.put("invalid", hashMap);
                                    }

                                    //处理人员预览数据
                                    //preViewPersonMap.remove(personFromSSO.getId());
                                    //处理keep人员 数据
                                    keepPersonMap.remove(personFromSSO.getId());
                                    log.info("人员对比后置为失效{}", newPerson.getId());
                                } else {
                                    log.info("人员对比后应置为失效{},但检测到对应权威源已无效或规则未启用,跳过该数据", newPerson.getId());
                                }
                            } else {
                                //if (!personFromSSO.getActive().equals(newPerson.getActive())) {
                                //    //personFromSSO.setActive(newPerson.getActive());
                                //    //personFromSSO.setActiveTime(newPerson.getUpdateTime());
                                //}
                                //if (personFromSSO.getActive() == 0 || personFromSSO.getDelMark() == 1) {
                                //    personFromSSO.setValidStartTime(OccupyServiceImpl.DEFAULT_START_TIME);
                                //    personFromSSO.setValidEndTime(OccupyServiceImpl.DEFAULT_START_TIME);
                                //}
                                if (newPerson.getActive() == 0 || newPerson.getDelMark() == 1) {
                                    newPerson.setValidStartTime(OccupyServiceImpl.DEFAULT_START_TIME);
                                    newPerson.setValidEndTime(OccupyServiceImpl.DEFAULT_START_TIME);
                                }
                                setValidTime(newPerson);


                                //if (result.containsKey("update")) {
                                //    result.get("update").add(personFromSSO);
                                //} else {
                                //    result.put("update", new ArrayList<Person>() {{
                                //        this.add(personFromSSO);
                                //    }});
                                //}
                                if (dyFlag) {
                                    //上游的扩展字段
                                    Map<String, String> dynamic = newPerson.getDynamic();
                                    List<DynamicValue> dyValuesFromSSO = null;
                                    //数据库的扩展字段
                                    if (!CollectionUtils.isEmpty(valueMap)) {
                                        dyValuesFromSSO = valueMap.get(newPerson.getId());
                                    }
                                    dynamicProcessing(valueUpdateMap, valueInsertMap, attrMap, newPerson, dynamic, dyValuesFromSSO, dynamicSSOValues);
                                    dyFlag = false;
                                }
                            }

                        }

                        // 对比后，权威源提供的"映射字段"数据和sso中没有差异。（active字段不提供）
                        if (!updateFlag && personFromSSO.getDelMark() != 1) {
                            //if (!updateFlag) {

                            if (!personFromSSO.getActive().equals(newPerson.getActive())) {
                                //personFromSSO.setActive(newPerson.getActive());
                                //personFromSSO.setActiveTime(newPerson.getUpdateTime());
                                //personFromSSO.setUpdateTime(newPerson.getUpdateTime());
                                newPerson.setActiveTime(newPerson.getUpdateTime());
                                setValidTime(newPerson);
                                if (dyFlag) {
                                    //上游的扩展字段
                                    Map<String, String> dynamic = newPerson.getDynamic();
                                    List<DynamicValue> dyValuesFromSSO = null;
                                    //数据库的扩展字段
                                    if (!CollectionUtils.isEmpty(valueMap)) {
                                        dyValuesFromSSO = valueMap.get(newPerson.getId());
                                    }
                                    dynamicProcessing(valueUpdateMap, valueInsertMap, attrMap, newPerson, dynamic, dyValuesFromSSO, dynamicSSOValues);
                                    dyFlag = false;
                                }


                            }

                        }
                        //}
                        //防止重复将数据放入
                        if (!dyFlag) {
                            log.info("人员对比后需要修改(标识字段无差异)  sso:{} -> 上游{}", personFromSSO, newPerson);
                            setValidTime(newPerson);
                            if (!CollectionUtils.isEmpty(distinctPersonMap)) {
                                if (distinctPersonMap.containsKey(newPerson.getId())) {
                                    personFromSSO.setDelMark(1);
                                    if (result.containsKey("merge")) {
                                        result.get("merge").add(newPerson);
                                    } else {
                                        ArrayList<Person> people = new ArrayList<>();
                                        people.add(newPerson);
                                        result.put("merge", people);
                                    }
                                }
                            }
                            //if (result.containsKey("update")) {
                            //    result.get("update").add(personFromSSO);
                            //} else {
                            //    ArrayList<Person> people = new ArrayList<>();
                            //    people.add(personFromSSO);
                            //    result.put("update", people);
                            //}
                            if (tempResult.containsKey("update")) {
                                tempResult.get("update").put(newPerson.getId(), newPerson);
                            } else {
                                ConcurrentHashMap<String, Person> hashMap = new ConcurrentHashMap<>();
                                hashMap.put(newPerson.getId(), newPerson);
                                tempResult.put("update", hashMap);
                            }
                            //处理keep人员 数据
                            keepPersonMap.remove(personFromSSO.getId());

                            //处理人员预览数据
                            preViewPersonMap.put(newPerson.getId(), newPerson);

                        }

                        //处理扩展字段对比     修改标识为false则认为主体字段没有差异
                        if (!updateFlag && dyFlag) {
                            //上游的扩展字段
                            Map<String, String> dynamic = newPerson.getDynamic();
                            List<DynamicValue> dyValuesFromSSO = null;
                            //数据库的扩展字段
                            if (!CollectionUtils.isEmpty(valueMap)) {
                                dyValuesFromSSO = valueMap.get(newPerson.getId());
                            }
                            Boolean valueFlag = dynamicProcessing(valueUpdateMap, valueInsertMap, attrMap, newPerson, dynamic, dyValuesFromSSO, dynamicSSOValues);
                            if (valueFlag) {
                                if (!CollectionUtils.isEmpty(distinctPersonMap)) {
                                    if (distinctPersonMap.containsKey(newPerson.getId())) {
                                        personFromSSO.setDelMark(1);
                                        if (result.containsKey("merge")) {
                                            result.get("merge").add(newPerson);
                                        } else {
                                            ArrayList<Person> people = new ArrayList<>();
                                            people.add(newPerson);
                                            result.put("merge", people);
                                        }
                                    }
                                }
                                setValidTime(newPerson);
                                log.info("人员{}对比后主体字段无差异,扩展字段有区别,对比为修改{}", newPerson.getId(), newPerson);
                                if (tempResult.containsKey("update")) {
                                    tempResult.get("update").put(newPerson.getId(), newPerson);
                                } else {
                                    ConcurrentHashMap<String, Person> hashMap = new ConcurrentHashMap<>();
                                    hashMap.put(newPerson.getId(), newPerson);
                                    tempResult.put("update", hashMap);
                                }
                                //处理keep人员 数据
                                keepPersonMap.remove(personFromSSO.getId());
                                //处理人员预览数据
                                preViewPersonMap.put(newPerson.getId(), newPerson);

                            }

                        }


                        //处理头像
                        if (null != newPerson.getAvatarUpdateTime()) {
                            if (avatarMap.containsKey(personFromSSO.getId())) {
                                Avatar avatar = avatarMap.get(personFromSSO.getId());


                                if (newPerson.getAvatarUpdateTime().isBefore(avatar.getAvatarUpdateTime())) {
                                    avatar.setCardNo(newPerson.getCardNo());
                                    avatar.setCardType(newPerson.getCardType());
                                    avatar.setCellphone(newPerson.getCellphone());
                                    avatar.setEmail(newPerson.getEmail());
                                    avatar.setAccountNo(newPerson.getAccountNo());
                                    avatar.setPersonCharacteristic(personCharacteristic);
                                    avatar.setAvatarUrl(newPerson.getAvatarUrl());
                                    if (avatarTempResult.containsKey("update")) {
                                        avatarTempResult.get("update").put(personFromSSO.getId(), avatar);
                                    } else {
                                        ConcurrentHashMap<String, Avatar> hashMap = new ConcurrentHashMap<>();
                                        hashMap.put(personFromSSO.getId(), avatar);
                                        avatarTempResult.put("update", hashMap);
                                    }
                                }
                            } else {
                                Avatar avatar = new Avatar();
                                avatar.setCardNo(newPerson.getCardNo());
                                avatar.setCardType(newPerson.getCardType());
                                avatar.setCellphone(newPerson.getCellphone());
                                avatar.setEmail(newPerson.getEmail());
                                avatar.setAccountNo(newPerson.getAccountNo());
                                avatar.setPersonCharacteristic(personCharacteristic);
                                //avatar.setId(UUID.randomUUID().toString());
                                //avatar.setAvatar(newPerson.getAvatar().getBytes(StandardCharsets.UTF_8));
                                avatar.setAvatarUrl(newPerson.getAvatarUrl());
                                avatar.setAvatarHashCode(newPerson.getAvatarHashCode());
                                avatar.setAvatarUpdateTime(newPerson.getAvatarUpdateTime());
                                avatar.setIdentityId(personFromSSO.getId());
                                if (avatarTempResult.containsKey("insert")) {
                                    avatarTempResult.get("insert").put(personFromSSO.getId(), avatar);
                                } else {
                                    ConcurrentHashMap<String, Avatar> hashMap = new ConcurrentHashMap<>();
                                    hashMap.put(personFromSSO.getId(), avatar);
                                    avatarTempResult.put("insert", hashMap);
                                }
                            }
                        } else if (null != newPerson.getAvatarHashCode()) {
                            if (avatarMap.containsKey(personFromSSO.getId())) {
                                Avatar avatar = avatarMap.get(personFromSSO.getId());
                                if (!newPerson.getAvatarHashCode().equals(avatar.getAvatarHashCode())) {
                                    avatar.setCardNo(newPerson.getCardNo());
                                    avatar.setCardType(newPerson.getCardType());
                                    avatar.setCellphone(newPerson.getCellphone());
                                    avatar.setEmail(newPerson.getEmail());
                                    avatar.setAccountNo(newPerson.getAccountNo());
                                    avatar.setPersonCharacteristic(personCharacteristic);
                                    //avatar.setAvatar(newPerson.getAvatar().getBytes(StandardCharsets.UTF_8));
                                    avatar.setAvatarUrl(newPerson.getAvatarUrl());
                                    avatar.setAvatarHashCode(newPerson.getAvatarHashCode());
                                    if (avatarTempResult.containsKey("update")) {
                                        avatarTempResult.get("update").put(personFromSSO.getId(), avatar);
                                    } else {
                                        ConcurrentHashMap<String, Avatar> hashMap = new ConcurrentHashMap<>();
                                        hashMap.put(personFromSSO.getId(), avatar);
                                        avatarTempResult.put("update", hashMap);
                                    }
                                }

                            } else {
                                Avatar avatar = new Avatar();
                                avatar.setCardNo(newPerson.getCardNo());
                                avatar.setCardType(newPerson.getCardType());
                                avatar.setCellphone(newPerson.getCellphone());
                                avatar.setEmail(newPerson.getEmail());
                                avatar.setAccountNo(newPerson.getAccountNo());
                                avatar.setPersonCharacteristic(personCharacteristic);

                                //avatar.setId(UUID.randomUUID().toString());
                                //avatar.setAvatar(newPerson.getAvatar().getBytes(StandardCharsets.UTF_8));
                                avatar.setAvatarUrl(newPerson.getAvatarUrl());
                                avatar.setAvatarHashCode(newPerson.getAvatarHashCode());
                                avatar.setAvatarUpdateTime(newPerson.getAvatarUpdateTime());
                                avatar.setIdentityId(personFromSSO.getId());
                                if (avatarTempResult.containsKey("insert")) {
                                    avatarTempResult.get("insert").put(personFromSSO.getId(), avatar);
                                } else {
                                    ConcurrentHashMap<String, Avatar> hashMap = new ConcurrentHashMap<>();
                                    hashMap.put(personFromSSO.getId(), avatar);
                                    avatarTempResult.put("insert", hashMap);
                                }
                            }
                        } else {
                            //上游未提供对比字段则认为未提供头像字段,sso有则删除
                            if (avatarMap.containsKey(personFromSSO.getId())) {
                                Avatar avatar = avatarMap.get(personFromSSO.getId());

                                if (avatarTempResult.containsKey("delete")) {
                                    avatarTempResult.get("delete").put(personFromSSO.getId(), avatar);
                                } else {
                                    ConcurrentHashMap<String, Avatar> hashMap = new ConcurrentHashMap<>();
                                    hashMap.put(personFromSSO.getId(), avatar);
                                    avatarTempResult.put("delete", hashMap);
                                }
                            }
                            log.error("人员{}头像对比后删除", newPerson);
                        }


                    } else {
                        log.debug("人员{},对应规则未启用,本次跳过该数据", newPerson);
                    }
                }

            }


        }
        //else if (!personFromUpstream.containsKey(key)
        //        && 1 != personFromSSO.getDelMark()
        //        && (null == personFromSSO.getActive() || personFromSSO.getActive() == 1)
        //        && (source.equals(personFromSSO.getSource()))
        //        && "PULL".equalsIgnoreCase(personFromSSO.getDataSource())) {
        //    if ((null == personFromSSO.getRuleStatus() || personFromSSO.getRuleStatus()) && (CollectionUtils.isEmpty(upstreamMap) || !upstreamMap.containsKey(personFromSSO.getSource()))) {
        //        personFromSSO.setActive(0);
        //        personFromSSO.setActiveTime(now);
        //        personFromSSO.setUpdateTime(now);
        //        personFromSSO.setValidStartTime(OccupyServiceImpl.DEFAULT_START_TIME);
        //        personFromSSO.setValidEndTime(OccupyServiceImpl.DEFAULT_START_TIME);
        //        if (!CollectionUtils.isEmpty(distinctPersonMap)) {
        //            if (distinctPersonMap.containsKey(personFromSSO.getId())) {
        //                personFromSSO.setDelMark(1);
        //                if (result.containsKey("merge")) {
        //                    result.get("merge").add(personFromSSO);
        //                } else {
        //                    result.put("merge", new ArrayList<Person>() {{
        //                        this.add(personFromSSO);
        //                    }});
        //                }
        //            }
        //        }
        //        if (result.containsKey("invalid")) {
        //            result.get("invalid").add(personFromSSO);
        //        } else {
        //            result.put("invalid", new ArrayList<Person>() {{
        //                this.add(personFromSSO);
        //            }});
        //        }
        //        //处理人员预览数据
        //        preViewPersonMap.put(personFromSSO.getId(), personFromSSO);
        //
        //        log.info("人员对比后上游丢失{}", personFromSSO);
        //    } else {
        //        log.info("人员对比后应置为失效{},但检测到对应权威源已无效或规则未启用,跳过该数据", personFromSSO.getId());
        //    }
        //}
    }

    private void dealWithResult(Map<String, List<Person>> result, Person personFromSSO, ConcurrentHashMap<String, ConcurrentHashMap<String, List<Person>>> upstreamCountMap, String type) {
        if (result.containsKey(type)) {
            result.get(type).add(personFromSSO);
        } else {
            result.put(type, new ArrayList<Person>() {{
                this.add(personFromSSO);
            }});
        }
        //处理增量
        if (upstreamCountMap.containsKey(personFromSSO.getUpstreamType())) {
            ConcurrentHashMap<String, List<Person>> hashMap = upstreamCountMap.get(personFromSSO.getUpstreamType());
            if (hashMap.containsKey(type)) {
                hashMap.get(type).add(personFromSSO);
            } else {
                hashMap.put(type, new ArrayList<Person>() {{
                    this.add(personFromSSO);
                }});
            }
        } else {
            ConcurrentHashMap<String, ConcurrentHashMap<String, List<Person>>> hashMap = new ConcurrentHashMap<>();
            ConcurrentHashMap<String, List<Person>> map = new ConcurrentHashMap<>();
            map.put(type, new ArrayList<Person>() {{
                this.add(personFromSSO);
            }});
            hashMap.put(personFromSSO.getUpstreamType(), map);

        }
    }

    private void setValidTime(Person personFromSSO) {
        personFromSSO.setValidStartTime(OccupyServiceImpl.DEFAULT_START_TIME);
        personFromSSO.setValidEndTime(OccupyServiceImpl.DEFAULT_END_TIME);
        if (personFromSSO.getActive() == 0 || personFromSSO.getDelMark() == 1) {
            personFromSSO.setValidStartTime(OccupyServiceImpl.DEFAULT_START_TIME);
            personFromSSO.setValidEndTime(OccupyServiceImpl.DEFAULT_START_TIME);
        }
    }

    @Override
    public PersonConnection findPersons(Map<String, Object> arguments, DomainInfo domain) throws Exception {
        List<PersonEdge> upstreamDept = new ArrayList<>();
        String upstreamTypeId = (String) arguments.get("upstreamTypeId");
        Integer offset = (Integer) arguments.get("offset");
        Integer first = (Integer) arguments.get("first");
        UpstreamType upstreamType = upstreamTypeDao.findById(upstreamTypeId);
        if (null != upstreamType && upstreamType.getIsPage()) {
            Map dataMap = null;
            try {
                dataMap = dataBusUtil.getDataByBus(upstreamType, offset, first, domain);
            } catch (CustomException e) {
                log.error("人员治理中:{} 类型 {} ", upstreamType.getDescription(), e.getMessage());
                throw new CustomException(ResultCode.PERSON_ERROR, null, null, upstreamType.getDescription(), e.getErrorMsg());
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
                    Person person = node1.toJavaObject(Person.class);
                    PersonEdge personEdge = new PersonEdge();
                    personEdge.setNode(person);
                    upstreamDept.add(personEdge);
                }
            }
            PersonConnection personConnection = new PersonConnection();
            personConnection.setEdges(upstreamDept);
            personConnection.setTotalCount(totalCount);


            return personConnection;
        } else {
            throw new CustomException(ResultCode.FAILED, "数据类型不合法,请检查");
        }

    }

    @Override
    public PersonConnection preViewPersons(Map<String, Object> arguments, DomainInfo domain) {
        Integer i = personDao.findPersonTempCount(null, domain);
        //判断数据库是否有数据
        if (i <= 0 || CollectionUtils.isEmpty(personPreViewData) || (!CollectionUtils.isEmpty(personPreViewData) && CollectionUtils.isEmpty(personPreViewData.get(domain.getId())))) {
            this.reFreshPersons(arguments, domain, null);
            return null;
        }
        Tenant tenant = tenantDao.findByDomainName(domain.getDomainName());
        if (null == tenant) {
            throw new CustomException(ResultCode.FAILED, "租户不存在");
        }


        List<DynamicValue> dynamicValues = new ArrayList<>();

        List<DynamicAttr> dynamicAttrs = dynamicAttrDao.findAllByType(TYPE, tenant.getId());

        if (!CollectionUtils.isEmpty(dynamicAttrs)) {

            //获取扩展value
            List<String> attrIds = dynamicAttrs.stream().map(DynamicAttr -> DynamicAttr.getId()).collect(Collectors.toList());

            dynamicValues = dynamicValueDao.findAllByAttrId(attrIds, tenant.getId());
        }

        //扩展字段值分组
        Map<String, List<DynamicValue>> valueMap = new ConcurrentHashMap<>();
        if (!CollectionUtils.isEmpty(dynamicValues)) {
            valueMap = dynamicValues.stream().filter(dynamicValue -> !StringUtils.isBlank(dynamicValue.getEntityId())).collect(Collectors.groupingBy(dynamicValue -> dynamicValue.getEntityId()));
        }
        Map<String, List<DynamicValue>> finalValueMap = valueMap;

        List<Person> personList = personPreViewData.get(domain.getId());
        PersonConnection personConnection = new PersonConnection();
        List<PersonEdge> upstreamDept = new ArrayList<>();

        Map<String, Person> preViewPersonMap = personList.stream().filter(person -> !StringUtils.isBlank(person.getId())).collect(Collectors.toMap(Person::getId, person -> person, (v1, v2) -> v2));
        //根据条件查询
        List<Person> people = personDao.findPersonTemp(arguments, domain);
        Integer personTempCount = personDao.findPersonTempCount(arguments, domain);
        personConnection.setTotalCount(personTempCount);
        if (!CollectionUtils.isEmpty(people)) {
            for (Person person : people) {
                PersonEdge personEdge = new PersonEdge();
                person = preViewPersonMap.get(person.getId());
                //如果上游数据不是最新的获取sso本身的扩展字段值
                if (CollectionUtils.isEmpty(person.getAttrsValues()) && !CollectionUtils.isEmpty(finalValueMap.get(person.getId()))) {
                    List<DynamicValue> dynValues = finalValueMap.get(person.getId());
                    person.setAttrsValues(dynValues);
                }
                personEdge.setNode(person);
                upstreamDept.add(personEdge);
            }
            personConnection.setEdges(upstreamDept);
        }
        return personConnection;


    }

    @Override
    public PreViewTask reFreshPersons(Map<String, Object> arguments, DomainInfo domain, PreViewTask viewTask) {

        if (null == viewTask) {
            viewTask = new PreViewTask();
            viewTask.setTaskId(UUID.randomUUID().toString());
            viewTask.setStatus("doing");
            viewTask.setDomain(domain.getId());
            viewTask.setType("person");
        }
        //查询进行中的刷新人员任务数
        Integer count = preViewTaskService.findByTypeAndStatus("person", "doing", domain);
        if (count <= 10) {
            viewTask = preViewTaskService.saveTask(viewTask);
        } else {

            throw new CustomException(ResultCode.FAILED, "当前任务数量已达上限,无法创建新的刷新任务,请耐心等待");

        }

        if (PreViewPersonThreadPool.executorServiceMap.containsKey(domain.getDomainName())) {
            ExecutorService executorService = PreViewPersonThreadPool.executorServiceMap.get(domain.getDomainName());
            PreViewTask finalViewTask = viewTask;
            executorService.execute(() -> {
                executePreView(arguments, domain, finalViewTask);
            });
        } else {
            PreViewPersonThreadPool.builderExecutor(domain.getDomainName());
            reFreshPersons(arguments, domain, viewTask);
        }


        return viewTask;
    }

    private void executePreView(Map<String, Object> arguments, DomainInfo domain, PreViewTask viewTask) {
        //错误数据置空
        TaskConfig.errorData.put(domain.getId(), "");
        personErrorData = new ConcurrentHashMap<>();
        Tenant tenant = tenantDao.findByDomainName(domain.getDomainName());
        if (null == tenant) {
            throw new CustomException(ResultCode.FAILED, "租户不存在");
        }
        //获取密码加密方式
        pwdConfig = configService.getPasswordConfigByTenantIdAndStatusAndPluginNameAndDelMarkIsFalse(tenant.getId(), "ENABLED", "CommonPlugin");


        List<DynamicAttr> dynamicAttrs = dynamicAttrDao.findAllByType(TYPE, tenant.getId());
        log.info("获取到当前租户{}的映射字段集为{}", tenant.getId(), dynamicAttrs);

        //扩展字段修改容器
        Map<String, DynamicValue> valueUpdateMap = new ConcurrentHashMap<>();
        //扩展字段新增容器
        Map<String, DynamicValue> valueInsertMap = new ConcurrentHashMap<>();

        // 存储最终需要操作的数据
        Map<String, List<Person>> result = new HashMap<>();

        // 存储最终需要操作的头像数据
        Map<String, List<Avatar>> avatarResult = new ConcurrentHashMap<>();

        log.info("----------------- upstream Person start:{}", System.currentTimeMillis());
        List<Person> personList;
        try {
            List<Node> nodes = nodeDao.findNodes(arguments, domain.getId());
            if (null == nodes || nodes.size() <= 0) {
                log.error("无人员管理规则信息");

                throw new CustomException(ResultCode.FAILED, "无人员管理规则信息");
            }
            String nodeId = nodes.get(0).getId();
            //
            List<NodeRules> userRules = rulesService.getByNodeAndType(nodeId, 1, null, 0);
            if (null == userRules || userRules.size() == 0) {
                log.error("无人员管理规则信息");
                throw new CustomException(ResultCode.FAILED, "无人员规则信息");
            }
            //获取该租户下的当前类型的无效有效权威源
            ArrayList<Upstream> invalidUpstreams = upstreamService.findByDomainAndActiveIsFalse(domain.getId());
            Map<String, Upstream> upstreamMap = new ConcurrentHashMap<>();
            if (!CollectionUtils.isEmpty(invalidUpstreams)) {
                upstreamMap = invalidUpstreams.stream().collect(Collectors.toMap((upstream -> upstream.getAppName() + "(" + upstream.getAppCode() + ")"), (upstream -> upstream)));
            }

            //存储头像需要处理的规则
            List<NodeRules> avatarRules = new ArrayList<>();

            personList = dataProcessing(userRules, domain, tenant, valueUpdateMap, valueInsertMap, result, null, avatarResult, upstreamMap, avatarRules);
        } catch (Exception e) {
            e.printStackTrace();
            throw new CustomException(ResultCode.FAILED, e.getMessage());
        }
        log.info("----------------- upstream Person end:{}", System.currentTimeMillis());
        //存储到临时表(首先清除上次遗留数据)
        personDao.removeData(domain);
        Integer i = personDao.findPersonTempCount(null, domain);
        log.info("---------------租户:{},清除人员数据完毕:{}", domain.getId(), i);
        personDao.saveToTemp(personList, domain);
        if (null == personPreViewData) {
            personPreViewData = new ConcurrentHashMap<>();
        }
        personPreViewData.put(domain.getId(), personList);
        if (null != viewTask) {
            viewTask.setStatus("done");
            viewTask.setUpdateTime(new Timestamp(System.currentTimeMillis()));
            preViewTaskService.saveTask(viewTask);
            log.info("人员刷新完毕,任务id为:{}", viewTask.getTaskId());
        }
    }

    @Override
    public PreViewTask reFreshTaskStatus(Map<String, Object> arguments, DomainInfo domain) {
        Object id = arguments.get("taskId");
        return preViewTaskService.findByTaskId(id, domain);
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
    private Boolean dynamicProcessing(Map<String, DynamicValue> valueUpdateMap, Map<String, DynamicValue> valueInsertMap, Map<String, String> attrMap, Person ssoBean, Map<String, String> dynamic, List<DynamicValue> dyValuesFromSSO, Map<String, String> dynamicSSOValues) {
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
                        if (!dynamicSSOValues.containsKey(dynamicValue.getAttrId() + "-" + dynamicValue.getEntityId()) && null != dynamicValue.getValue()) {
                            dynamicSSOValues.put(dynamicValue.getAttrId() + "-" + dynamicValue.getEntityId(), dynamicValue.getValue());
                        }
                        log.info("主体{}扩展字段不同{}->{},修改扩展字段", ssoBean.getName() + ":" + ssoBean.getAccountNo(), dynamicValue.getValue(), o);
                        dynamicValue.setValue(o);
                        //扩展字段预览展示
                        dynamicValue.setKey(dynamicValue.getAttrId());
                        dynamicValue.setCode(str.getValue());

                        valueUpdateMap.put(dynamicValue.getAttrId() + "-" + dynamicValue.getEntityId(), dynamicValue);
                        if (dynamicSSOValues.containsKey(dynamicValue.getAttrId() + "-" + dynamicValue.getEntityId())) {
                            if (o.equals(dynamicSSOValues.get(dynamicValue.getAttrId() + "-" + dynamicValue.getEntityId()))) {
                                valueUpdateMap.remove(dynamicValue.getAttrId() + "-" + dynamicValue.getEntityId());
                                dynamicValue.setValue(dynamicSSOValues.get(dynamicValue.getAttrId() + "-" + dynamicValue.getEntityId()));
                                valueFlag = false;
                            } else {
                                valueFlag = true;
                            }
                        } else {
                            valueFlag = true;
                        }
                        dynValues.add(dynamicValue);
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
                        dynamicValue.setEntityId(ssoBean.getId());
                        dynamicValue.setAttrId(str.getKey());
                        valueFlag = true;
                        log.info("主体{}扩展字段新增{}", ssoBean.getName() + ":" + ssoBean.getAccountNo(), o);
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
                    dynamicValue.setEntityId(ssoBean.getId());
                    dynamicValue.setAttrId(str.getKey());
                    log.info("主体{}扩展字段新增{}", ssoBean.getName() + ":" + ssoBean.getAccountNo(), o);
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


    private String getPasswordByConfig(String pwdConfig, Object newValue) {
        try {
            switch (pwdConfig) {
                case "SHA-1":
                    MessageDigest md = MessageDigest.getInstance("SHA-1");
                    md.update(((String) newValue).getBytes());
                    byte[] digest = md.digest();
                    return "{SHA}" + Base64.encodeBase64String(digest);
                case "SSHA":
                    SecureRandom secureRandom = new SecureRandom();
                    byte[] salt = new byte[8];
                    secureRandom.nextBytes(salt);

                    MessageDigest crypt = null;
                    try {
                        crypt = MessageDigest.getInstance("SHA-1");
                    } catch (NoSuchAlgorithmException e) {
                        e.printStackTrace();
                    }
                    crypt.reset();
                    try {
                        crypt.update(((String) newValue).getBytes("utf-8"));
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    crypt.update(salt);
                    byte[] hash = crypt.digest();

                    byte[] hashPlusSalt = new byte[hash.length + salt.length];
                    System.arraycopy(hash, 0, hashPlusSalt, 0, hash.length);
                    System.arraycopy(salt, 0, hashPlusSalt, hash.length, salt.length);

                    String ssha = new StringBuilder().append("{SSHA}")
                            .append(new String(Base64.encodeBase64(hashPlusSalt), Charset.forName("utf-8")))
                            .toString();
                    return ssha;
                case "MD5":

                default:
                    return "{MD5}" + Base64.encodeBase64String(Hex.decodeHex(DigestUtils.md5DigestAsHex(((String) newValue).getBytes()).toCharArray()));
            }
        } catch (DecoderException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public PreViewTask testUserTask(DomainInfo domain, PreViewTask viewTask) {

        if (null == viewTask) {
            viewTask = new PreViewTask();
            viewTask.setTaskId(UUID.randomUUID().toString());
            viewTask.setStatus("doing");
            viewTask.setDomain(domain.getId());
            viewTask.setType("user");
        }

        viewTask = preViewTaskService.saveTask(viewTask);


        if (PreViewPersonThreadPool.executorServiceMap.containsKey(domain.getDomainName())) {
            ExecutorService executorService = PreViewPersonThreadPool.executorServiceMap.get(domain.getDomainName());
            PreViewTask finalViewTask = viewTask;
            try {
                executorService.execute(() -> {
                    dealTask(domain, finalViewTask);
                });
            } catch (RejectedExecutionException e) {
                viewTask.setStatus("failed");
                finalViewTask.setReason("当前正在人员测试同步中,请稍后再试");
                preViewTaskService.saveTask(viewTask);
                throw new CustomException(ResultCode.FAILED, "当前正在人员测试同步中,请稍后再试");
            } catch (Exception e) {
                viewTask.setStatus("failed");
                finalViewTask.setReason(e.getMessage());
                preViewTaskService.saveTask(viewTask);
                throw new CustomException(ResultCode.FAILED, e.getMessage());
            }
        } else {
            PreViewPersonThreadPool.builderExecutor(domain.getDomainName());
            testUserTask(domain, viewTask);
        }
        return viewTask;
    }

    private void dealTask(DomainInfo domain, PreViewTask viewTask) {
        //错误数据置空
        TaskConfig.errorData.put(domain.getId(), "");
        personErrorData = new ConcurrentHashMap<>();
        Tenant tenant = tenantDao.findByDomainName(domain.getDomainName());
        if (null == tenant) {
            throw new CustomException(ResultCode.FAILED, "租户不存在");
        }

        // 获取规则
        Map<String, Object> arguments = new ConcurrentHashMap<>();
        arguments.put("type", "person");
        arguments.put("status", 0);

        List<Node> nodes = nodeDao.findNodes(arguments, domain.getId());
        if (null == nodes || nodes.size() <= 0) {
            log.error("无人员管理规则信息");
            return;
        }
        String nodeId = nodes.get(0).getId();
        //
        List<NodeRules> userRules = rulesService.getByNodeAndType(nodeId, 1, null, 0);
        if (null == userRules || userRules.size() == 0) {
            log.error("无人员管理规则信息");
            return;
        }

        // 所有证件类型
        List<CardType> cardTypes = cardTypeDao.findAllUser(tenant.getId());
        Map<String, CardType> cardTypeMap = cardTypes.stream().collect(Collectors.toMap(CardType::getCardTypeCode, CardType -> CardType));
        //获取密码加密方式
        pwdConfig = configService.getPasswordConfigByTenantIdAndStatusAndPluginNameAndDelMarkIsFalse(tenant.getId(), "ENABLED", "CommonPlugin");

        //扩展字段修改容器
        Map<String, DynamicValue> valueUpdateMap = new ConcurrentHashMap<>();
        List<DynamicValue> valueUpdate = new ArrayList<>();
        //扩展字段新增容器
        Map<String, DynamicValue> valueInsertMap = new ConcurrentHashMap<>();
        ArrayList<DynamicValue> valueInsert = new ArrayList<>();

        // 存储最终需要操作的数据
        Map<String, List<Person>> result = new HashMap<>();
        // 存储最终需要操作的头像数据
        Map<String, List<Avatar>> avatarResult = new ConcurrentHashMap<>();


        //获取该租户下的当前类型的无效有效权威源
        ArrayList<Upstream> invalidUpstreams = upstreamService.findByDomainAndActiveIsFalse(domain.getId());
        Map<String, Upstream> upstreamMap = new ConcurrentHashMap<>();
        if (!CollectionUtils.isEmpty(invalidUpstreams)) {
            upstreamMap = invalidUpstreams.stream().collect(Collectors.toMap((upstream -> upstream.getAppName() + "(" + upstream.getAppCode() + ")"), (upstream -> upstream)));
        }
        //存储头像需要处理的规则
        List<NodeRules> avatarRules = new ArrayList<>();
        dataProcessing(userRules, domain, tenant, valueUpdateMap, valueInsertMap, result, null, avatarResult, upstreamMap, avatarRules);

        if (!CollectionUtils.isEmpty(personErrorData.get(domain.getId()))) {
            TaskConfig.errorData.put(domain.getId(), JSONObject.toJSONString(personErrorData.get(domain.getId())));
        }
        //处理特征表
        List<Certificate> certificates = null;
        if (!CollectionUtils.isEmpty(result) && !CollectionUtils.isEmpty(result.get("merge"))) {
            certificates = getCertificates(tenant, result);
        }
        if (!CollectionUtils.isEmpty(valueInsertMap)) {
            valueInsert = new ArrayList<>(valueInsertMap.values());
        }
        if (!CollectionUtils.isEmpty(valueUpdateMap)) {
            valueUpdate = new ArrayList<>(valueUpdateMap.values());
        }

        //获取扩展字段列表

        List<DynamicValue> dynamicValues = new ArrayList<>();

        List<DynamicAttr> dynamicAttrs = dynamicAttrDao.findAllByType(TYPE, tenant.getId());
        if (!CollectionUtils.isEmpty(dynamicAttrs)) {
            dynamicValues = dynamicValueDao.findAllAttrByType(tenant.getId(), TYPE);
        }

        personDao.saveToSsoTest(result, tenant.getId(), valueUpdate, valueInsert, dynamicAttrs, certificates, dynamicValues);
        if (null != viewTask) {
            viewTask.setStatus("done");
            //没有变化/新增/删除/修改/无效
            Integer keep = result.containsKey("keep") ? result.get("keep").size() : 0;
            Integer insert = (result.containsKey("insert") ? result.get("insert").size() : 0);
            Integer delete = result.containsKey("delete") ? result.get("delete").size() : 0;
            Integer update = (result.containsKey("update") ? result.get("update").size() : 0);
            Integer invalid = result.containsKey("invalid") ? result.get("invalid").size() : 0;
            String statistics = keep + "/" + insert + "/" + delete + "/" + update + "/" + invalid;
            viewTask.setStatistics(statistics);
            viewTask.setUpdateTime(new Timestamp(System.currentTimeMillis()));
            preViewTaskService.saveTask(viewTask);
            log.info("人员刷新完毕,任务id为:{}", viewTask.getTaskId());
        }
    }

    @Override
    public JSONObject dealWithPerson(DomainInfo domain) {
        Tenant tenant = tenantDao.findByDomainName(domain.getDomainName());
        if (null == tenant) {
            throw new CustomException(ResultCode.FAILED, "租户不存在");
        }
        //最终结果集
        ArrayList<Person> resultPeople = new ArrayList<>();
        //存放重复标识的map
        ConcurrentHashMap<String, List<Person>> peopleMap = new ConcurrentHashMap<>();
        //获取 有重复 用户名 证件类型 及 证件类型号码  的标识
        List<Person> people = personDao.findRepeatPerson(tenant.getId(), "PULL");
        if (!CollectionUtils.isEmpty(people)) {
            for (Person person : people) {
                if (StringUtils.isBlank(person.getAccountNo()) && StringUtils.isBlank(person.getCardType()) && StringUtils.isBlank(person.getCardNo())) {
                    continue;
                }
                peopleMap.put(person.getAccountNo() + ":" + person.getCardType() + ":" + person.getCardNo(), new ArrayList<>());
            }
        }
        //获取当前租户所有PULL source的人员
        List<Person> repeatPeople = personDao.findPersonByDataSource(tenant.getId(), "PULL");
        //去除更新时间最新的人员
        if (!CollectionUtils.isEmpty(repeatPeople)) {
            for (Person repeatPerson : repeatPeople) {
                if (peopleMap.containsKey(repeatPerson.getAccountNo() + ":" + repeatPerson.getCardType() + ":" + repeatPerson.getCardNo())) {
                    peopleMap.get(repeatPerson.getAccountNo() + ":" + repeatPerson.getCardType() + ":" + repeatPerson.getCardNo()).add(repeatPerson);
                }
            }
        }
        if (!CollectionUtils.isEmpty(peopleMap)) {
            for (String key : peopleMap.keySet()) {
                List<Person> values = peopleMap.get(key);
                values.remove(values.size() - 1);
                if (!CollectionUtils.isEmpty(values)) {
                    resultPeople.addAll(values);
                }
            }
        }
        //删除人员,人员身份 及其对应中间表信息
        return personDao.dealWithPeople(resultPeople);
    }

    @Override
    public PersonConnection igaUser(Map<String, Object> arguments, DomainInfo domain) {
        Tenant tenant = tenantDao.findByDomainName(domain.getDomainName());
        Map<String, Object> testPersons = personDao.findTestPersons(arguments, tenant);
        List<Person> list = (List<Person>) testPersons.get("list");
        Integer count = (Integer) testPersons.get("count");
        PersonConnection personConnection = new PersonConnection();
        if (!CollectionUtils.isEmpty(list)) {
            List<DynamicValue> dynamicValues = new ArrayList<>();

            List<DynamicAttr> dynamicAttrs = dynamicAttrDao.findAllByTypeIGA(TYPE, tenant.getId());

            if (!CollectionUtils.isEmpty(dynamicAttrs)) {

                //获取扩展value
                List<String> attrIds = dynamicAttrs.stream().map(DynamicAttr::getId).collect(Collectors.toList());

                dynamicValues = dynamicValueDao.findAllByAttrIdIGA(attrIds, tenant.getId());
            }
            //扩展字段值分组
            Map<String, List<DynamicValue>> valueMap = new ConcurrentHashMap<>();
            if (!CollectionUtils.isEmpty(dynamicValues)) {
                valueMap = dynamicValues.stream().filter(dynamicValue -> !StringUtils.isBlank(dynamicValue.getEntityId())).collect(Collectors.groupingBy(dynamicValue -> dynamicValue.getEntityId()));
            }
            Map<String, List<DynamicValue>> finalValueMap = valueMap;

            ArrayList<PersonEdge> personEdges = new ArrayList<>();
            for (Person person : list) {
                PersonEdge personEdge = new PersonEdge();
                personEdge.setNode(person);
                //处理扩展字段值
                if (!CollectionUtils.isEmpty(finalValueMap.get(person.getId()))) {
                    List<DynamicValue> dynValues = finalValueMap.get(person.getId());
                    person.setAttrsValues(dynValues);
                }
                personEdges.add(personEdge);
            }
            personConnection.setEdges(personEdges);
        }
        personConnection.setTotalCount(count);

        //查询上次同步的时间
        PreViewTask person = preViewTaskService.findByTypeAndUpdateTime("user", domain.getId());
        if (null != person) {
            personConnection.setUpdateTime(person.getUpdateTime());
        }

        return personConnection;
    }

}
