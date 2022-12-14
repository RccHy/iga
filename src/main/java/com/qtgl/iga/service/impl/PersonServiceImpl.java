package com.qtgl.iga.service.impl;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.qtgl.iga.bean.PersonConnection;
import com.qtgl.iga.bean.PersonEdge;
import com.qtgl.iga.bo.*;
import com.qtgl.iga.config.PreViewPersonThreadPool;
import com.qtgl.iga.dao.*;
import com.qtgl.iga.dao.impl.DynamicAttrDaoImpl;
import com.qtgl.iga.dao.impl.DynamicValueDaoImpl;
import com.qtgl.iga.service.ConfigService;
import com.qtgl.iga.service.IncrementalTaskService;
import com.qtgl.iga.service.PersonService;
import com.qtgl.iga.service.PreViewTaskService;
import com.qtgl.iga.task.TaskConfig;
import com.qtgl.iga.utils.ClassCompareUtil;
import com.qtgl.iga.utils.DataBusUtil;
import com.qtgl.iga.utils.TreeEnum;
import com.qtgl.iga.utils.enumerate.ResultCode;
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
    NodeRulesDao rulesDao;
    @Autowired
    UpstreamTypeDao upstreamTypeDao;
    @Autowired
    UpstreamDao upstreamDao;
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

    public static ConcurrentHashMap<String, List<JSONObject>> personErrorData = null;
    //
    public static ConcurrentHashMap<String, List<Person>> personPreViewData = null;
    //??????
    private final String TYPE = "USER";
    //????????????
    private String pwdConfig = "";
    //???????????????
    private final String ACCOUNT_NO = "USERNAME";
    //????????????+????????????
    private final String CARD_TYPE_NO = "CARD_TYPE_NO";
    //???????????????
    private final String CARD_NO = "CARD_NO";
    //??????
    private final String EMAIL = "EMAIL";
    //?????????
    private final String CELLPHONE = "CELLPHONE";

    /**
     * <p>
     * * 1?????????????????????????????? ????????????
     * <p>
     * MapA(123???12X) ??? ????????????+?????????  MapB(X24???XX5)????????????????????????+??????????????????????????????   MapC(123???X24???XX5)??? A?????????????????????+B
     * 2?????????????????? ????????????+????????????  ?????? ????????????????????????????????????
     * 2.1  ??????
     * 2.1.1 ?????????
     * A????????????????????????????????????????????????
     * A-2?????????MapC?????????????????????????????????????????? ?????????????????????????????????+????????????????????????????????????????????????????????????????????????????????????&??????????????? ?????? ????????????????????????MapA or MapB??????????????????MapA+1
     * B?????????????????????????????????MapA?????????????????????(???????????????????????????????????????||????????????????????????????????????????????????????????????????????????????????????????????????)??? MapA+1 or (Map-1,MapA+1)??????????????????????????????????????????????????????MapC?????????
     * <p>
     * <p>
     * 2.2 ????????? ???????????????????????????????????????
     * 2.2.1 ??????????????????  ?????????????????????????????????
     * 2.2.2 ?????????????????? ??????
     * A?????????MapC???????????????????????????????????????????????????????????????????????????+??????????????????  ???????????? ?????? ????????????????????????MapA or MapB???????????????MapB+1
     * B?????????MapC???????????? ??? MapB+1???MapC+1
     * <p>
     * *
     * * 3??????????????????????????????????????????????????????
     * * A?????????  ???????????????sso??????????????????
     * * B?????????  ?????????sso???????????????????????????
     * * C?????????  ???????????????del_mark
     * * D: ??????  ???????????????????????????????????? OR ???????????????active
     * * E: ??????  ????????????????????????????????????????????????????????????
     * Doing??? ????????????????????? ??????????????????????????????????????????????????????
     * TODO??? ?????????????????????????????????????????????????????????????????????
     */

    @Override
    public Map<String, List<Person>> buildPerson(DomainInfo domain, TaskLog taskLog, TaskLog currentTask) throws Exception {
        //??????????????????
        TaskConfig.errorData.put(domain.getId(), "");
        personErrorData = new ConcurrentHashMap<>();
        Tenant tenant = tenantDao.findByDomainName(domain.getDomainName());
        if (null == tenant) {
            throw new CustomException(ResultCode.FAILED, "???????????????");
        }
        // ??????????????????
        List<CardType> cardTypes = cardTypeDao.findAllUser(tenant.getId());
        Map<String, CardType> cardTypeMap = cardTypes.stream().collect(Collectors.toMap(CardType::getCardTypeCode, CardType -> CardType));
        //????????????????????????
        pwdConfig = configService.getPasswordConfigByTenantIdAndStatusAndPluginNameAndDelMarkIsFalse(tenant.getId(), "ENABLED", "CommonPlugin");

        //????????????????????????
        List<String> dynamicCodes = new ArrayList<>();

        List<DynamicValue> dynamicValues = new ArrayList<>();

        List<DynamicAttr> dynamicAttrs = dynamicAttrDao.findAllByType(TYPE, tenant.getId());
        log.info("?????????????????????{}?????????????????????{}", tenant.getId(), dynamicAttrs);
        ////??????????????????
        //List<IncrementalTask> incrementalTasks = new ArrayList<>();

        //????????????????????????
        Map<String, DynamicValue> valueUpdateMap = new ConcurrentHashMap<>();
        List<DynamicValue> valueUpdate = new ArrayList<>();
        //????????????????????????
        Map<String, DynamicValue> valueInsertMap = new ConcurrentHashMap<>();
        ArrayList<DynamicValue> valueInsert = new ArrayList<>();

        if (!CollectionUtils.isEmpty(dynamicAttrs)) {
            dynamicCodes = dynamicAttrs.stream().map(DynamicAttr -> DynamicAttr.getCode()).collect(Collectors.toList());
            //????????????value
            List<String> attrIds = dynamicAttrs.stream().map(DynamicAttr -> DynamicAttr.getId()).collect(Collectors.toList());

            dynamicValues = dynamicValueDao.findAllByAttrId(attrIds, tenant.getId());
        }

        //?????????????????????
        Map<String, List<DynamicValue>> valueMap = new ConcurrentHashMap<>();
        if (!CollectionUtils.isEmpty(dynamicValues)) {
            valueMap = dynamicValues.stream().filter(dynamicValue -> !StringUtils.isBlank(dynamicValue.getEntityId())).collect(Collectors.groupingBy(dynamicValue -> dynamicValue.getEntityId()));
        }
        List<String> finalDynamicCodes = dynamicCodes;
        Map<String, List<DynamicValue>> finalValueMap = valueMap;

        // ?????????????????????????????????
        Map<String, List<Person>> result = new HashMap<>();
        //????????????id???code??????map
        Map<String, String> attrMap = new ConcurrentHashMap<>();
        Map<String, String> attrReverseMap = new ConcurrentHashMap<>();

        // ????????????
        Map arguments = new ConcurrentHashMap();
        arguments.put("type", "person");
        arguments.put("status", 0);

        List<Node> nodes= nodeDao.findNodes(arguments, domain.getId());
        if (null == nodes || nodes.size() <= 0) {
            log.error( "???????????????????????????");
            return null;
            //throw new CustomException(ResultCode.FAILED, "???????????????????????????");
        }
        String nodeId = nodes.get(0).getId();
        //
        List<NodeRules> userRules = rulesDao.getByNodeAndType(nodeId, 1, null, 0);
        if (null == userRules || userRules.size() == 0) {
            log.error( "???????????????????????????");
            return null;
            //throw new CustomException(ResultCode.FAILED, "?????????????????????");
        }
        dataProcessing(nodes,userRules,domain, tenant, cardTypeMap, dynamicAttrs, valueUpdateMap, valueInsertMap, finalDynamicCodes, finalValueMap, result, attrMap, attrReverseMap, arguments, currentTask);
        // ??????????????????
        List<Person> personFromSSOList = personDao.getAll(tenant.getId());
        log.info("--------------------??????????????????????????????");
        calculationService.monitorRules(domain, taskLog, personFromSSOList.size(), result.get("delete"), result.get("invalid"));
        log.info("--------------------??????????????????????????????");
        if (!CollectionUtils.isEmpty(personErrorData.get(domain.getId()))) {
            TaskConfig.errorData.put(domain.getId(), JSONObject.toJSONString(personErrorData.get(domain.getId())));
        }
        //???????????????
        ArrayList<Certificate> certificates = new ArrayList<>();
        if (!CollectionUtils.isEmpty(result) && !CollectionUtils.isEmpty(result.get("merge"))) {
            //????????????????????????
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
                                //????????????+????????????
                                if (StringUtils.isNotBlank(person.getCardNo()) && StringUtils.isNotBlank(person.getCardType()) && (!oldCertificate.getCardNo().equals(person.getCardNo()) || !oldCertificate.getCardType().equals(person.getCardType()))) {
                                    oldCertificate.setCardNo(person.getCardNo());
                                    oldCertificate.setCardType(person.getCardType());
                                    oldCertificate.setUpdateTime(now);
                                    certificates.add(oldCertificate);
                                }

                            } else if (CARD_NO.equals(oldCertificate.getCardType())) {
                                //???????????????
                                if (StringUtils.isNotBlank(person.getCardNo()) && !oldCertificate.getCardNo().equals(person.getCardNo())) {
                                    oldCertificate.setCardNo(person.getCardNo());
                                    oldCertificate.setUpdateTime(now);
                                    certificates.add(oldCertificate);
                                }

                            } else if (EMAIL.equals(oldCertificate.getCardType())) {
                                //??????
                                if (StringUtils.isNotBlank(person.getEmail()) && !oldCertificate.getCardNo().equals(person.getEmail())) {
                                    oldCertificate.setCardNo(person.getEmail());
                                    oldCertificate.setUpdateTime(now);
                                    certificates.add(oldCertificate);
                                }

                            } else if (CELLPHONE.equals(oldCertificate.getCardType())) {
                                //?????????
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
        }
        if (!CollectionUtils.isEmpty(valueInsertMap)) {
            valueInsert = new ArrayList<>(valueInsertMap.values());
        }
        if (!CollectionUtils.isEmpty(valueUpdateMap)) {
            valueUpdate = new ArrayList<>(valueUpdateMap.values());
        }
        personDao.saveToSso(result, tenant.getId(), valueUpdate, valueInsert, certificates);


        return result;
    }

    private List<Person> dataProcessing(List<Node> nodes,List<NodeRules> userRules,DomainInfo domain, Tenant tenant, Map<String, CardType> cardTypeMap, List<DynamicAttr> dynamicAttrs, Map<String, DynamicValue> valueUpdateMap, Map<String, DynamicValue> valueInsertMap, List<String> finalDynamicCodes, Map<String, List<DynamicValue>> finalValueMap, Map<String, List<Person>> result, Map<String, String> attrMap, Map<String, String> attrReverseMap, Map arguments, TaskLog currentTask) {
        final LocalDateTime now = LocalDateTime.now();
        List<Person> people = new ArrayList<>();

        //??????sso ?????????????????????
        List<Person> personDelMarkFromSSOList = personDao.getDelMarkPeople(tenant.getId());
        if (!CollectionUtils.isEmpty(personDelMarkFromSSOList)) {
            people.addAll(personDelMarkFromSSOList);
        }

        // ?????? sso?????? (????????????????????????,)
        List<Person> personFromSSOList = personDao.getAll(tenant.getId());
        if (!CollectionUtils.isEmpty(personFromSSOList)) {
            people.addAll(personFromSSOList);
        }
        //??????  sso????????????????????????
        List<Person> distinctPerson = personDao.findDistinctPerson(tenant.getId());
        Map<String, Person> distinctPersonMap = new ConcurrentHashMap<>();
        if (!CollectionUtils.isEmpty(distinctPerson)) {
            distinctPersonMap = distinctPerson.stream().filter(person -> !StringUtils.isBlank(person.getId())).collect(Collectors.toMap(Person::getId, person -> person, (v1, v2) -> v2));
            people.addAll(distinctPerson);
        }

        Map<String, Person> preViewPersonMap = people.stream().filter(person -> !StringUtils.isBlank(person.getId())).collect(Collectors.toMap(Person::getId, person -> person, (v1, v2) -> v2));
        //?????????????????????????????????
        Map<String, Person> invalidPersonMap = people.stream().filter(person -> !StringUtils.isBlank(person.getId()) && null != person.getValidEndTime() && null != person.getValidStartTime() && now.isBefore(person.getValidEndTime()) && now.isAfter(person.getValidStartTime())).collect(Collectors.toMap(person -> (person.getId()), person -> person, (v1, v2) -> v2));

        //??????  sso ?????????????????????????????????   key  id ->  person
        Map<String, Person> backUpPersonMap = new ConcurrentHashMap<>();
        //??????????????????????????????
        Map<String, Map<String, Person>> tempResult = new HashMap<>();
        //????????????????????????
        if (!CollectionUtils.isEmpty(dynamicAttrs)) {
            attrMap = dynamicAttrs.stream().collect(Collectors.toMap(DynamicAttr::getId, DynamicAttr::getCode));
            attrReverseMap = dynamicAttrs.stream().collect(Collectors.toMap(DynamicAttr::getCode, DynamicAttr::getId));
        }

        Map<String, String> finalAttrMap = attrMap;
        Map<String, String> finalAttrReverseMap = attrReverseMap;
        //?????????????????????????????????????????????????????????
        ArrayList<Upstream> invalidUpstreams = upstreamDao.findByDomainAndActiveIsFalse(domain.getId());
        Map<String, Upstream> upstreamMap = new ConcurrentHashMap<>();
        if (!CollectionUtils.isEmpty(invalidUpstreams)) {
            upstreamMap = invalidUpstreams.stream().collect(Collectors.toMap((upstream -> upstream.getAppName() + "(" + upstream.getAppCode() + ")"), (upstream -> upstream)));
        }
        Map<String, Upstream> finalUpstreamMap = upstreamMap;
        Map<String, Person> finalDistinctPersonMap = distinctPersonMap;
        userRules.forEach(rules -> {

            //?????????????????????????????????
            Map<String, Person> personFromUpstreamByPersonCharacteristic = new ConcurrentHashMap<>();


            // ????????????????????????
            UpstreamType upstreamType = upstreamTypeDao.findById(rules.getUpstreamTypesId());
            if (null == upstreamType) {
                log.error("??????????????????????????????'{}'??????????????????????????????", rules);
                throw new CustomException(ResultCode.NO_UPSTREAM_TYPE, null, null, "??????", rules.getId());
            }
            ArrayList<Upstream> upstreams = upstreamDao.getUpstreams(upstreamType.getUpstreamId(), domain.getId());
            if (CollectionUtils.isEmpty(upstreams)) {
                log.error("??????????????????????????????'{}'??????????????????", rules.getId());
                throw new CustomException(ResultCode.NO_UPSTREAM, null, null, "??????", rules.getId());
            }
            String source = upstreams.get(0).getAppName() + "(" + upstreams.get(0).getAppCode() + ")";
            JSONArray dataByBus = null;
            try {
                dataByBus = dataBusUtil.getDataByBus(upstreamType, domain.getDomainName());
            } catch (CustomException e) {
                e.printStackTrace();
                if (new Long("1085").equals(e.getCode())) {
                    throw new CustomException(ResultCode.INVOKE_URL_ERROR, "????????????????????????,??????????????????:" + upstreams.get(0).getAppName() + "(" + upstreams.get(0).getAppCode() + ")" + "?????????????????????:" + upstreamType.getDescription());
                } else {
                    throw e;
                }
            } catch (Exception e) {
                e.printStackTrace();
                log.error("????????????????????? : " + upstreamType.getUpstreamId() + "???????????????");
                throw new CustomException(ResultCode.PERSON_ERROR, null, null, upstreamType.getDescription(), e.getMessage());
            }
            //??????????????????????????????
            String personCharacteristic = upstreamType.getPersonCharacteristic();
            List<Person> personUpstreamList = new ArrayList<>();

            //List<Person> invalidList = result.get("invalid");
            ////??????map
            //Map<String, Person> invalidMap = new ConcurrentHashMap<>();
            //if (!CollectionUtils.isEmpty(invalidList)) {
            //    if (ACCOUNT_NO.equals(personCharacteristic)) {
            //        invalidMap = invalidList.stream().filter(person ->
            //                !StringUtils.isBlank(person.getAccountNo())).collect(Collectors.toMap(Person::getAccountNo, person -> person, (v1, v2) -> v2));
            //
            //    } else if (CARD_TYPE_NO.equals(personCharacteristic)) {
            //        //????????????+????????????
            //        invalidMap = invalidList.stream()
            //                .filter(person -> !StringUtils.isBlank(person.getCardType()) && !StringUtils.isBlank(person.getCardNo()))
            //                .collect(Collectors.toMap(person -> (person.getCardType() + ":" + person.getCardNo()), person -> person, (v1, v2) -> v2));
            //
            //    } else if (CARD_NO.equals(personCharacteristic)) {
            //        //???????????????
            //        invalidMap = invalidList.stream()
            //                .filter(person -> !StringUtils.isBlank(person.getCardNo()))
            //                .collect(Collectors.toMap(Person::getCardNo, person -> person, (v1, v2) -> v2));
            //
            //    } else if (EMAIL.equals(personCharacteristic)) {
            //        //??????
            //        invalidMap = invalidList.stream()
            //                .filter(person -> !StringUtils.isBlank(person.getEmail()))
            //                .collect(Collectors.toMap(Person::getEmail, person -> person, (v1, v2) -> v2));
            //
            //    } else if (CELLPHONE.equals(personCharacteristic)) {
            //        //?????????
            //        invalidMap = invalidList.stream()
            //                .filter(person -> !StringUtils.isBlank(person.getCellphone()))
            //                .collect(Collectors.toMap(Person::getCellphone, person -> person, (v1, v2) -> v2));
            //
            //    }
            //}

            //??????????????????????????????????????????
            if (null != dataByBus) {
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
                        extracted(domain, personUpstream, "?????????????????????????????????");
                        log.error("?????????????????????????????????:{}", personUpstream.getActive());
                        continue;
                    }
                    if (null != personUpstream.getDelMark() && personUpstream.getDelMark() != 0 && personUpstream.getDelMark() != 1) {
                        extracted(domain, personUpstream, "?????????????????????????????????");
                        log.error("?????????????????????????????????:{}", personUpstream.getDelMark());
                        continue;
                    }
                    // ???????????? ???????????????????????????   OR    ????????? accountNo  ???????????????
                    if (StringUtils.isBlank(personUpstream.getCardNo()) && StringUtils.isBlank(personUpstream.getCardType())) {
                        if (StringUtils.isBlank(personUpstream.getAccountNo())) {
                            extracted(domain, personUpstream, "?????????????????????:??????????????????????????????????????????");
                            log.error("{}?????????????????????:??????????????????????????????????????????", personUpstream.getName());
                            continue;
                        }

                    }
                    // ????????????????????????,???????????????????????????
                    if (StringUtils.isBlank(personUpstream.getCardNo()) && !StringUtils.isBlank(personUpstream.getCardType())) {
                        extracted(domain, personUpstream, "????????????????????????????????????????????????");
                        log.error("{}-{}????????????????????????????????????????????????", personUpstream.getCardNo(), personUpstream.getAccountNo());
                        continue;
                    }
                    if (!StringUtils.isBlank(personUpstream.getCardType()) && cardTypeMap.containsKey(personUpstream.getCardType())) {
                        String cardTypeReg = cardTypeMap.get(personUpstream.getCardType()).getCardTypeReg();
                        if (StringUtils.isNotBlank(cardTypeReg) && !Pattern.matches(cardTypeReg, personUpstream.getCardNo())) {
                            extracted(domain, personUpstream, "???????????????????????????");
                            log.error("???????????????????????????:{}", personUpstream.getCardNo());
                            continue;
                        }
                    } else if (!StringUtils.isBlank(personUpstream.getCardType()) && !cardTypeMap.containsKey(personUpstream.getCardType())) {
                        extracted(domain, personUpstream, "??????????????????");
                        log.error("??????????????????:{}", personUpstream.getCardType());
                        continue;
                    }

                    if (StringUtils.isBlank(personUpstream.getName())) {
                        extracted(domain, personUpstream, "????????????");
                        log.error("{}-{}????????????", personUpstream.getCardNo(), personUpstream.getAccountNo());
                        continue;
                    }

                    personUpstream.setSource(source);
                    personUpstream.setUpstreamType(upstreamType.getId());
                    personUpstream.setCreateTime(now);
                    if (null != upstreamType.getIsIncremental() && upstreamType.getIsIncremental()) {
                        personUpstream.setDataSource("INC_PULL");
                    } else {
                        personUpstream.setDataSource("PULL");
                    }

                    if (null == personUpstream.getUpdateTime()) {
                        personUpstream.setUpdateTime(now);
                    }
                    //???????????????del,????????????????????????
                    if (null == personUpstream.getDelMark()) {
                        personUpstream.setDelMark(0);
                    }
                    //???????????????active,?????????????????????
                    if (null == personUpstream.getActive()) {
                        personUpstream.setActive(1);
                    }
                    //??????activeTime?????????
                    personUpstream.setActiveTime(LocalDateTime.now());

                    //??????????????????
                    ConcurrentHashMap<String, String> map = null;
                    if (!CollectionUtils.isEmpty(finalDynamicCodes)) {
                        map = new ConcurrentHashMap<>();
                        for (String dynamicCode : finalDynamicCodes) {
                            if (personObj.containsKey(dynamicCode)) {
                                if (StringUtils.isNotBlank(personObj.getString(dynamicCode))) {
                                    map.put(dynamicCode, personObj.getString(dynamicCode));
                                }
                            }
                        }
                        log.info("??????{}???????????????????????????{}", personObj, map);
                        personUpstream.setDynamic(map);
                    }
                    personUpstream.setRuleStatus(rules.getActive());
                    personUpstreamList.add(personUpstream);
                    dealWithUpstreamDataByPersonCharacteristic(domain, personUpstream, personCharacteristic, personFromUpstreamByPersonCharacteristic, null, preViewPersonMap);
                }

                //result.put("invalid", new ArrayList<>(invalidMap.values()));

                //????????????????????????????????????????????????????????????
                if (null != currentTask && null != upstreamType.getIsIncremental() && upstreamType.getIsIncremental() && !CollectionUtils.isEmpty(personUpstreamList)) {
                    List<Person> collect1 = personUpstreamList.stream().sorted(Comparator.comparing(Person::getUpdateTime).reversed()).collect(Collectors.toList());
                    IncrementalTask incrementalTask = new IncrementalTask();
                    incrementalTask.setId(UUID.randomUUID().toString());
                    incrementalTask.setMainTaskId(currentTask.getId());
                    incrementalTask.setType("person");
                    incrementalTask.setMainTaskId(currentTask.getId());
                    log.info("??????:{},???????????????:{},??????????????????????????????:{} -> {},????????????:{}", upstreamType.getSynType(), upstreamType.getId(), collect1.get(0).getUpdateTime(), collect1.get(0).getUpdateTime().toInstant(ZoneOffset.ofHours(+8)).toEpochMilli(), System.currentTimeMillis());
                    long min = Math.min(collect1.get(0).getUpdateTime().toInstant(ZoneOffset.ofHours(+8)).toEpochMilli(), System.currentTimeMillis());
                    incrementalTask.setTime(new Timestamp(min));
                    incrementalTask.setUpstreamTypeId(collect1.get(0).getUpstreamType());
                    incrementalTaskService.save(incrementalTask, domain);
                    //incrementalTaskMap.put(incrementalTask.getUpstreamTypeId(), incrementalTask);
                }
                if (!CollectionUtils.isEmpty(personFromUpstreamByPersonCharacteristic)) {
                    log.info("???????????????:{}??????????????????????????????:{}", upstreamType.getId(), personFromUpstreamByPersonCharacteristic.size());

                    //???????????????????????????????????????????????????(??????)
                    ConcurrentHashMap<String, ConcurrentHashMap<String, List<Person>>> upstreamCountMap = new ConcurrentHashMap<>();
                    //?????????????????????????????????
                    List<UpstreamTypeField> fields = DataBusUtil.typeFields.get(upstreamType.getId());
                    Map<String, UpstreamTypeField> filedsMap = fields.stream().collect(Collectors.toMap(UpstreamTypeField::getSourceField, sourceFiled -> sourceFiled, (v1, v2) -> v2));


                    if (ACCOUNT_NO.equals(personCharacteristic)) {
                        //todo ???????????????
                        // ?????????
                        Map<String, Person> personFromSSOMapByAccount = new ArrayList<>(preViewPersonMap.values()).stream().filter(person ->
                                !StringUtils.isBlank(person.getAccountNo())).collect(Collectors.toMap(Person::getAccountNo, person -> person, (v1, v2) -> v2));
                        personFromSSOMapByAccount.forEach((key, personFromSSO) -> {
                            calculate(personFromUpstreamByPersonCharacteristic, now, result, key, personFromSSO, domain, finalAttrMap, finalValueMap, valueUpdateMap, valueInsertMap, finalUpstreamMap, preViewPersonMap, upstreamCountMap, source, finalDistinctPersonMap, invalidPersonMap, tempResult, backUpPersonMap, fields, filedsMap);
                        });
                        personFromUpstreamByPersonCharacteristic.forEach((key, val) -> {
                            calculateInsert(personFromSSOMapByAccount, result, key, val, domain, upstreamCountMap);
                        });
                    } else if (CARD_TYPE_NO.equals(personCharacteristic)) {
                        // ????????????+????????????
                        Map<String, Person> personFromSSOMapByCardTypeAndNo = new ArrayList<>(preViewPersonMap.values()).stream()
                                .filter(person -> !StringUtils.isBlank(person.getCardType()) && !StringUtils.isBlank(person.getCardNo()))
                                .collect(Collectors.toMap(person -> (person.getCardType() + ":" + person.getCardNo()), person -> person, (v1, v2) -> v2));

                        personFromSSOMapByCardTypeAndNo.forEach((key, personFromSSO) -> {
                            calculate(personFromUpstreamByPersonCharacteristic, now, result, key, personFromSSO, domain, finalAttrMap, finalValueMap, valueUpdateMap, valueInsertMap, finalUpstreamMap, preViewPersonMap, upstreamCountMap, source, finalDistinctPersonMap, invalidPersonMap, tempResult, backUpPersonMap, fields, filedsMap);
                        });
                        personFromUpstreamByPersonCharacteristic.forEach((key, val) -> {
                            calculateInsert(personFromSSOMapByCardTypeAndNo, result, key, val, domain, upstreamCountMap);
                        });
                    } else if (CARD_NO.equals(personCharacteristic)) {
                        // ????????????
                        Map<String, Person> personFromSSOMapByCardNo = new ArrayList<>(preViewPersonMap.values()).stream()
                                .filter(person -> !StringUtils.isBlank(person.getCardNo()))
                                .collect(Collectors.toMap(Person::getCardNo, person -> person, (v1, v2) -> v2));

                        personFromSSOMapByCardNo.forEach((key, personFromSSO) -> {
                            calculate(personFromUpstreamByPersonCharacteristic, now, result, key, personFromSSO, domain, finalAttrMap, finalValueMap, valueUpdateMap, valueInsertMap, finalUpstreamMap, preViewPersonMap, upstreamCountMap, source, finalDistinctPersonMap, invalidPersonMap, tempResult, backUpPersonMap, fields, filedsMap);
                        });
                        personFromUpstreamByPersonCharacteristic.forEach((key, val) -> {
                            calculateInsert(personFromSSOMapByCardNo, result, key, val, domain, upstreamCountMap);
                        });

                    } else if (EMAIL.equals(personCharacteristic)) {
                        // ??????
                        Map<String, Person> personFromSSOMapByEmail = new ArrayList<>(preViewPersonMap.values()).stream()
                                .filter(person -> !StringUtils.isBlank(person.getEmail()))
                                .collect(Collectors.toMap(Person::getEmail, person -> person, (v1, v2) -> v2));

                        personFromSSOMapByEmail.forEach((key, personFromSSO) -> {
                            calculate(personFromUpstreamByPersonCharacteristic, now, result, key, personFromSSO, domain, finalAttrMap, finalValueMap, valueUpdateMap, valueInsertMap, finalUpstreamMap, preViewPersonMap, upstreamCountMap, source, finalDistinctPersonMap, invalidPersonMap, tempResult, backUpPersonMap, fields, filedsMap);
                        });
                        personFromUpstreamByPersonCharacteristic.forEach((key, val) -> {
                            calculateInsert(personFromSSOMapByEmail, result, key, val, domain, upstreamCountMap);
                        });
                    } else if (CELLPHONE.equals(personCharacteristic)) {
                        // ?????????
                        Map<String, Person> personFromSSOMapByCellphone = new ArrayList<>(preViewPersonMap.values()).stream()
                                .filter(person -> !StringUtils.isBlank(person.getCellphone()))
                                .collect(Collectors.toMap(Person::getCellphone, person -> person, (v1, v2) -> v2));

                        personFromSSOMapByCellphone.forEach((key, personFromSSO) -> {
                            calculate(personFromUpstreamByPersonCharacteristic, now, result, key, personFromSSO, domain, finalAttrMap, finalValueMap, valueUpdateMap, valueInsertMap, finalUpstreamMap, preViewPersonMap, upstreamCountMap, source, finalDistinctPersonMap, invalidPersonMap, tempResult, backUpPersonMap, fields, filedsMap);
                        });
                        personFromUpstreamByPersonCharacteristic.forEach((key, val) -> {
                            calculateInsert(personFromSSOMapByCellphone, result, key, val, domain, upstreamCountMap);
                        });
                    }

                    if (!CollectionUtils.isEmpty(result.get("insert"))) {
                        Map<String, Person> insert = result.get("insert").stream().filter(person -> !StringUtils.isBlank(person.getId())).collect(Collectors.toMap(person -> (person.getId()), person -> person, (v1, v2) -> v2));
                        preViewPersonMap.putAll(insert);
                    }

                } else {
                    log.error("???????????????:{}?????????????????????????????????,????????????????????????????????????", upstreamType.getId());
                }
            }
        });
        // sso ????????????????????????
        try {
            List<Person> insert = result.get("insert");
            if (null != insert && insert.size() > 0) {
                for (Person key : insert) {
                    ArrayList<DynamicValue> dynamicValues = new ArrayList<>();
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
                            //????????????????????????
                            dynamicValue.setKey(dynamicValue.getAttrId());
                            dynamicValue.setCode(str.getValue());
                            dynamicValues.add(dynamicValue);
                        }
                    }
                    key.setAttrsValues(dynamicValues);
                }
            }
            log.info("??????????????????:??????????????????????????????{},????????????{}", CollectionUtils.isEmpty(valueUpdateMap) ? 0 : valueUpdateMap.size(), CollectionUtils.isEmpty(valueInsertMap) ? 0 : valueInsertMap.size());
            log.debug("??????????????????:??????????????????????????????{},????????????{}", valueInsertMap, valueInsertMap);
        } catch (CustomException e) {
            TaskConfig.errorData.put(domain.getId(), JSONObject.toJSONString(preViewPersonMap.values()));
            throw new CustomException(ResultCode.FAILED, e.getErrorMsg());
        }
        //?????? ?????? ???????????????
        if (!CollectionUtils.isEmpty(invalidPersonMap)) {
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
                        //????????????????????????
                        preViewPersonMap.put(invalidPerson.getId(), invalidPerson);

                        log.info("???????????????????????????{}", invalidPerson);
                    } else {
                        log.info("??????????????????????????????{},??????????????????????????????????????????????????????,???????????????", invalidPerson.getId());
                    }
                }


            }
        }
        // ??????result
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

        //????????????????????????
        people = new ArrayList<>(preViewPersonMap.values());
        people = people.stream().filter(person -> ((null == person.getDelMark() || person.getDelMark().equals(0)) && person.getActive().equals(1))).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(result.get("insert"))) {
            people.addAll(result.get("insert"));
        }
        return people;
    }

    private void dealWithUpstreamDataByPersonCharacteristic(DomainInfo domain, Person personUpstream, String personCharacteristic, Map<String, Person> personFromUpstreamByPersonCharacteristic, Map<String, Person> invalidMap, Map<String, Person> preViewPersonMap) {
        if (ACCOUNT_NO.equals(personCharacteristic)) {
            //???????????????
            if (StringUtils.isNotEmpty(personUpstream.getAccountNo())) {
                ////????????????
                //if (invalidMap.containsKey(personUpstream.getAccountNo())) {
                //    //??????????????????????????????????????? ????????????????????????
                //    preViewPersonMap.put(invalidMap.get(personUpstream.getAccountNo()).getId(), personUpstream);
                //    invalidMap.remove(personUpstream.getAccountNo());
                //}
                //???????????????????????????????????????
                if (personFromUpstreamByPersonCharacteristic.containsKey(personUpstream.getAccountNo())) {
                    if (personUpstream.getActive() == 1) {
                        Person person = personFromUpstreamByPersonCharacteristic.get(personUpstream.getAccountNo());
                        log.error("????????????:{},?????? : ????????????", person);
                        extracted(domain, person, "????????????");
                        personFromUpstreamByPersonCharacteristic.put(personUpstream.getAccountNo(), personUpstream);
                    } else {
                        log.error("????????????:{},?????? : ???????????????????????????,??????????????????", personUpstream);
                        extracted(domain, personUpstream, "???????????????????????????,??????????????????");
                    }
                } else {
                    //????????????????????????????????????????????????,??????????????????
                    personFromUpstreamByPersonCharacteristic.put(personUpstream.getAccountNo(), personUpstream);
                }

            } else {
                log.error("????????????:{},?????? : ????????????????????????,?????????????????????????????????????????????:{}", personUpstream, personCharacteristic);
                extracted(domain, personUpstream, "????????????????????????");
            }
        } else if (CARD_TYPE_NO.equals(personCharacteristic)) {
            //????????????+????????????
            if (StringUtils.isNotBlank(personUpstream.getCardNo()) && StringUtils.isNotBlank(personUpstream.getCardType())) {
                ////????????????
                //if (invalidMap.containsKey(personUpstream.getCardType() + ":" + personUpstream.getCardNo())) {
                //    //??????????????????????????????????????? ????????????????????????
                //    preViewPersonMap.put(invalidMap.get(personUpstream.getCardType() + ":" + personUpstream.getCardNo()).getId(), personUpstream);
                //
                //    invalidMap.remove(personUpstream.getCardType() + ":" + personUpstream.getCardNo());
                //}
                //??????
                if (personFromUpstreamByPersonCharacteristic.containsKey(personUpstream.getCardType() + ":" + personUpstream.getCardNo())) {
                    //?????????????????????
                    if (personUpstream.getActive() == 1) {
                        Person person = personFromUpstreamByPersonCharacteristic.get(personUpstream.getCardType() + ":" + personUpstream.getCardNo());

                        log.error("????????????:{},?????? : ????????????", person);
                        extracted(domain, person, "????????????");
                        personFromUpstreamByPersonCharacteristic.put(personUpstream.getCardType() + ":" + personUpstream.getCardNo(), personUpstream);

                    } else {
                        log.error("????????????:{},?????? : ???????????????????????????", personUpstream);
                        extracted(domain, personUpstream, "???????????????????????????,??????????????????");
                    }
                } else {
                    //?????????????????????????????????
                    personFromUpstreamByPersonCharacteristic.put(personUpstream.getCardType() + ":" + personUpstream.getCardNo(), personUpstream);
                }
            } else {
                log.error("????????????:{},?????? : ????????????????????????,?????????????????????????????????????????????:{}", personUpstream, personCharacteristic);
                extracted(domain, personUpstream, "????????????????????????");
            }
        } else if (CARD_NO.equals(personCharacteristic)) {
            //???????????????
            if (StringUtils.isNotEmpty(personUpstream.getCardNo())) {
                ////????????????
                //if (invalidMap.containsKey(personUpstream.getCardNo())) {
                //    //??????????????????????????????????????? ????????????????????????
                //    preViewPersonMap.put(invalidMap.get(personUpstream.getCardNo()).getId(), personUpstream);
                //    invalidMap.remove(personUpstream.getCardNo());
                //
                //}
                //??????????????????????????????????????????
                if (personFromUpstreamByPersonCharacteristic.containsKey(personUpstream.getCardNo())) {
                    if (personUpstream.getActive() == 1) {
                        Person person = personFromUpstreamByPersonCharacteristic.get(personUpstream.getCardNo());
                        log.error("????????????:{},?????? : ????????????", person);
                        extracted(domain, person, "????????????");
                        personFromUpstreamByPersonCharacteristic.put(personUpstream.getCardNo(), personUpstream);
                    } else {
                        log.error("????????????:{},?????? : ???????????????????????????,??????????????????", personUpstream);
                        extracted(domain, personUpstream, "???????????????????????????,??????????????????");
                    }
                } else {
                    //???????????????????????????????????????????????????,??????????????????
                    personFromUpstreamByPersonCharacteristic.put(personUpstream.getCardNo(), personUpstream);
                }

            } else {
                log.error("????????????:{},?????? : ????????????????????????,?????????????????????????????????????????????:{}", personUpstream, personCharacteristic);
                extracted(domain, personUpstream, "????????????????????????");
            }
        } else if (EMAIL.equals(personCharacteristic)) {
            //??????
            if (StringUtils.isNotEmpty(personUpstream.getEmail())) {
                ////????????????
                //if (invalidMap.containsKey(personUpstream.getEmail())) {
                //    //??????????????????????????????????????? ????????????????????????
                //    preViewPersonMap.put(invalidMap.get(personUpstream.getEmail()).getId(), personUpstream);
                //    invalidMap.remove(personUpstream.getEmail());
                //}
                //????????????????????????????????????
                if (personFromUpstreamByPersonCharacteristic.containsKey(personUpstream.getEmail())) {
                    if (personUpstream.getActive() == 1) {
                        Person person = personFromUpstreamByPersonCharacteristic.get(personUpstream.getEmail());
                        log.error("????????????:{},?????? : ????????????", person);
                        extracted(domain, person, "????????????");
                        personFromUpstreamByPersonCharacteristic.put(personUpstream.getEmail(), personUpstream);
                    } else {
                        log.error("????????????:{},?????? : ???????????????????????????,??????????????????", personUpstream);
                        extracted(domain, personUpstream, "???????????????????????????,??????????????????");
                    }
                } else {
                    //?????????????????????????????????????????????,??????????????????
                    personFromUpstreamByPersonCharacteristic.put(personUpstream.getEmail(), personUpstream);
                }

            } else {
                log.error("????????????:{},?????? : ????????????????????????,?????????????????????????????????????????????:{}", personUpstream, personCharacteristic);
                extracted(domain, personUpstream, "????????????????????????");
            }
        } else if (CELLPHONE.equals(personCharacteristic)) {
            //?????????
            if (StringUtils.isNotEmpty(personUpstream.getCellphone())) {
                ////????????????
                //if (invalidMap.containsKey(personUpstream.getCellphone())) {
                //    //??????????????????????????????????????? ????????????????????????
                //    preViewPersonMap.put(invalidMap.get(personUpstream.getCellphone()).getId(), personUpstream);
                //    invalidMap.remove(personUpstream.getCellphone());
                //}
                //???????????????????????????????????????
                if (personFromUpstreamByPersonCharacteristic.containsKey(personUpstream.getCellphone())) {
                    if (personUpstream.getActive() == 1) {
                        Person person = personFromUpstreamByPersonCharacteristic.get(personUpstream.getCellphone());
                        log.error("????????????:{},?????? : ????????????", person);
                        extracted(domain, person, "????????????");
                        personFromUpstreamByPersonCharacteristic.put(personUpstream.getCellphone(), personUpstream);
                    } else {
                        log.error("????????????:{},?????? : ???????????????????????????,??????????????????", personUpstream);
                        extracted(domain, personUpstream, "???????????????????????????,??????????????????");
                    }
                } else {
                    //????????????????????????????????????????????????,??????????????????
                    personFromUpstreamByPersonCharacteristic.put(personUpstream.getCellphone(), personUpstream);
                }

            } else {
                log.error("????????????:{},?????? : ????????????????????????,?????????????????????????????????????????????:{}", personUpstream, personCharacteristic);
                extracted(domain, personUpstream, "????????????????????????");
            }
        }
    }

    //    ??????????????????
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
        log.warn("??????{}?????????????????????????????????{}", domain.getId(), jsonObject);
    }

//    private void calculate(Map<String, Person> personFromUpstream, Map<String, Person> personRepeatByAccount, LocalDateTime now, Map<String, List<Person>> result, String key, Person personFromSSO, Map<String, String> attrMap, Map<String, List<DynamicValue>> valueMap, List<DynamicValue> valueUpdate, List<DynamicValue> valueInsert, Map<String, Upstream> upstreamMap, Map<String, Person> preViewPersonMap, ConcurrentHashMap<String, ConcurrentHashMap<String, List<Person>>> upstreamCountMap) {
//        // ????????????????????????person
//        if (personFromUpstream.containsKey(key) &&
//                personFromUpstream.get(key).getUpdateTime().isAfter(personFromSSO.getUpdateTime())) {
//            Person newPerson = personFromUpstream.get(key);
//            //????????????????????????????????????????????????
//            if (newPerson.getRuleStatus()) {
//
//                //????????????
//                boolean updateFlag = false;
//                //del????????????
//                boolean delFlag = false;
//                //????????????
//                boolean invalidFlag = false;
//                //????????????
//                boolean passwordFlag = false;
//                //??????????????????
//                // boolean invalidRecoverFlag = true;
//                //??????????????????????????????
//                boolean dyFlag = true;
//
//                if (!"PULL".equals(personFromSSO.getDataSource())) {
//                    updateFlag = true;
//                }
//                personFromSSO.setSource(newPerson.getSource());
//                personFromSSO.setDataSource(newPerson.getDataSource());
//                personFromSSO.setUpstreamType(newPerson.getUpstreamType());
//                //??????????????????????????????
//                personFromSSO.setRuleStatus(newPerson.getRuleStatus());
//                //??????sso?????????active???null?????????
//                if (null == personFromSSO.getActive() || "".equals(personFromSSO.getActive())) {
//                    personFromSSO.setActive(1);
//                }
//                List<UpstreamTypeField> fields = DataBusUtil.typeFields.get(newPerson.getUpstreamType());
//                // ????????????????????????????????????????????????
//                //    ?????????????????????????????????????????????
//                if (null != fields && fields.size() > 0) {
//                    for (UpstreamTypeField field : fields) {
//                        String sourceField = field.getSourceField();
//                        Object newValue = ClassCompareUtil.getGetMethod(newPerson, sourceField);
//                        Object oldValue = ClassCompareUtil.getGetMethod(personFromSSO, sourceField);
//                        //???????????????????????????
//                        if (sourceField.equalsIgnoreCase("password")) {
//                            if (null == oldValue && null != newValue) {
//                                //todo??????????????????
//                                String password = getPasswordByConfig(pwdConfig, newValue);
//                                //String password = "{MD5}" + Base64.encodeBase64String(Hex.decodeHex(DigestUtils.md5DigestAsHex(((String) newValue).getBytes()).toCharArray()));
//                                personFromSSO.setPassword(password);
//                                if (result.containsKey("password")) {
//                                    result.get("password").add(personFromSSO);
//                                } else {
//                                    result.put("password", new ArrayList<Person>() {{
//                                        this.add(personFromSSO);
//                                    }});
//                                }
//                            }
//                            continue;
//                        }
//                        if (null == oldValue && null == newValue) {
//                            continue;
//                        }
//                        if (null != oldValue && oldValue.equals(newValue)) {
//                            continue;
//                        }
////                        if (sourceField.equals("delMark") && (Integer) oldValue == 1 && (Integer) newValue == 0) {
////                            delFlag = true;
////                            log.info("????????????{}???????????????", personFromSSOList.getId());
////                        }
//                        if (sourceField.equalsIgnoreCase("delMark") && (Integer) oldValue == 0 && (Integer) newValue == 1) {
//                            delFlag = true;
//                            log.info("????????????{}??????", personFromSSO.getId());
//                            continue;
//                        }
//
//                        updateFlag = true;
//                        if (sourceField.equalsIgnoreCase("active") && (Integer) oldValue == 1 && (Integer) newValue == 0) {
//                            invalidFlag = true;
//                            log.info("????????????{}??????", personFromSSO.getId());
//                            // continue;
//                        }
//                        if (sourceField.equalsIgnoreCase("active") && (Integer) oldValue == 0 && (Integer) newValue == 1) {
//                            log.info("????????????{}???????????????", personFromSSO.getId());
//                            continue;
//                        }
//                        if (sourceField.equalsIgnoreCase("password") && null != newValue) {
//                            //   if (StringUtils.isBlank((String) oldValue) && !StringUtils.isBlank((String) newValue)) {
//                            //todo??????????????????
//                            String password = getPasswordByConfig(pwdConfig, newValue);
//                            //String password = "{MD5}" + Base64.encodeBase64String(Hex.decodeHex(DigestUtils.md5DigestAsHex(((String) newValue).getBytes()).toCharArray()));
//                            passwordFlag = true;
//                            personFromSSO.setPassword(password);
//                            continue;
//                            // }
//                        }
//                  /*  if (sourceField.equalsIgnoreCase("active") && (Integer) oldValue == 0 && (Integer) newValue == 1) {
//                        invalidRecoverFlag = false;
//                    }*/
//
//                        ClassCompareUtil.setValue(personFromSSO, personFromSSO.getClass(), sourceField, oldValue, newValue);
//                        log.info("??????????????????{}:??????{}???{} -> {}", personFromSSO.getId(), sourceField, oldValue, newValue);
//                    }
//                }
//
//
//                if (delFlag) {
//                    if ((null == personFromSSO.getRuleStatus() || personFromSSO.getRuleStatus()) && (CollectionUtils.isEmpty(upstreamMap) || !upstreamMap.containsKey(personFromSSO.getSource()))) {
//                        personFromSSO.setDelMark(1);
//                        personFromSSO.setUpdateTime(newPerson.getUpdateTime());
//                        personFromSSO.setValidStartTime(OccupyServiceImpl.DEFAULT_START_TIME);
//                        personFromSSO.setValidEndTime(OccupyServiceImpl.DEFAULT_START_TIME);
//                        if (result.containsKey("delete")) {
//                            result.get("delete").add(personFromSSO);
//                        } else {
//                            result.put("delete", new ArrayList<Person>() {{
//                                this.add(personFromSSO);
//                            }});
//                        }
//                        //????????????????????????
//                        //preViewPersonMap.remove(personFromSSO.getId());
//                        log.info("??????????????????{}", personFromSSO.getId());
//                    } else {
//                        log.info("????????????????????????{},??????????????????????????????????????????????????????,???????????????", personFromSSO.getId());
//                    }
//                }
//                if (updateFlag && personFromSSO.getDelMark() != 1) {
//                    personFromSSO.setSource(newPerson.getSource());
//                    personFromSSO.setUpdateTime(newPerson.getUpdateTime());
//                    // ????????????????????????
//                    if (passwordFlag) {
//                        if (result.containsKey("password")) {
//                            result.get("password").add(personFromSSO);
//                        } else {
//                            result.put("password", new ArrayList<Person>() {{
//                                this.add(personFromSSO);
//                            }});
//                        }
//                    }
//                    //??????
//                    if (invalidFlag) {
//                        if ((null == personFromSSO.getRuleStatus() || personFromSSO.getRuleStatus()) && (CollectionUtils.isEmpty(upstreamMap) || !upstreamMap.containsKey(personFromSSO.getSource()))) {
//                            personFromSSO.setActive(0);
//                            personFromSSO.setActiveTime(newPerson.getUpdateTime());
//                            personFromSSO.setValidStartTime(OccupyServiceImpl.DEFAULT_START_TIME);
//                            personFromSSO.setValidEndTime(OccupyServiceImpl.DEFAULT_START_TIME);
//                            if (result.containsKey("invalid")) {
//                                result.get("invalid").add(personFromSSO);
//                            } else {
//                                result.put("invalid", new ArrayList<Person>() {{
//                                    this.add(personFromSSO);
//                                }});
//                            }
//                            //????????????????????????
//                            //preViewPersonMap.remove(personFromSSO.getId());
//                            log.info("??????????????????{}", personFromSSO.getId());
//                        } else {
//                            log.info("??????????????????????????????{},??????????????????????????????????????????????????????,???????????????", personFromSSO.getId());
//                        }
//                    } else {
//                        if (!personFromSSO.getActive().equals(newPerson.getActive())) {
//                            personFromSSO.setActive(newPerson.getActive());
//                            personFromSSO.setActiveTime(newPerson.getUpdateTime());
//                        }
//                        if (personFromSSO.getActive() == 0 || personFromSSO.getDelMark() == 1) {
//                            personFromSSO.setValidStartTime(OccupyServiceImpl.DEFAULT_START_TIME);
//                            personFromSSO.setValidEndTime(OccupyServiceImpl.DEFAULT_START_TIME);
//                        }
//                        setValidTime(personFromSSO);
//
//                        //if (result.containsKey("update")) {
//                        //    result.get("update").add(personFromSSO);
//                        //} else {
//                        //    result.put("update", new ArrayList<Person>() {{
//                        //        this.add(personFromSSO);
//                        //    }});
//                        //}
//                        if (dyFlag) {
//                            //?????????????????????
//                            Map<String, String> dynamic = newPerson.getDynamic();
//                            List<DynamicValue> dyValuesFromSSO = null;
//                            //????????????????????????
//                            if (!CollectionUtils.isEmpty(valueMap)) {
//                                dyValuesFromSSO = valueMap.get(personFromSSO.getId());
//                            }
//                            dynamicProcessing(valueUpdate, valueInsert, attrMap, personFromSSO, dynamic, dyValuesFromSSO);
//                            dyFlag = false;
//                        }
//                    }
//                    log.info("???????????????????????????(?????????????????????){}", personFromSSO);
//
//                }
//
//                // ??????????????????????????????"????????????"?????????sso?????????????????????active??????????????????
//                if (!updateFlag && personFromSSO.getDelMark() != 1) {
//                    //
//                    if (!personFromSSO.getActive().equals(newPerson.getActive())) {
//                        personFromSSO.setActive(newPerson.getActive());
//                        personFromSSO.setActiveTime(newPerson.getUpdateTime());
//                        personFromSSO.setUpdateTime(newPerson.getUpdateTime());
//                        setValidTime(personFromSSO);
//                        if (dyFlag) {
//                            //?????????????????????
//                            Map<String, String> dynamic = newPerson.getDynamic();
//                            List<DynamicValue> dyValuesFromSSO = null;
//                            //????????????????????????
//                            if (!CollectionUtils.isEmpty(valueMap)) {
//                                dyValuesFromSSO = valueMap.get(personFromSSO.getId());
//                            }
//                            dynamicProcessing(valueUpdate, valueInsert, attrMap, personFromSSO, dynamic, dyValuesFromSSO);
//                            dyFlag = false;
//                        }
//
//                    }
//
//                }
//                //???????????????????????????
//                if (!dyFlag) {
//                    if (result.containsKey("update")) {
//                        result.get("update").add(personFromSSO);
//                    } else {
//                        result.put("update", new ArrayList<Person>() {{
//                            this.add(personFromSSO);
//                        }});
//                    }
//                    //????????????????????????
//                    preViewPersonMap.put(personFromSSO.getId(), personFromSSO);
//                }
//
//                //????????????????????????     ???????????????false?????????????????????????????????
//                if (!updateFlag && dyFlag) {
//                    //?????????????????????
//                    Map<String, String> dynamic = newPerson.getDynamic();
//                    List<DynamicValue> dyValuesFromSSO = null;
//                    //????????????????????????
//                    if (!CollectionUtils.isEmpty(valueMap)) {
//                        dyValuesFromSSO = valueMap.get(personFromSSO.getId());
//                    }
//                    Boolean valueFlag = dynamicProcessing(valueUpdate, valueInsert, attrMap, personFromSSO, dynamic, dyValuesFromSSO);
//                    if (valueFlag) {
//                        if (result.containsKey("update")) {
//                            result.get("update").add(personFromSSO);
//                        } else {
//                            result.put("update", new ArrayList<Person>() {{
//                                this.add(personFromSSO);
//                            }});
//                        }
//                        //????????????????????????
//                        preViewPersonMap.put(personFromSSO.getId(), personFromSSO);
//                    }
//
//                }
//            } else {
//                log.debug("??????{},?????????????????????,?????????????????????", newPerson);
//            }
//
//
//        } else if (!personFromUpstream.containsKey(key)
//                && (StringUtils.isNotBlank(personFromSSO.getAccountNo()) && !personRepeatByAccount.containsKey(personFromSSO.getAccountNo()))
//                && 1 != personFromSSO.getDelMark()
//                && (null == personFromSSO.getActive() || personFromSSO.getActive() == 1)
//                && "PULL".equalsIgnoreCase(personFromSSO.getDataSource())) {
//
//            if ((null == personFromSSO.getRuleStatus() || personFromSSO.getRuleStatus()) && (CollectionUtils.isEmpty(upstreamMap) || !upstreamMap.containsKey(personFromSSO.getSource()))) {
//                personFromSSO.setActive(0);
//                personFromSSO.setActiveTime(now);
//                personFromSSO.setUpdateTime(now);
//                personFromSSO.setValidStartTime(OccupyServiceImpl.DEFAULT_START_TIME);
//                personFromSSO.setValidEndTime(OccupyServiceImpl.DEFAULT_START_TIME);
//                if (result.containsKey("invalid")) {
//                    result.get("invalid").add(personFromSSO);
//                } else {
//                    result.put("invalid", new ArrayList<Person>() {{
//                        this.add(personFromSSO);
//                    }});
//                }
//                //????????????????????????
//                //preViewPersonMap.remove(personFromSSO.getId());
//
//                log.info("???????????????????????????{}", personFromSSO.getId());
//            } else {
//                log.info("??????????????????????????????{},??????????????????????????????????????????????????????,???????????????", personFromSSO.getId());
//            }
//        }
//    }

    private void calculateInsert(Map<String, Person> personFromSSOMap, Map<String, List<Person>> result, String key, Person val, DomainInfo domainInfo, ConcurrentHashMap<String, ConcurrentHashMap<String, List<Person>>> upstreamCountMap) {
        //sso???????????????????????????????????????????????????
        if (!personFromSSOMap.containsKey(key) && (val.getDelMark() == 0)) {
            if (val.getRuleStatus()) {
                //??????????????????????????????
                String id = UUID.randomUUID().toString();
                val.setId(id);
                val.setOpenId(RandomStringUtils.randomAlphabetic(20));
                val.setValidStartTime(OccupyServiceImpl.DEFAULT_START_TIME);
                val.setValidEndTime(OccupyServiceImpl.DEFAULT_END_TIME);

                // ????????????????????? active=0 ?????? ?????? del_mark=1 ??????  ?????? ???????????????
                //   ?????? ???????????????????????? ??????
                if (val.getActive() == 0 || val.getDelMark() == 1) {
                    val.setValidStartTime(OccupyServiceImpl.DEFAULT_START_TIME);
                    val.setValidEndTime(OccupyServiceImpl.DEFAULT_START_TIME);
                }
                // ?????????????????? ?????????????????? password??????
                if (!StringUtils.isBlank(val.getPassword())) {
                    String password = val.getPassword();
                    //todo??????????????????
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
                // ????????????????????????????????? ????????????
                if (null == val.getFreezeTime()) {
                    //?????????????????????,???????????????????????????(??????????????????)
                    val.setFreezeTime(val.getCreateTime().minusDays(1));
                }
                //????????????????????????????????????
                if (id.equals(val.getId())) {
                    if (result.containsKey("insert")) {
                        result.get("insert").add(val);
                    } else {
                        result.put("insert", new ArrayList<Person>() {{
                            this.add(val);
                        }});
                    }
                    log.debug("?????????????????????{}", val);
                } else {
                    if (result.containsKey("update")) {
                        result.get("update").add(val);
                    } else {
                        result.put("update", new ArrayList<Person>() {{
                            this.add(val);
                        }});
                    }
                    log.debug("?????????????????????{}", val);
                }


            } else {
                log.debug("????????????????????????{},???????????????????????????,?????????????????????", val);
            }
        }
    }

    private void calculate(Map<String, Person> personFromUpstream, LocalDateTime now, Map<String, List<Person>> result, String key, Person personFromSSO, DomainInfo domainInfo, Map<String, String> attrMap, Map<String, List<DynamicValue>> valueMap, Map<String, DynamicValue> valueUpdateMap, Map<String, DynamicValue> valueInsertMap, Map<String, Upstream> upstreamMap, Map<String, Person> preViewPersonMap, ConcurrentHashMap<String, ConcurrentHashMap<String, List<Person>>> upstreamCountMap, String source, Map<String, Person> distinctPersonMap, Map<String, Person> invalidPersonMap, Map<String, Map<String, Person>> tempResult, Map<String, Person> backUpPersonMap, List<UpstreamTypeField> fields, Map<String, UpstreamTypeField> filedsMap) {
        // ????????????????????????person
        if (personFromUpstream.containsKey(key)) {
            //?????????????????????????????????????????????map?????????
            invalidPersonMap.remove(personFromSSO.getId());
            if (!personFromUpstream.get(key).getUpdateTime().isBefore(personFromSSO.getUpdateTime())) {

                //?????????????????????????????????,????????????????????????????????????
                if (backUpPersonMap.containsKey(personFromSSO.getId())) {
                    personFromSSO = backUpPersonMap.get(personFromSSO.getId());

                    //?????????????????????????????????
                    //?????????????????????????????????,?????????????????????????????????????????????????????????????????????
                    if (tempResult.containsKey("invalid")) {
                        tempResult.get("invalid").remove(personFromSSO.getId());
                    }
                    if (tempResult.containsKey("update")) {
                        tempResult.get("update").remove(personFromSSO.getId());
                    }
                    if (tempResult.containsKey("delete")) {
                        tempResult.get("delete").remove(personFromSSO.getId());
                    }

                } else {

                    //Person clone = (Person) personFromSSO.clone();
                    backUpPersonMap.put(personFromSSO.getId(), personFromSSO);
                }
                Person newPerson = personFromUpstream.get(key);


                //????????????????????????????????????????????????
                if (newPerson.getRuleStatus()) {
                    ////????????????????????????
                    //boolean licitFlag = true;
                    //????????????
                    boolean updateFlag = false;
                    //del????????????
                    boolean delFlag = false;
                    //????????????
                    boolean invalidFlag = false;
                    //????????????
                    boolean passwordFlag = false;
                    //??????????????????
                    // boolean invalidRecoverFlag = true;
                    //??????????????????????????????
                    boolean dyFlag = true;

                    //??????sso?????????,???????????????????????????
                    if (!"PULL".equals(personFromSSO.getDataSource()) && !"INC_PULL".equals(personFromSSO.getDataSource())) {
                        updateFlag = true;
                    }
                    //personFromSSO.setDataSource(newPerson.getDataSource());
                    //personFromSSO.setSource(newPerson.getSource());
                    //personFromSSO.setUpstreamType(newPerson.getUpstreamType());
                    //????????????????????????
                    //personFromSSO.setRuleStatus(newPerson.getRuleStatus());
                    //??????sso?????????active???null?????????
                    if (null == personFromSSO.getActive() || "".equals(personFromSSO.getActive())) {
                        personFromSSO.setActive(1);
                    }
                    //List<UpstreamTypeField> fields = DataBusUtil.typeFields.get(newPerson.getUpstreamType());
                    //??????????????????
                    Map<String, Map<String, Object>> oldValueMap = ClassCompareUtil.compareObject(personFromSSO, newPerson);
                    if (!CollectionUtils.isEmpty(oldValueMap)) {
                        for (String k : oldValueMap.keySet()) {
                            if (filedsMap.containsKey(k)) {
                                continue;
                            }
                            if (k.equals("updateTime") || k.equals("ruleStatus") || k.equals("attrsValues") || k.equals("createSource") || k.equals("createDataSource")
                                    || k.equals("dynamic") || k.equals("password") || k.equals("freezeTime") || k.equals("validEndTime") || k.equals("validStartTime")
                                    || k.equals("upstreamType") || k.equals("delMark") || k.equals("createTime") || k.equals("activeTime") || k.equals("source")
                                    || k.equals("dataSource") || k.equals("tenantId")) {
                                continue;
                            }

                            ClassCompareUtil.setValue(newPerson, personFromSSO.getClass(), k, oldValueMap.get(k).get("newValue"), oldValueMap.get(k).get("oldValue"));

                        }
                    }
                    // ????????????????????????????????????????????????
                    //    ?????????????????????????????????????????????
                    if (null != fields && fields.size() > 0) {
                        for (UpstreamTypeField field : fields) {
                            String sourceField = field.getSourceField();
                            Object newValue = ClassCompareUtil.getGetMethod(newPerson, sourceField);
                            Object oldValue = ClassCompareUtil.getGetMethod(personFromSSO, sourceField);
                            //???????????????????????????
                            if (sourceField.equalsIgnoreCase("password")) {
                                if (null == oldValue && null != newValue) {
                                    //todo??????????????????
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
//                            log.info("????????????{}???????????????", personFromSSOList.getId());
//                        }
//
//                    if (sourceField.equalsIgnoreCase("accountNo") && null != newValue && StringUtils.isNotEmpty(newValue.toString()) && personFromSSOMapByAccountAll.containsKey(newValue)) {
//                        Person person = personFromSSOMapByAccountAll.get(newValue);
//                        if (!newPerson.getCardType().equals(person.getCardType()) || !newPerson.getCardNo().equals(person.getCardNo())) {
//
//                            extracted(domainInfo, person, "??????????????????????????????????????????????????????,?????????");
//                            extracted(domainInfo, newPerson, "??????????????????????????????????????????????????????,?????????");
//                            //????????????????????????????????????????????????,???????????????
//                            log.error("?????????????????????{}?????????????????????????????????{}{},?????????", person.getAccountNo(), person, newPerson);
//                            licitFlag = false;
//                            personFromUpstream.remove(key);
//                            personFromSSOMapCopy.remove(key);
//                            break;
//                        }
//                    }

                            //????????????????????????
                            if (sourceField.equals("delMark") && (Integer) oldValue == 1 && (Integer) newValue == 0) {
                                if (!CollectionUtils.isEmpty(distinctPersonMap) && distinctPersonMap.containsKey(newPerson.getId())) {
                                    //????????????
                                    newPerson.setDelMark(0);
                                    continue;
                                }
                            }
                            if (sourceField.equalsIgnoreCase("delMark") && (Integer) oldValue == 0 && (Integer) newValue == 1) {
                                delFlag = true;
                                log.info("????????????{}??????", newPerson.getId());
                                continue;
                            }

                            updateFlag = true;
                            if (sourceField.equalsIgnoreCase("active") && (Integer) oldValue == 1 && (Integer) newValue == 0) {
                                invalidFlag = true;
                                log.info("????????????{}??????", newPerson.getId());
                                // continue;
                            }
                            if (sourceField.equalsIgnoreCase("active") && (Integer) oldValue == 0 && (Integer) newValue == 1) {

                                log.info("????????????{}???????????????", newPerson.getId());
                                continue;
                            }
                            if (sourceField.equalsIgnoreCase("password") && null != newValue) {
                                //   if (StringUtils.isBlank((String) oldValue) && !StringUtils.isBlank((String) newValue)) {
                                //??????????????????
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
                            log.info("??????????????????{}:??????{}???{} -> {}", newPerson.getId(), sourceField, oldValue, newValue);

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
                            //??????????????????
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
                            //????????????????????????
                            //preViewPersonMap.remove(personFromSSO.getId());

                            log.info("??????????????????{}", newPerson.getId());
                        } else {
                            log.info("????????????????????????{},??????????????????????????????????????????????????????,???????????????", newPerson.getId());
                        }
                    }
                    //if (updateFlag && personFromSSO.getDelMark() != 1) {
                    if (updateFlag) {
                        //personFromSSO.setSource(newPerson.getSource());
                        //personFromSSO.setUpdateTime(newPerson.getUpdateTime());
                        // ????????????????????????
                        if (passwordFlag) {
                            if (result.containsKey("password")) {
                                result.get("password").add(newPerson);
                            } else {
                                ArrayList<Person> people = new ArrayList<>();
                                people.add(newPerson);
                                result.put("password", people);
                            }
                        }
                        //??????
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

                                //????????????????????????
                                //preViewPersonMap.remove(personFromSSO.getId());

                                log.info("???????????????????????????{}", newPerson.getId());
                            } else {
                                log.info("??????????????????????????????{},??????????????????????????????????????????????????????,???????????????", newPerson.getId());
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
                                //?????????????????????
                                Map<String, String> dynamic = newPerson.getDynamic();
                                List<DynamicValue> dyValuesFromSSO = null;
                                //????????????????????????
                                if (!CollectionUtils.isEmpty(valueMap)) {
                                    dyValuesFromSSO = valueMap.get(newPerson.getId());
                                }
                                dynamicProcessing(valueUpdateMap, valueInsertMap, attrMap, newPerson, dynamic, dyValuesFromSSO);
                                dyFlag = false;
                            }
                        }

                    }

                    // ??????????????????????????????"????????????"?????????sso?????????????????????active??????????????????
                    //if (!updateFlag && personFromSSO.getDelMark() != 1) {
                    if (!updateFlag) {

                        if (!personFromSSO.getActive().equals(newPerson.getActive())) {
                            //personFromSSO.setActive(newPerson.getActive());
                            //personFromSSO.setActiveTime(newPerson.getUpdateTime());
                            //personFromSSO.setUpdateTime(newPerson.getUpdateTime());
                            newPerson.setActiveTime(newPerson.getUpdateTime());
                            setValidTime(newPerson);
                            if (dyFlag) {
                                //?????????????????????
                                Map<String, String> dynamic = newPerson.getDynamic();
                                List<DynamicValue> dyValuesFromSSO = null;
                                //????????????????????????
                                if (!CollectionUtils.isEmpty(valueMap)) {
                                    dyValuesFromSSO = valueMap.get(newPerson.getId());
                                }
                                dynamicProcessing(valueUpdateMap, valueInsertMap, attrMap, newPerson, dynamic, dyValuesFromSSO);
                                dyFlag = false;
                            }


                        }

                    }
                    //}
                    //???????????????????????????
                    if (!dyFlag) {
                        log.info("???????????????????????????(?????????????????????)  sso:{} -> ??????{}", personFromSSO, newPerson);
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

                        //????????????????????????
                        preViewPersonMap.put(newPerson.getId(), newPerson);

                    }

                    //????????????????????????     ???????????????false?????????????????????????????????
                    if (!updateFlag && dyFlag) {
                        //?????????????????????
                        Map<String, String> dynamic = newPerson.getDynamic();
                        List<DynamicValue> dyValuesFromSSO = null;
                        //????????????????????????
                        if (!CollectionUtils.isEmpty(valueMap)) {
                            dyValuesFromSSO = valueMap.get(newPerson.getId());
                        }
                        Boolean valueFlag = dynamicProcessing(valueUpdateMap, valueInsertMap, attrMap, newPerson, dynamic, dyValuesFromSSO);
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

                            if (tempResult.containsKey("update")) {
                                tempResult.get("update").put(newPerson.getId(), newPerson);
                            } else {
                                ConcurrentHashMap<String, Person> hashMap = new ConcurrentHashMap<>();
                                hashMap.put(newPerson.getId(), newPerson);
                                tempResult.put("update", hashMap);
                            }
                            //????????????????????????
                            preViewPersonMap.put(newPerson.getId(), newPerson);

                        }

                    }

                } else {
                    log.debug("??????{},?????????????????????,?????????????????????", newPerson);
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
        //        //????????????????????????
        //        preViewPersonMap.put(personFromSSO.getId(), personFromSSO);
        //
        //        log.info("???????????????????????????{}", personFromSSO);
        //    } else {
        //        log.info("??????????????????????????????{},??????????????????????????????????????????????????????,???????????????", personFromSSO.getId());
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
        //????????????
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
                log.error("???????????????:{} ?????? {} ", upstreamType.getDescription(), e.getMessage());
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
            throw new CustomException(ResultCode.FAILED, "?????????????????????,?????????");
        }

    }

    @Override
    public PersonConnection preViewPersons(Map<String, Object> arguments, DomainInfo domain) {
        Integer i = personDao.findPersonTempCount(null, domain);
        //??????????????????????????????
        if (i <= 0 || CollectionUtils.isEmpty(personPreViewData) || (!CollectionUtils.isEmpty(personPreViewData) && CollectionUtils.isEmpty(personPreViewData.get(domain.getId())))) {
            this.reFreshPersons(arguments, domain, null);
            return null;
        }
        Tenant tenant = tenantDao.findByDomainName(domain.getDomainName());
        if (null == tenant) {
            throw new CustomException(ResultCode.FAILED, "???????????????");
        }


        List<DynamicValue> dynamicValues = new ArrayList<>();

        List<DynamicAttr> dynamicAttrs = dynamicAttrDao.findAllByType(TYPE, tenant.getId());

        if (!CollectionUtils.isEmpty(dynamicAttrs)) {

            //????????????value
            List<String> attrIds = dynamicAttrs.stream().map(DynamicAttr -> DynamicAttr.getId()).collect(Collectors.toList());

            dynamicValues = dynamicValueDao.findAllByAttrId(attrIds, tenant.getId());
        }

        //?????????????????????
        Map<String, List<DynamicValue>> valueMap = new ConcurrentHashMap<>();
        if (!CollectionUtils.isEmpty(dynamicValues)) {
            valueMap = dynamicValues.stream().filter(dynamicValue -> !StringUtils.isBlank(dynamicValue.getEntityId())).collect(Collectors.groupingBy(dynamicValue -> dynamicValue.getEntityId()));
        }
        Map<String, List<DynamicValue>> finalValueMap = valueMap;

        List<Person> personList = personPreViewData.get(domain.getId());
        PersonConnection personConnection = new PersonConnection();
        List<PersonEdge> upstreamDept = new ArrayList<>();

        Map<String, Person> preViewPersonMap = personList.stream().filter(person -> !StringUtils.isBlank(person.getId())).collect(Collectors.toMap(person -> (person.getId()), person -> person, (v1, v2) -> v2));
        //??????????????????
        List<Person> people = personDao.findPersonTemp(arguments, domain);
        Integer personTempCount = personDao.findPersonTempCount(arguments, domain);
        personConnection.setTotalCount(personTempCount);
        if (!CollectionUtils.isEmpty(people)) {
            for (Person person : people) {
                PersonEdge personEdge = new PersonEdge();
                person = preViewPersonMap.get(person.getId());
                //???????????????????????????????????????sso????????????????????????
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
        ////???????????????
        //if (null == preViewTask) {
        //    preViewTask = new ConcurrentHashMap<>();
        //}
        if (null == viewTask) {
            viewTask = new PreViewTask();
            viewTask.setTaskId(UUID.randomUUID().toString());
            viewTask.setStatus("doing");
            viewTask.setDomain(domain.getId());
            viewTask.setType("person");
        }
        //???????????????????????????????????????
        Integer count = preViewTaskService.findByTypeAndStatus("person", "doing", domain);
        if (count <= 10) {
            //PersonServiceImpl.preViewTask.put(viewResult.getTaskId(), viewResult);
            viewTask = preViewTaskService.saveTask(viewTask);
        } else {
            //Optional<String> first = PersonServiceImpl.preViewTask.keySet().stream().findFirst();
            //String s = first.get();
            //if (null != PersonServiceImpl.preViewTask.get(s) && PersonServiceImpl.preViewTask.get(s).getStatus().equals("done")) {
            //    PersonServiceImpl.preViewTask.remove(s);
            //    PersonServiceImpl.preViewTask.put(viewResult.getTaskId(), viewResult);
            //} else {
            throw new CustomException(ResultCode.FAILED, "??????????????????????????????,??????????????????????????????,???????????????");
            //}

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
        //??????????????????
        TaskConfig.errorData.put(domain.getId(), "");
        personErrorData = new ConcurrentHashMap<>();
        Tenant tenant = tenantDao.findByDomainName(domain.getDomainName());
        if (null == tenant) {
            throw new CustomException(ResultCode.FAILED, "???????????????");
        }
        // ??????????????????
        List<CardType> cardTypes = cardTypeDao.findAllUser(tenant.getId());
        Map<String, CardType> cardTypeMap = cardTypes.stream().collect(Collectors.toMap(CardType::getCardTypeCode, CardType -> CardType));
        //????????????????????????
        pwdConfig = configService.getPasswordConfigByTenantIdAndStatusAndPluginNameAndDelMarkIsFalse(tenant.getId(), "ENABLED", "CommonPlugin");

        //????????????????????????
        List<String> dynamicCodes = new ArrayList<>();

        List<DynamicValue> dynamicValues = new ArrayList<>();

        List<DynamicAttr> dynamicAttrs = dynamicAttrDao.findAllByType(TYPE, tenant.getId());
        log.info("?????????????????????{}?????????????????????{}", tenant.getId(), dynamicAttrs);

        //????????????????????????
        Map<String, DynamicValue> valueUpdateMap = new ConcurrentHashMap<>();
        //????????????????????????
        Map<String, DynamicValue> valueInsertMap = new ConcurrentHashMap<>();

        if (!CollectionUtils.isEmpty(dynamicAttrs)) {
            dynamicCodes = dynamicAttrs.stream().map(DynamicAttr -> DynamicAttr.getCode()).collect(Collectors.toList());

            //????????????value
            List<String> attrIds = dynamicAttrs.stream().map(DynamicAttr -> DynamicAttr.getId()).collect(Collectors.toList());

            dynamicValues = dynamicValueDao.findAllByAttrId(attrIds, tenant.getId());
        }

        //?????????????????????
        Map<String, List<DynamicValue>> valueMap = new ConcurrentHashMap<>();
        if (!CollectionUtils.isEmpty(dynamicValues)) {
            valueMap = dynamicValues.stream().filter(dynamicValue -> !StringUtils.isBlank(dynamicValue.getEntityId())).collect(Collectors.groupingBy(dynamicValue -> dynamicValue.getEntityId()));
        }
        List<String> finalDynamicCodes = dynamicCodes;
        Map<String, List<DynamicValue>> finalValueMap = valueMap;


        // ?????????????????????????????????
        Map<String, List<Person>> result = new HashMap<>();
        //????????????id???code??????map
        Map<String, String> attrMap = new ConcurrentHashMap<>();
        Map<String, String> attrReverseMap = new ConcurrentHashMap<>();
        log.info("----------------- upstream Person start:{}", System.currentTimeMillis());
        List<Person> personList = null;
        try {
            List<Node> nodes= nodeDao.findNodes(arguments, domain.getId());
            if (null == nodes || nodes.size() <= 0) {
                log.error( "???????????????????????????");

                throw new CustomException(ResultCode.FAILED, "???????????????????????????");
            }
            String nodeId = nodes.get(0).getId();
            //
            List<NodeRules> userRules = rulesDao.getByNodeAndType(nodeId, 1, null, 0);
            if (null == userRules || userRules.size() == 0) {
                log.error( "???????????????????????????");
                throw new CustomException(ResultCode.FAILED, "?????????????????????");
            }
            personList = dataProcessing(nodes,userRules,domain, tenant, cardTypeMap, dynamicAttrs, valueUpdateMap, valueInsertMap, finalDynamicCodes, finalValueMap, result, attrMap, attrReverseMap, arguments, null);
        } catch (Exception e) {
            e.printStackTrace();
            throw new CustomException(ResultCode.FAILED, e.getMessage());
        }
        log.info("----------------- upstream Person end:{}", System.currentTimeMillis());
        //??????????????????(??????????????????????????????)
        personDao.removeData(domain);
        Integer i = personDao.findPersonTempCount(null, domain);
        log.info("---------------??????:{},????????????????????????:{}", domain.getId(), i);
        personDao.saveToTemp(personList, domain);
        if (null == personPreViewData) {
            personPreViewData = new ConcurrentHashMap<>();
        }
        personPreViewData.put(domain.getId(), personList);
        if (null != viewTask) {
            viewTask.setStatus("done");
            viewTask.setUpdateTime(new Timestamp(System.currentTimeMillis()));
            preViewTaskService.saveTask(viewTask);
            log.info("??????????????????,??????id???:{}", viewTask.getTaskId());
        }
    }

    @Override
    public PreViewTask reFreshTaskStatus(Map<String, Object> arguments, DomainInfo domain) {
        Object id = arguments.get("taskId");
        return preViewTaskService.findByTaskId(id, domain);
    }

    /**
     * @param valueUpdateMap  ??????????????????map
     * @param valueInsertMap  ??????????????????map
     * @param attrMap         ????????????  id ???code  ??????map
     * @param ssoBean         sso??????
     * @param dynamic         ??????????????????
     * @param dyValuesFromSSO sso???????????????
     * @return
     */
    private Boolean dynamicProcessing(Map<String, DynamicValue> valueUpdateMap, Map<String, DynamicValue> valueInsertMap, Map<String, String> attrMap, Person ssoBean, Map<String, String> dynamic, List<DynamicValue> dyValuesFromSSO) {
        Boolean valueFlag = false;
        //???????????????????????????
        ArrayList<DynamicValue> dynValues = new ArrayList<>();
        if (!CollectionUtils.isEmpty(dyValuesFromSSO)) {
            Map<String, DynamicValue> collect = dyValuesFromSSO.stream().collect(Collectors.toMap(DynamicValue::getAttrId, dynamicValue -> dynamicValue));
            for (Map.Entry<String, String> str : attrMap.entrySet()) {
                String o = dynamic.get(str.getValue());
                if (collect.containsKey(str.getKey())) {
                    DynamicValue dynamicValue = collect.get(str.getKey());
                    if (null != o && !o.equals(dynamicValue.getValue())) {
                        log.info("??????{}??????????????????{}->{},??????????????????", ssoBean.getName() + ":" + ssoBean.getAccountNo(), dynamicValue.getValue(), o);
                        dynamicValue.setValue(o);
                        //????????????????????????
                        dynamicValue.setKey(dynamicValue.getAttrId());
                        dynamicValue.setCode(str.getValue());
                        dynValues.add(dynamicValue);
                        valueUpdateMap.put(dynamicValue.getAttrId() + "-" + dynamicValue.getEntityId(), dynamicValue);
                        valueFlag = true;
                    } else {
                        //?????????????????????person
                        //????????????????????????
                        dynamicValue.setKey(dynamicValue.getAttrId());
                        dynamicValue.setCode(str.getValue());
                        dynValues.add(dynamicValue);
                    }
                } else {
                    if (dynamic.containsKey(str.getValue())) {
                        //?????????  ????????????????????????
                        DynamicValue dynamicValue = new DynamicValue();
                        dynamicValue.setId(UUID.randomUUID().toString());
                        dynamicValue.setValue(o);
                        dynamicValue.setEntityId(ssoBean.getId());
                        dynamicValue.setAttrId(str.getKey());
                        valueFlag = true;
                        log.info("??????{}??????????????????{}", ssoBean.getName() + ":" + ssoBean.getAccountNo(), o);
                        //????????????????????????
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
                    //?????????  ????????????????????????
                    DynamicValue dynamicValue = new DynamicValue();
                    dynamicValue.setId(UUID.randomUUID().toString());
                    dynamicValue.setValue(o);
                    dynamicValue.setEntityId(ssoBean.getId());
                    dynamicValue.setAttrId(str.getKey());
                    log.info("??????{}??????????????????{}", ssoBean.getName() + ":" + ssoBean.getAccountNo(), o);
                    valueInsertMap.put(dynamicValue.getAttrId() + "-" + dynamicValue.getEntityId(), dynamicValue);
                    //????????????????????????
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

}
