package com.qtgl.iga.service.impl;


import com.alibaba.druid.util.StringUtils;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.qtgl.iga.bean.PersonConnection;
import com.qtgl.iga.bean.PersonEdge;
import com.qtgl.iga.bo.*;
import com.qtgl.iga.dao.*;
import com.qtgl.iga.service.PersonService;
import com.qtgl.iga.utils.ClassCompareUtil;
import com.qtgl.iga.utils.DataBusUtil;
import com.qtgl.iga.utils.enumerate.ResultCode;
import com.qtgl.iga.utils.exception.CustomException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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


    /**
     * 1:  循环遍历参与治理的所有权威源
     * 2： 拉取权威源数据并根据 证件类型+证件 号码进行和重，并验证证件类型对应的号码是否符合 正则表达式
     * 3： 如果有手动合重规则，运算手动合重规则【待确认】
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
        // 根据证件类型+证件号进行合重
        Map<String, Person> personFromUpstream = new ConcurrentHashMap<>();
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
            List<Person> personBeanList = dataByBus.toJavaList(Person.class);
            if (null != personBeanList) {
                for (Person personBean : personBeanList) {
                    if (StringUtils.isEmpty(personBean.getName())) {
                        log.warn("姓名为空");
                        continue;
                    }
                    if (StringUtils.isEmpty(personBean.getCardNo()) && StringUtils.isEmpty(personBean.getCardType())) {
                        log.warn(personBean.getName() + "证件类型、号码为空");
                        continue;
                    }
                    if (cardTypeMap.containsKey(personBean.getCardType())) {
                        String cardTypeReg = cardTypeMap.get(personBean.getCardType()).getCardTypeReg();
                        if (!Pattern.matches(cardTypeReg, personBean.getCardNo())) {
                            log.warn(personBean.getName() + "证件号码不符合规则");
                            continue;
                        }
                    } else {
                        log.warn(personBean.getName() + "证件类型无效");
                        continue;
                    }
                    personBean.setSource(upstreams.get(0).getAppName() + "(" + upstreams.get(0).getAppCode() + ")");
                    personBean.setUpstreamType(upstreamType.getId());
                    personBean.setCreateTime(now);
                    if (null == personBean.getUpdateTime()) {
                        personBean.setUpdateTime(now);
                    }
                    if (null == personBean.getDelMark()) {
                        personBean.setDelMark(0);
                    }

                    personFromUpstream.put(personBean.getCardType() + ":" + personBean.getCardNo(), personBean);
                }
            }
        });

        log.info("所有人员数据获取完成:{}", personFromUpstream.size());
        /*
          将合重后的数据插入进数据库
        */
        // 获取 sso数据
        List<Person> personFromSSO = personDao.getAll(tenant.getId());
        Map<String, Person> personFromSSOMap = personFromSSO.stream().filter(person -> !StringUtils.isEmpty(person.getCardType()) && !StringUtils.isEmpty(person.getCardNo())).collect(Collectors.toMap(person -> (person.getCardType() + ":" + person.getCardNo()), person -> person, (v1, v2) -> v2));
        // 存储最终需要操作的数据
        Map<String, List<Person>> result = new HashMap<>();

        personFromSSOMap.forEach((key, val) -> {
            // 对比出需要修改的person
            if (personFromUpstream.containsKey(key) &&
                    personFromUpstream.get(key).getCreateTime().isAfter(val.getUpdateTime())) {
                boolean flag = false;
                boolean delFlag = true;
                Person newPerson = personFromUpstream.get(key);
                List<UpstreamTypeField> fields = DataBusUtil.typeFields.get(newPerson.getUpstreamType());
                // 如果字段上游不提供，则不进行更新
                //    字段值没有发生改变，不进行更新
                if (null != fields && fields.size() > 0) {
                    for (UpstreamTypeField field : fields) {
                        String sourceField = field.getSourceField();
                        Object newValue = ClassCompareUtil.getGetMethod(newPerson, sourceField);
                        Object oldValue = ClassCompareUtil.getGetMethod(val, sourceField);
                        if (null == oldValue && null == newValue) {
                            continue;
                        }
                        if (null != oldValue && oldValue.equals(newValue)) {
                            continue;
                        }
                        if (sourceField.equals("delMark") && (Integer) oldValue == 1 && (Integer) newValue == 0) {
                            delFlag = false;
                            log.info("人员信息{}从删除恢复", val.getId());
                        }
                        if (sourceField.equals("delMark") && (Integer) oldValue == 0 && (Integer) newValue == 1) {
                            log.info("人员信息{}删除", val.getId());
                        }
                        flag = true;
                        ClassCompareUtil.setValue(val, val.getClass(), sourceField, oldValue, newValue);
                        log.debug("人员信息更新{}:字段{}：{} -> {}", val.getId(), sourceField, oldValue, newValue);
                    }
                }


                if (val.getDelMark().equals(1) && delFlag) {
                    flag = true;
                    val.setDelMark(0);
                    log.info("人员信息{}从删除恢复", val.getId());
                }
                if (flag) {
                    val.setSource(newPerson.getSource());
                    val.setUpdateTime(newPerson.getUpdateTime());
                    log.info("对比后需要修改{}", val);
                    if (result.containsKey("update")) {
                        result.get("update").add(val);
                    } else {
                        result.put("update", new ArrayList<Person>() {{
                            this.add(val);
                        }});
                    }
                }

            } else if (!personFromUpstream.containsKey(key) && 1 != val.getDelMark() && "PULL".equalsIgnoreCase(val.getDataSource())) {
                val.setUpdateTime(now);
                if (result.containsKey("delete")) {
                    result.get("delete").add(val);
                } else {
                    result.put("delete", new ArrayList<Person>() {{
                        this.add(val);
                    }});
                }

                log.info("人员对比后删除{}", val);
            }
        });

        personFromUpstream.forEach((key, val) -> {
            if (!personFromSSOMap.containsKey(key)) {
                val.setId(UUID.randomUUID().toString());
                val.setOpenId(RandomStringUtils.randomAlphanumeric(20));
                if (result.containsKey("install")) {
                    result.get("install").add(val);
                } else {
                    result.put("install", new ArrayList<Person>() {{
                        this.add(val);
                    }});
                }
                log.debug("人员对比后新增{}", val);
            }
        });


        // 验证监控规则
        calculationService.monitorRules(domain, taskLog, personFromSSO.size(), result.get("delete"));

        // sso 批量新增
        personDao.saveToSso(result, tenant.getId());


        return result;
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
