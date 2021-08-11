package com.qtgl.iga.service.impl;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.qtgl.iga.bean.PersonConnection;
import com.qtgl.iga.bean.PersonEdge;
import com.qtgl.iga.bo.*;
import com.qtgl.iga.dao.*;
import com.qtgl.iga.service.PersonService;
import com.qtgl.iga.task.TaskConfig;
import com.qtgl.iga.utils.ClassCompareUtil;
import com.qtgl.iga.utils.DataBusUtil;
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
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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

    public static ConcurrentHashMap<String, List<Person>> errorData = new ConcurrentHashMap<>();

    /**
     * 如果有手动合重规则，运算手动合重规则【待确认】
     * <p>
     * * 1：根据规则获取所有的 人员数据
     * * 2： 根据 (证件类型+证件号码)  或 用户名 进行和重，并验证证件类型对应的号码是否符合 正则表达式
     * *
     * * 3：根据上游人员和数据库中人员进行对比
     * * A：新增  上游提供、sso数据库中没有
     * * B：修改  上游和sso对比后字段值有差异
     * * C：删除  上游提供了del_mark
     * * D: 无效  上游曾经提供后，不再提供 OR 上游提供了active
     * * E: 恢复  之前被标记为失效后再通过推送了相同的数据
     * TODO： 一对多的证件。 一个人可以有多个证件；身份证、护照等
     */

    @Override
    public Map<String, List<Person>> buildPerson(DomainInfo domain, TaskLog taskLog) throws Exception {
        Tenant tenant = tenantDao.findByDomainName(domain.getDomainName());
        if (null == tenant) {
            throw new CustomException(ResultCode.FAILED, "租户不存在");
        }
        // 所有证件类型
        List<CardType> cardTypes = cardTypeDao.findAll(tenant.getId());
        Map<String, CardType> cardTypeMap = cardTypes.stream().collect(Collectors.toMap(CardType::getCardTypeCode, CardType -> CardType));


        // 获取规则
        Map arguments = new ConcurrentHashMap();
        arguments.put("type", "person");
        arguments.put("status", 0);
        List<Node> nodes = nodeDao.findNodes(arguments, domain.getId());
        if (null == nodes || nodes.size() <= 0) {
            throw new CustomException(ResultCode.FAILED, "无人员管理规则信息");
        }
        String nodeId = nodes.get(0).getId();
        //
        List<NodeRules> userRules = rulesDao.getByNodeAndType(nodeId, 1, true, 0);
        // 根据证件类型+证件号
        Map<String, Person> personFromUpstream = new ConcurrentHashMap<>();
        // 根据用户名
        Map<String, Person> personFromUpstreamByAccount = new ConcurrentHashMap<>();
        //合重容器
        Map<String, Person> personRepeatByAccount = new ConcurrentHashMap<>();

        final LocalDateTime now = LocalDateTime.now();
        userRules.forEach(rules -> {
            // 通过规则获取数据
            UpstreamType upstreamType = upstreamTypeDao.findById(rules.getUpstreamTypesId());
            ArrayList<Upstream> upstreams = upstreamDao.getUpstreams(upstreamType.getUpstreamId(), domain.getId());
            JSONArray dataByBus = null;
            try {
                dataByBus = dataBusUtil.getDataByBus(upstreamType, domain.getDomainName());
            } catch (Exception e) {
                log.error("人员治理中类型 : " + upstreamType.getUpstreamId() + "表达式异常");
            }

            List<Person> personUpstreamList = dataByBus.toJavaList(Person.class);
            //将该源数据放入errorData,方便程序运行异常后排查数据源头问题
            TaskConfig.errorData.put(domain.getId(), JSON.toJSONString(JSON.toJSON(personUpstreamList)));

            //遍历权威源数据进行数据规范化
            if (null != personUpstreamList) {
                for (Person personUpstream : personUpstreamList) {

                    if (null != personUpstream.getActive() && personUpstream.getActive() != 0 && personUpstream.getActive() != 1) {
                        log.error("人员是否有效字段不合法:{}", personUpstream.getActive());
                        continue;
                    }
                    if (null != personUpstream.getDelMark() && personUpstream.getDelMark() != 0 && personUpstream.getDelMark() != 1) {
                        log.error("人员是否删除字段不合法:{}", personUpstream.getDelMark());
                        continue;
                    }
                    // 人员标识 证件类型、证件号码   OR    用户名 accountNo  必提供一个
                    if (StringUtils.isBlank(personUpstream.getCardNo()) && StringUtils.isBlank(personUpstream.getCardType())) {
                        if (StringUtils.isBlank(personUpstream.getAccountNo())) {
                            log.error( "{}未提供标识信息:证件类型、证件号码或者用户名",personUpstream.getName());
                            continue;
                        }

                    }
                    // 如果提供证件类型,提供的证件号码为空
                    if (StringUtils.isBlank(personUpstream.getCardNo()) && !StringUtils.isBlank(personUpstream.getCardType())) {
                        if (errorData.containsKey(personUpstream.getAccountNo())) {
                            errorData.get(personUpstream.getAccountNo()).add(personUpstream);
                        } else {
                            errorData.put(personUpstream.getAccountNo(), new ArrayList<Person>() {{
                                this.add(personUpstream);
                            }});
                        }
                        log.error("{}-{}提供证件类型但对应的证件号码为空",personUpstream.getCardNo(), personUpstream.getAccountNo());
                        continue;
                    }
                    if (!StringUtils.isBlank(personUpstream.getCardType()) && cardTypeMap.containsKey(personUpstream.getCardType())) {
                        String cardTypeReg = cardTypeMap.get(personUpstream.getCardType()).getCardTypeReg();
                        if (null != cardTypeReg && !Pattern.matches(cardTypeReg, personUpstream.getCardNo())) {
                            log.error("证件号码不符合规则:{}",personUpstream.getCardNo());
                            continue;
                        }
                    } else if (!StringUtils.isBlank(personUpstream.getCardType()) && !cardTypeMap.containsKey(personUpstream.getCardType())) {
                        log.error("证件类型无效:{}",personUpstream.getCardType());
                        continue;
                    }

                    if (StringUtils.isBlank(personUpstream.getName())) {
                        log.error("{}-{}姓名为空",personUpstream.getCardNo(), personUpstream.getAccountNo());
                        continue;
                    }

                    personUpstream.setSource(upstreams.get(0).getAppName() + "(" + upstreams.get(0).getAppCode() + ")");
                    personUpstream.setUpstreamType(upstreamType.getId());
                    personUpstream.setCreateTime(now);

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

                    // 人员标识 证件类型、证件号码   OR    用户名 accountNo  必提供一个
                    if (StringUtils.isBlank(personUpstream.getCardNo()) && StringUtils.isBlank(personUpstream.getCardType())) {
                        //如果用户名不为空
                        if (StringUtils.isNotEmpty(personUpstream.getAccountNo())) {
                            //并且合重容器中包含该用户名
                            if (personRepeatByAccount.containsKey(personUpstream.getAccountNo())) {
                                Person person = personRepeatByAccount.get(personUpstream.getAccountNo());
                                //有效并且合重容器中的数据没有证件标识 才进行合重复 否则跳过
                                if (personUpstream.getActive() == 1 && StringUtils.isBlank(person.getCardType()) && StringUtils.isBlank(person.getCardNo())) {
                                    personFromUpstreamByAccount.put(personUpstream.getAccountNo(), personUpstream);
                                    personRepeatByAccount.put(personUpstream.getAccountNo(), personUpstream);
                                }
                            } else {
                                //合重容器中没有对应用户名标识数据,则添加进容器
                                personFromUpstreamByAccount.put(personUpstream.getAccountNo(), personUpstream);
                                personRepeatByAccount.put(personUpstream.getAccountNo(), personUpstream);
                            }

                        }
                    } else {
                        //既有证件标识又有用户名标识,处理用户名标识
                        if (StringUtils.isNotEmpty(personUpstream.getAccountNo())) {
                            if (personFromUpstream.containsKey(personUpstream.getCardType() + ":" + personUpstream.getCardNo())) {
                                //查询之前数据是否已有该用户名的数据
                                if (personRepeatByAccount.containsKey(personUpstream.getAccountNo())) {
                                    //有效才进行合重
                                    if (personUpstream.getActive() == 1) {
                                        Person person = personRepeatByAccount.get(personUpstream.getAccountNo());
                                        //有该用户名数据则判断证件标识是否一致,一致则进行覆盖
                                        if (StringUtils.isNotEmpty(person.getCardNo()) && StringUtils.isNotEmpty(person.getCardType())) {
                                            if (person.getCardType().equals(personUpstream.getCardType()) && personUpstream.getCardNo().equals(person.getCardNo())) {
                                                personFromUpstream.put(personUpstream.getCardType() + ":" + personUpstream.getCardNo(), personUpstream);
                                                personRepeatByAccount.put(personUpstream.getAccountNo(), personUpstream);
                                            } else {
                                                //有效且同一用户名对应不同证件类型,放入errorData
                                                extracted(personUpstream, person);
                                                log.error("用户名{}下有不同证件类型的数据{}{},请检查源数据", person.getAccountNo(), person, personUpstream);
                                            }
                                        } else {
                                            personFromUpstream.put(personUpstream.getCardType() + ":" + personUpstream.getCardNo(), personUpstream);
                                            personRepeatByAccount.put(personUpstream.getAccountNo(), personUpstream);
                                        }

                                    }
                                } else {
                                    //用户名不重复
                                    if (personUpstream.getActive() == 1) {
                                        //有效则覆盖
                                        personFromUpstream.put(personUpstream.getCardType() + ":" + personUpstream.getCardNo(), personUpstream);
                                        personRepeatByAccount.put(personUpstream.getAccountNo(), personUpstream);
                                    }
                                }
                            } else {
                                //合重容器中没有对应用户名标识数据,则添加进容器
                                personFromUpstream.put(personUpstream.getCardType() + ":" + personUpstream.getCardNo(), personUpstream);
                                personRepeatByAccount.put(personUpstream.getAccountNo(), personUpstream);
                            }

                        } else {
                            //仅提供证件标识
                            //合重
                            if (personFromUpstream.containsKey(personUpstream.getCardType() + ":" + personUpstream.getCardNo())) {
                                //有效则进行覆盖
                                if (personUpstream.getActive() == 1) {
                                    personFromUpstream.put(personUpstream.getCardType() + ":" + personUpstream.getCardNo(), personUpstream);
                                }
                            } else {
                                personFromUpstream.put(personUpstream.getCardType() + ":" + personUpstream.getCardNo(), personUpstream);
                            }

                        }

                    }

                }
            }
        });
        log.info("所有人员数据获取完成:{}", personFromUpstream.size() + personFromUpstreamByAccount.size());
        if (personFromUpstream.size() > 0 || personFromUpstreamByAccount.size() > 0) {
            TaskConfig.errorData.put(domain.getId(), "");
             /*
                将合重后的数据插入进数据库
            */
            // 获取 sso数据
            List<Person> personFromSSOList = personDao.getAll(tenant.getId());
            // 存储最终需要操作的数据
            Map<String, List<Person>> result = new HashMap<>();

            // 将数据库中  证件类型 && 证件号不为空的
            Map<String, Person> personFromSSOMap = personFromSSOList.stream().filter(person -> !StringUtils.isBlank(person.getCardType()) && !StringUtils.isBlank(person.getCardNo())).collect(Collectors.toMap(person -> (person.getCardType() + ":" + person.getCardNo()), person -> person, (v1, v2) -> v2));
            Map<String, Person> personFromSSOMapCopy = personFromSSOList.stream().filter(person -> !StringUtils.isBlank(person.getCardType()) && !StringUtils.isBlank(person.getCardNo())).collect(Collectors.toMap(person -> (person.getCardType() + ":" + person.getCardNo()), person -> person, (v1, v2) -> v2));
            // 将数据库中 用户名不为空&&证件标识为空的
            Map<String, Person> personFromSSOMapByAccount = personFromSSOList.stream().filter(person ->
                    !StringUtils.isBlank(person.getAccountNo()) && (StringUtils.isBlank(person.getCardNo()) || StringUtils.isBlank(person.getCardType()))
            ).collect(Collectors.toMap(Person::getAccountNo, person -> person, (v1, v2) -> v2));
            // 将数据库中 用户名不为空
            Map<String, Person> personFromSSOMapByAccountAll = personFromSSOList.stream().filter(person ->
                    !StringUtils.isBlank(person.getAccountNo())).collect(Collectors.toMap(Person::getAccountNo, person -> person, (v1, v2) -> v2));


            personFromSSOMap.forEach((key, personFromSSO) -> {
                calculate(personFromUpstream, personFromSSOMapByAccountAll, personFromSSOMapCopy, now, result, key, personFromSSO);
            });

            personFromSSOMapByAccount.forEach((key, personFromSSO) -> {
                calculate(personFromUpstreamByAccount, now, result, key, personFromSSO);
            });

            personFromUpstreamByAccount.forEach((key, val) -> {
                calculateInsert(personFromSSOMapByAccount, result, key, val);
            });
            personFromUpstream.forEach((key, val) -> {
                calculateInsert(personFromSSOMapCopy, personFromSSOMapByAccountAll, result, key, val);
            });


            // 验证监控规则
            calculationService.monitorRules(domain, taskLog, personFromSSOList.size(), result.get("delete"));
            // sso 批量新增
            personDao.saveToSso(result, tenant.getId());
            TaskConfig.errorData.put(domain.getId(), JSON.toJSONString(JSON.toJSON(new ArrayList<>(errorData.values()))));
            return result;
        } else {
            log.error("上游提供人员数据不符合规范,数据同步失败");
            throw new CustomException(ResultCode.FAILED, "上游提供人员数据不符合规范,数据同步失败");
        }


    }

    //处理异常数据
    private void extracted(Person personBean, Person person) {
        if (errorData.containsKey(person.getAccountNo())) {
            errorData.get(person.getAccountNo()).add(person);
            errorData.get(person.getAccountNo()).add(personBean);
        } else {
            errorData.put(person.getAccountNo(), new ArrayList<Person>() {{
                this.add(person);
                this.add(personBean);
            }});
        }
    }

    private void calculateInsert(Map<String, Person> personFromSSOMap, Map<String, List<Person>> result, String key, Person
            val) {
        //sso没有并且未删除标记的数据才进行新增
        if (!personFromSSOMap.containsKey(key) && (val.getDelMark() == 0)) {
            //新增逻辑字段赋默认值
            val.setId(UUID.randomUUID().toString());
            val.setOpenId(RandomStringUtils.randomAlphabetic(20));
            val.setValidStartTime(LocalDateTime.of(1970, 1, 1, 0, 0, 0));
            val.setValidEndTime(LocalDateTime.of(2100, 1, 1, 0, 0, 0));

            // 如果新增的数据 active=0 失效 或者 del_mark=1 删除  或者 判断为孤儿
            //   都将 最终有效期设置为 失效
            if (val.getActive() == 0 || val.getDelMark() == 1) {
                val.setValidStartTime(LocalDateTime.of(1970, 1, 1, 0, 0, 0));
                val.setValidEndTime(LocalDateTime.of(1970, 1, 1, 0, 0, 0));
            }
            if (result.containsKey("insert")) {
                result.get("insert").add(val);
            } else {
                result.put("insert", new ArrayList<Person>() {{
                    this.add(val);
                }});
            }
            // 对新增的用户 判断是否提供 password字段
            if (!StringUtils.isBlank(val.getPassword())) {
                String password = val.getPassword();
                try {
                    password = "{MD5}" + Base64.encodeBase64String(Hex.decodeHex(DigestUtils.md5DigestAsHex(password.getBytes()).toCharArray()));
                    val.setPassword(password);
                } catch (DecoderException e) {
                    e.printStackTrace();
                }
                if (result.containsKey("password")) {
                    result.get("password").add(val);
                } else {
                    result.put("password", new ArrayList<Person>() {{
                        this.add(val);
                    }});
                }
            }
            log.debug("人员对比后新增{}", val);
        }
    }

    private void calculate(Map<String, Person> personFromUpstream, LocalDateTime now, Map<String, List<Person>> result, String key, Person personFromSSO) {
        // 对比出需要修改的person
        if (personFromUpstream.containsKey(key) &&
                personFromUpstream.get(key).getCreateTime().isAfter(personFromSSO.getUpdateTime())) {
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

            Person newPerson = personFromUpstream.get(key);
            List<UpstreamTypeField> fields = DataBusUtil.typeFields.get(newPerson.getUpstreamType());
            // 如果字段上游不提供，则不进行更新
            //    字段值没有发生改变，不进行更新
            if (null != fields && fields.size() > 0) {
                for (UpstreamTypeField field : fields) {
                    String sourceField = field.getSourceField();
                    Object newValue = ClassCompareUtil.getGetMethod(newPerson, sourceField);
                    Object oldValue = ClassCompareUtil.getGetMethod(personFromSSO, sourceField);
                    //对于密码字段不处理
                    if (sourceField.equalsIgnoreCase("password")) {
                        if (null == oldValue) {
                            if (result.containsKey("password")) {
                                result.get("password").add(personFromSSO);
                            } else {
                                result.put("password", new ArrayList<Person>() {{
                                    this.add(personFromSSO);
                                }});
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
                    if (sourceField.equalsIgnoreCase("delMark") && (Integer) oldValue == 0 && (Integer) newValue == 1) {
                        delFlag = true;
                        log.info("人员信息{}删除", personFromSSO.getId());
                        continue;
                    }

                    updateFlag = true;
                    if (sourceField.equalsIgnoreCase("active") && (Integer) oldValue == 1 && (Integer) newValue == 0) {
                        invalidFlag = true;
                        log.info("人员信息{}失效", personFromSSO.getId());
                        // continue;
                    }
                    if (sourceField.equalsIgnoreCase("password")) {
                        //   if (StringUtils.isBlank((String) oldValue) && !StringUtils.isBlank((String) newValue)) {
                        try {
                            String password = "{MD5}" + Base64.encodeBase64String(Hex.decodeHex(DigestUtils.md5DigestAsHex(((String) newValue).getBytes()).toCharArray()));
                            passwordFlag = true;
                            personFromSSO.setPassword(password);
                        } catch (DecoderException e) {
                            e.printStackTrace();
                        }
                        continue;
                        // }
                    }
                  /*  if (sourceField.equalsIgnoreCase("active") && (Integer) oldValue == 0 && (Integer) newValue == 1) {
                        invalidRecoverFlag = false;
                    }*/

                    ClassCompareUtil.setValue(personFromSSO, personFromSSO.getClass(), sourceField, oldValue, newValue);
                    log.debug("人员信息更新{}:字段{}：{} -> {}", personFromSSO.getId(), sourceField, oldValue, newValue);
                }
            }


            if (delFlag) {
                personFromSSO.setDelMark(1);
                personFromSSO.setUpdateTime(newPerson.getUpdateTime());
                personFromSSO.setValidStartTime(LocalDateTime.of(1970, 1, 1, 0, 0, 0));
                personFromSSO.setValidEndTime(LocalDateTime.of(1970, 1, 1, 0, 0, 0));
                if (result.containsKey("delete")) {
                    result.get("delete").add(personFromSSO);
                } else {
                    result.put("delete", new ArrayList<Person>() {{
                        this.add(personFromSSO);
                    }});
                }
                log.info("人员信息删除{}", personFromSSO.getId());
            }
            if (updateFlag && personFromSSO.getDelMark() != 1) {
                personFromSSO.setSource(newPerson.getSource());
                personFromSSO.setUpdateTime(newPerson.getUpdateTime());
                // 需要设置人员密码
                if (passwordFlag) {
                    if (result.containsKey("password")) {
                        result.get("password").add(personFromSSO);
                    } else {
                        result.put("password", new ArrayList<Person>() {{
                            this.add(personFromSSO);
                        }});
                    }
                }
                //失效
                if (invalidFlag) {
                    personFromSSO.setActive(0);
                    personFromSSO.setActiveTime(newPerson.getUpdateTime());
                    personFromSSO.setValidStartTime(LocalDateTime.of(1970, 1, 1, 0, 0, 0));
                    personFromSSO.setValidEndTime(LocalDateTime.of(1970, 1, 1, 0, 0, 0));
                    if (result.containsKey("invalid")) {
                        result.get("invalid").add(personFromSSO);
                    } else {
                        result.put("invalid", new ArrayList<Person>() {{
                            this.add(personFromSSO);
                        }});
                    }
                } else {
                    if (!personFromSSO.getActive().equals(newPerson.getActive())) {
                        personFromSSO.setActive(newPerson.getActive());
                        personFromSSO.setActiveTime(newPerson.getUpdateTime());
                    }
                    if (personFromSSO.getActive() == 0 || personFromSSO.getDelMark() == 1) {
                        personFromSSO.setValidStartTime(LocalDateTime.of(1970, 1, 1, 0, 0, 0));
                        personFromSSO.setValidEndTime(LocalDateTime.of(1970, 1, 1, 0, 0, 0));
                    }

                    if (result.containsKey("update")) {
                        result.get("update").add(personFromSSO);
                    } else {
                        result.put("update", new ArrayList<Person>() {{
                            this.add(personFromSSO);
                        }});
                    }
                }
                log.info("人员对比后需要修改{}", personFromSSO);

            }

            // 对比后，权威源提供的"映射字段"数据和sso中没有差异。（active字段不提供）
            if (!updateFlag && personFromSSO.getDelMark() != 1) {
                //
                if (!personFromSSO.getActive().equals(newPerson.getActive())) {
                    personFromSSO.setActive(newPerson.getActive());
                    personFromSSO.setActiveTime(newPerson.getUpdateTime());
                    personFromSSO.setUpdateTime(newPerson.getUpdateTime());
                    if (result.containsKey("update")) {
                        result.get("update").add(personFromSSO);
                    } else {
                        result.put("update", new ArrayList<Person>() {{
                            this.add(personFromSSO);
                        }});
                    }

                }

            }

        } else if (!personFromUpstream.containsKey(key)
                && 1 != personFromSSO.getDelMark()
                && (null == personFromSSO.getActive() || personFromSSO.getActive() == 1)
                && "PULL".equalsIgnoreCase(personFromSSO.getDataSource())) {
            personFromSSO.setActive(0);
            personFromSSO.setActiveTime(now);
            personFromSSO.setUpdateTime(now);
            personFromSSO.setValidStartTime(LocalDateTime.of(1970, 1, 1, 0, 0, 0));
            personFromSSO.setValidEndTime(LocalDateTime.of(1970, 1, 1, 0, 0, 0));
            if (result.containsKey("invalid")) {
                result.get("invalid").add(personFromSSO);
            } else {
                result.put("invalid", new ArrayList<Person>() {{
                    this.add(personFromSSO);
                }});
            }

            log.info("人员对比后上游丢失{}", personFromSSO);
        }
    }

    private void calculateInsert(Map<String, Person> personFromSSOMap, Map<String, Person> personFromSSOMapByAccountAll, Map<String, List<Person>> result, String key, Person val) {
        //sso没有并且未删除标记的数据才进行新增
        if (!personFromSSOMap.containsKey(key) && (val.getDelMark() == 0)) {
            //是否执行标识
            Boolean flag = true;
            //新增逻辑字段赋默认值
            String id = UUID.randomUUID().toString();
            val.setId(id);
            val.setOpenId(RandomStringUtils.randomAlphabetic(20));
            val.setValidStartTime(LocalDateTime.of(1970, 1, 1, 0, 0, 0));
            val.setValidEndTime(LocalDateTime.of(2100, 1, 1, 0, 0, 0));
            //之前存在该用户名
            if (StringUtils.isNotEmpty(val.getAccountNo()) && personFromSSOMapByAccountAll.containsKey(val.getAccountNo())) {
                Person person = personFromSSOMapByAccountAll.get(val.getAccountNo());
                //有效标识一致,或者新增的为有效的则继续执行
                if (val.getActive() == 1 || val.getActive().equals(person.getActive())) {
                    if (StringUtils.isNotEmpty(person.getCardNo()) && StringUtils.isNotEmpty(person.getCardType())) {
                        if (person.getCardType().equals(val.getCardType()) && val.getCardNo().equals(person.getCardNo())) {
                            //todo 
                            val.setId(person.getId());
                            val.setOpenId(person.getOpenId());
                            val.setPassword(person.getPassword());
                        } else {
                            //有效且同一用户名对应不同证件类型,目前不支持
                            extracted(person, val);
                            flag = false;
                            log.error("用户名{}下有不同证件类型的数据{}{},请检查源数据", person.getAccountNo(), person, val);
                        }
                    } else {
                        val.setId(person.getId());
                        val.setOpenId(person.getOpenId());
                    }
                } else {
                    //无效无法覆盖有效,抛弃该数据
                    flag = false;
                }

            }

            if (flag) {
                // 如果新增的数据 active=0 失效 或者 del_mark=1 删除  或者 判断为孤儿
                //   都将 最终有效期设置为 失效
                if (val.getActive() == 0 || val.getDelMark() == 1) {
                    val.setValidStartTime(LocalDateTime.of(1970, 1, 1, 0, 0, 0));
                    val.setValidEndTime(LocalDateTime.of(1970, 1, 1, 0, 0, 0));
                }
                // 对新增的用户 判断是否提供 password字段
                if (!StringUtils.isBlank(val.getPassword())) {
                    String password = val.getPassword();
                    try {
                        password = "{MD5}" + Base64.encodeBase64String(Hex.decodeHex(DigestUtils.md5DigestAsHex(password.getBytes()).toCharArray()));
                        val.setPassword(password);
                    } catch (DecoderException e) {
                        e.printStackTrace();
                    }
                    if (result.containsKey("password")) {
                        result.get("password").add(val);
                    } else {
                        result.put("password", new ArrayList<Person>() {{
                            this.add(val);
                        }});
                    }
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


            }
        }
    }

    private void calculate(Map<String, Person> personFromUpstream, Map<String, Person> personFromSSOMapByAccountAll, Map<String, Person> personFromSSOMapCopy, LocalDateTime now, Map<String, List<Person>> result, String key, Person personFromSSO) {
        // 对比出需要修改的person
        if (personFromUpstream.containsKey(key) &&
                personFromUpstream.get(key).getCreateTime().isAfter(personFromSSO.getUpdateTime())) {
            //修改是否合法标识
            boolean licitFlag = true;
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

            Person newPerson = personFromUpstream.get(key);
            List<UpstreamTypeField> fields = DataBusUtil.typeFields.get(newPerson.getUpstreamType());
            // 如果字段上游不提供，则不进行更新
            //    字段值没有发生改变，不进行更新
            if (null != fields && fields.size() > 0) {
                for (UpstreamTypeField field : fields) {
                    String sourceField = field.getSourceField();
                    Object newValue = ClassCompareUtil.getGetMethod(newPerson, sourceField);
                    Object oldValue = ClassCompareUtil.getGetMethod(personFromSSO, sourceField);
                    //对于密码字段不处理
                    if (sourceField.equalsIgnoreCase("password")) {
                        if (null == oldValue) {
                            if (result.containsKey("password")) {
                                result.get("password").add(personFromSSO);
                            } else {
                                result.put("password", new ArrayList<Person>() {{
                                    this.add(personFromSSO);
                                }});
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

                    if (sourceField.equalsIgnoreCase("accountNo") && null != newValue && StringUtils.isNotEmpty(newValue.toString()) && personFromSSOMapByAccountAll.containsKey(newValue)) {
                        Person person = personFromSSOMapByAccountAll.get(newValue);
                        if (!newPerson.getCardType().equals(person.getCardType()) || !newPerson.getCardNo().equals(person.getCardNo())) {

                            extracted(newPerson, person);
                            //有效且同一用户名对应不同证件类型,目前不支持
                            log.error("该修改使用户名{}下有不同证件类型的数据{}{},请检查", person.getAccountNo(), person, newPerson);
                            licitFlag = false;
                            personFromUpstream.remove(key);
                            personFromSSOMapCopy.remove(key);
                            break;
                        }
                    }

                    if (sourceField.equalsIgnoreCase("delMark") && (Integer) oldValue == 0 && (Integer) newValue == 1) {
                        delFlag = true;
                        log.info("人员信息{}删除", personFromSSO.getId());
                        continue;
                    }

                    updateFlag = true;
                    if (sourceField.equalsIgnoreCase("active") && (Integer) oldValue == 1 && (Integer) newValue == 0) {
                        invalidFlag = true;
                        log.info("人员信息{}失效", personFromSSO.getId());
                        // continue;
                    }
                    if (sourceField.equalsIgnoreCase("password")) {
                        //   if (StringUtils.isBlank((String) oldValue) && !StringUtils.isBlank((String) newValue)) {
                        try {
                            String password = "{MD5}" + Base64.encodeBase64String(Hex.decodeHex(DigestUtils.md5DigestAsHex(((String) newValue).getBytes()).toCharArray()));
                            passwordFlag = true;
                            personFromSSO.setPassword(password);
                        } catch (DecoderException e) {
                            e.printStackTrace();
                        }
                        continue;
                        // }
                    }
                  /*  if (sourceField.equalsIgnoreCase("active") && (Integer) oldValue == 0 && (Integer) newValue == 1) {
                        invalidRecoverFlag = false;
                    }*/

                    ClassCompareUtil.setValue(personFromSSO, personFromSSO.getClass(), sourceField, oldValue, newValue);
                    log.debug("人员信息更新{}:字段{}：{} -> {}", personFromSSO.getId(), sourceField, oldValue, newValue);

                }
            }

            if (licitFlag) {
                if (delFlag) {
                    personFromSSO.setDelMark(1);
                    personFromSSO.setUpdateTime(newPerson.getUpdateTime());
                    personFromSSO.setValidStartTime(LocalDateTime.of(1970, 1, 1, 0, 0, 0));
                    personFromSSO.setValidEndTime(LocalDateTime.of(1970, 1, 1, 0, 0, 0));
                    if (result.containsKey("delete")) {
                        result.get("delete").add(personFromSSO);
                    } else {
                        result.put("delete", new ArrayList<Person>() {{
                            this.add(personFromSSO);
                        }});
                    }
                    log.info("人员信息删除{}", personFromSSO.getId());
                }
                if (updateFlag && personFromSSO.getDelMark() != 1) {
                    personFromSSO.setSource(newPerson.getSource());
                    personFromSSO.setUpdateTime(newPerson.getUpdateTime());
                    // 需要设置人员密码
                    if (passwordFlag) {
                        if (result.containsKey("password")) {
                            result.get("password").add(personFromSSO);
                        } else {
                            result.put("password", new ArrayList<Person>() {{
                                this.add(personFromSSO);
                            }});
                        }
                    }
                    //失效
                    if (invalidFlag) {
                        personFromSSO.setActive(0);
                        personFromSSO.setActiveTime(newPerson.getUpdateTime());
                        personFromSSO.setValidStartTime(LocalDateTime.of(1970, 1, 1, 0, 0, 0));
                        personFromSSO.setValidEndTime(LocalDateTime.of(1970, 1, 1, 0, 0, 0));
                        if (result.containsKey("invalid")) {
                            result.get("invalid").add(personFromSSO);
                        } else {
                            result.put("invalid", new ArrayList<Person>() {{
                                this.add(personFromSSO);
                            }});
                        }
                    } else {
                        if (!personFromSSO.getActive().equals(newPerson.getActive())) {
                            personFromSSO.setActive(newPerson.getActive());
                            personFromSSO.setActiveTime(newPerson.getUpdateTime());
                        }
                        if (personFromSSO.getActive() == 0 || personFromSSO.getDelMark() == 1) {
                            personFromSSO.setValidStartTime(LocalDateTime.of(1970, 1, 1, 0, 0, 0));
                            personFromSSO.setValidEndTime(LocalDateTime.of(1970, 1, 1, 0, 0, 0));
                        }

                        if (result.containsKey("update")) {
                            result.get("update").add(personFromSSO);
                        } else {
                            result.put("update", new ArrayList<Person>() {{
                                this.add(personFromSSO);
                            }});
                        }
                    }
                    log.info("人员对比后需要修改{}", personFromSSO);

                }

                // 对比后，权威源提供的"映射字段"数据和sso中没有差异。（active字段不提供）
                if (!updateFlag && personFromSSO.getDelMark() != 1) {
                    //
                    if (!personFromSSO.getActive().equals(newPerson.getActive())) {
                        personFromSSO.setActive(newPerson.getActive());
                        personFromSSO.setActiveTime(newPerson.getUpdateTime());
                        personFromSSO.setUpdateTime(newPerson.getUpdateTime());
                        if (result.containsKey("update")) {
                            result.get("update").add(personFromSSO);
                        } else {
                            result.put("update", new ArrayList<Person>() {{
                                this.add(personFromSSO);
                            }});
                        }

                    }

                }
            }


        } else if (!personFromUpstream.containsKey(key)
                && 1 != personFromSSO.getDelMark()
                && (null == personFromSSO.getActive() || personFromSSO.getActive() == 1)
                && "PULL".equalsIgnoreCase(personFromSSO.getDataSource())) {
            personFromSSO.setActive(0);
            personFromSSO.setActiveTime(now);
            personFromSSO.setUpdateTime(now);
            personFromSSO.setValidStartTime(LocalDateTime.of(1970, 1, 1, 0, 0, 0));
            personFromSSO.setValidEndTime(LocalDateTime.of(1970, 1, 1, 0, 0, 0));
            if (result.containsKey("invalid")) {
                result.get("invalid").add(personFromSSO);
            } else {
                result.put("invalid", new ArrayList<Person>() {{
                    this.add(personFromSSO);
                }});
            }

            log.info("人员对比后上游丢失{}", personFromSSO);
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
                dataMap = dataBusUtil.getDataByBus(upstreamType, offset, first);
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


}
