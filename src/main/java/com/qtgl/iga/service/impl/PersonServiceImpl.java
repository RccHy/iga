package com.qtgl.iga.service.impl;


import com.alibaba.druid.util.StringUtils;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.qtgl.iga.bean.PersonConnection;
import com.qtgl.iga.bean.PersonEdge;
import com.qtgl.iga.bo.*;
import com.qtgl.iga.dao.*;
import com.qtgl.iga.service.PersonService;
import com.qtgl.iga.utils.DataBusUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Transactional
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


    /**
     * todo 1:  循环遍历参与治理的所有上游源
     * 2： 拉取上游源数据并根据 证件类型+证件 号码进行和重，并验证证件类型对应的号码是否符合 正则表达式
     * 3： 如果有手动合重规则，运算手动合重规则【待确认】
     */
    @SneakyThrows
    @Override
    public Map<String, List<Person>> buildPerson(DomainInfo domain)  {
        Tenant tenant = tenantDao.findByDomainName(domain.getDomainName());
        if (null == tenant) {
            throw new Exception("租户不存在");
        }
        // 所有证件类型
        List<CardType> cardTypes = cardTypeDao.findAll(tenant.getId());
        Map<String, CardType> cardTypeMap = cardTypes.stream().collect(Collectors.toMap(CardType::getCardTypeCode, CardType -> CardType));


        // 获取规则
        Map arguments = new ConcurrentHashMap();
        arguments.put("type", "person");
        List<Node> nodes = nodeDao.findNodes(arguments, domain.getDomainId());
        String nodeId = nodes.get(0).getId();
        //
        List<NodeRules> userRules = rulesDao.getByNodeAndType(nodeId, 1, true, 0);
        // 根据证件类型+证件号进行合重
        Map<String, Person> personFromUpstream = new ConcurrentHashMap<>();
        userRules.forEach(rules -> {
            // 通过规则获取数据
            UpstreamType upstreamType = upstreamTypeDao.findById(rules.getUpstreamTypesId());
            ArrayList<Upstream> upstreams = upstreamDao.getUpstreams(upstreamType.getUpstreamId(), domain.getDomainId());
            JSONArray dataByBus = dataBusUtil.getDataByBus(upstreamType,domain.getDomainName());
            List<Person> personBeanList = dataByBus.toJavaList(Person.class);
            if (null != personBeanList) {
                for (Person personBean : personBeanList) {
                    if (StringUtils.isEmpty(personBean.getName())) {
                        log.error("姓名为空");
                        continue;
                    }
                    if (StringUtils.isEmpty(personBean.getCardNo()) && StringUtils.isEmpty(personBean.getCardType())) {
                        log.error(personBean.getName() + "证件类型、号码为空");
                        continue;
                        //throw new Exception(userBean.getName() + "证件类型、号码为空");
                    }
                    if (cardTypeMap.containsKey(personBean.getCardType())) {
                        String cardTypeReg = cardTypeMap.get(personBean.getCardType()).getCardTypeReg();
                        if (!Pattern.matches(cardTypeReg, personBean.getCardNo())) {
                            log.error(personBean.getName() + "证件号码不符合规则");
                            continue;
                            //throw new Exception(userBean.getName() + "证件号码不符合规则");
                        }
                    } else {
                        log.error(personBean.getName() + "证件类型无效");
                        continue;
                        //throw new Exception(userBean.getName() + "证件类型无效");
                    }
                    personBean.setDataSource(upstreams.get(0).getAppCode());
                    personFromUpstream.put(personBean.getCardType() + ":" + personBean.getCardNo(), personBean);
                }
            }
        });


        /*
          将合重后的数据插入进数据库
        */

        // 获取 sso数据
        List<Person> personFromSSO = personDao.getAll(tenant.getId());
        Map<String, Person> personFromSSOMap = personFromSSO.stream().collect(Collectors.toMap(person -> (person.getCardType() + ":" + person.getCardNo()), person -> person));
        // 存储最终需要操作的数据
        Map<String, List<Person>> result = new HashMap<>();
        personFromSSOMap.forEach((key, val) -> {
            // 对比出需要修改的person
            if (personFromUpstream.containsKey(key) &&
                    personFromUpstream.get(key).getCreateTime() > val.getUpdateTime()) {
                if (result.containsKey("update")) {
                    result.get("update").add(personFromUpstream.get(key));
                } else {
                    result.put("update", new ArrayList<Person>() {{
                        this.add(personFromUpstream.get(key));
                    }});
                }
                log.debug("对比后需要修改{}", val.toString());
            } else if (!personFromUpstream.containsKey(key) && 1 != val.getDelMark()) {
                if (result.containsKey("delete")) {
                    result.get("delete").add(val);
                } else {
                    result.put("delete", new ArrayList<Person>() {{
                        this.add(val);
                    }});
                }

                log.debug("对比后删除{}", val.toString());
            }
        });

        personFromUpstream.forEach((key, val) -> {
            if (!personFromSSOMap.containsKey(key)) {
                val.setCreateTime(System.currentTimeMillis());
                val.setOpenId(RandomStringUtils.randomAlphanumeric(20));
                if (result.containsKey("install")) {
                    result.get("install").add(val);
                } else {
                    result.put("install", new ArrayList<Person>() {{
                        this.add(val);
                    }});
                }
                log.debug("对比后新增{}", val.toString());
            }
        });

        // sso 批量新增
        if (result.containsKey("install")) {
            personDao.savePerson(result.get("install"), tenant.getId());
        }

        // sso 修改
        if (result.containsKey("update")) {
            personDao.updatePerson(result.get("update"), tenant.getId());
        }

        /* sso 批量删除
         *  仅在 人 表中打上删除标记
         *  不删除关联，删除人下面的所有身份及账号
         *  关联表  IdentityAccount 人账号关联表 、 IdentityUser 人身份关联关系
         */
        if (result.containsKey("delete")) {
            List<Person> personList = result.get("delete");
            personDao.deletePerson(personList, tenant.getId());
            List<String> ids = personList.stream().map(Person::getId).collect(Collectors.toList());
            // 查IdentityAccount关联表
            List<String> accounts = personDao.getAccountByIdentityId(ids);
            // 根据关联关系 删除 account表
            personDao.deleteAccount(accounts);
            // 查IdentityUser关联表
            List<String> occupies = personDao.getOccupyByIdentityId(ids);
            // 根据关联关系 删除 account表
            personDao.deleteOccupy(occupies);

        }

        return result;
    }

    @Override
    public PersonConnection findPersons(Map<String, Object> arguments, DomainInfo domain) {
        List<PersonEdge> upstreamDept = new ArrayList<>();
        String upstreamTypeId = (String) arguments.get("upstreamTypeId");
        Integer offset = (Integer) arguments.get("offset");
        Integer first = (Integer) arguments.get("first");
        UpstreamType upstreamType = upstreamTypeDao.findById(upstreamTypeId);

        Map dataMap = dataBusUtil.getDataByBus(upstreamType, offset, first);

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
    }
}
