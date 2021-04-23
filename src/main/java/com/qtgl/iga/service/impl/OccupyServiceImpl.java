package com.qtgl.iga.service.impl;

import com.alibaba.druid.util.StringUtils;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.qtgl.iga.bean.OccupyConnection;
import com.qtgl.iga.bean.OccupyDto;
import com.qtgl.iga.bean.OccupyEdge;
import com.qtgl.iga.bo.*;
import com.qtgl.iga.dao.*;
import com.qtgl.iga.service.OccupyService;
import com.qtgl.iga.utils.ClassCompareUtil;
import com.qtgl.iga.utils.DataBusUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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
    OccupyDao occupyDao;

    @Autowired
    DataBusUtil dataBusUtil;


    /**
     * 1：根据规则获取所有的 人员身份数据
     * 2：人员身份根据人员进行分组
     * 3：根据人员和数据库中身份进行对比
     * A：新增
     * B：修改
     * C：删除
     *
     * @param domain
     * @return
     */
    @Override
    public Map<String, List<OccupyDto>> buildPerson(DomainInfo domain) {

        Tenant tenant = tenantDao.findByDomainName(domain.getDomainName());
        if (null == tenant) {
            throw new RuntimeException("租户不存在");
        }


        // 获取规则
        Map arguments = new ConcurrentHashMap();
        arguments.put("type", "occupy");
        arguments.put("status", 0);
        List<Node> nodes = nodeDao.findNodes(arguments, domain.getId());
        if (null == nodes || nodes.size() <= 0) {
            throw new RuntimeException("无人员身份管理规则信息");
        }
        String nodeId = nodes.get(0).getId();

        List<NodeRules> occupyRules = rulesDao.getByNodeAndType(nodeId, 1, true, 0);

        // 获取sso中所有人员，用于验证 身份信息是否合法
        List<Person> personFromSSO = personDao.getAll(tenant.getId());
        Map<String, Person> personFromSSOMap = personFromSSO.stream().filter(person -> !StringUtils.isEmpty(person.getCardType()) && !StringUtils.isEmpty(person.getCardNo())).collect(Collectors.toMap(person -> (person.getCardType() + ":" + person.getCardNo()), person -> person, (v1, v2) -> v2));

        // 获取所有规则 字段，用于更新验证
        Map<String, OccupyDto> occupyDtoFromUpstream = new HashMap<>();
        occupyRules.forEach(rules -> {
            UpstreamType upstreamType = upstreamTypeDao.findById(rules.getUpstreamTypesId());
            ArrayList<Upstream> upstreams = upstreamDao.getUpstreams(upstreamType.getUpstreamId(), domain.getId());
            final LocalDateTime now = LocalDateTime.now();
            JSONArray dataByBus = dataBusUtil.getDataByBus(upstreamType, domain.getDomainName());
            final List<OccupyDto> occupies = dataByBus.toJavaList(OccupyDto.class);
            for (OccupyDto occupyDto : occupies) {
                if (StringUtils.isEmpty(occupyDto.getPersonCardType()) || StringUtils.isEmpty(occupyDto.getPersonCardNo())) {
                    log.warn("人员身份信息人员为空{}", occupyDto);
                    continue;
                }
                if (StringUtils.isEmpty(occupyDto.getPostCode())) {
                    log.warn("人员身份信息岗位代码为空{}", occupyDto);
                    continue;
                }
                if (StringUtils.isEmpty(occupyDto.getDeptCode())) {
                    log.warn("人员身份部门代码为空{}", occupyDto);
                    continue;
                }
                occupyDto.setSource(upstreams.get(0).getAppName() + "(" + upstreams.get(0).getAppCode() + ")");
                String personKey = occupyDto.getPersonCardType() + ":" + occupyDto.getPersonCardNo();

                //
                if (!personFromSSOMap.containsKey(personKey)) {
                    log.warn("人员身份无法找到对应对人员信息{}", personKey);
                    continue;
                }
                final String personId = personFromSSOMap.get(personKey).getId();
                occupyDto.setPersonId(personId);
                occupyDto.setCreateTime(now);
                occupyDto.setUpstreamType(upstreamType.getId());
                if (null == occupyDto.getUpdateTime()) {
                    occupyDto.setUpdateTime(now);
                }
                if (null == occupyDto.getDelMark()) {
                    occupyDto.setDelMark(0);
                }
                if(null==occupyDto.getActive()){
                    occupyDto.setActive("1");
                    occupyDto.setActiveTime(LocalDateTime.now());
                }
                String key = personId + ":" + occupyDto.getPostCode() + ":" + occupyDto.getDeptCode();
                if (occupyDtoFromUpstream.containsKey(key)) {
                    log.warn("上游源人员身份数据覆盖:{}->{}", occupyDtoFromUpstream.get(key).toString(), occupyDto);
                }
                occupyDtoFromUpstream.put(key, occupyDto);
            }

        });
        log.info("所有人员身份数据获取完成:{}", occupyDtoFromUpstream.size());
        //final List<OccupyDto> occupyDtos = (List<OccupyDto>) occupyDtoFromUpstream.values();
        // 获取sso中人员身份信息
        final List<OccupyDto> occupiesFromSSO = occupyDao.findAll(tenant.getId());
        log.info("数据库中人员身份数据获取完成:{}", occupiesFromSSO.size());
        Map<String, OccupyDto> occupiesFromSSOMap = occupiesFromSSO.stream().
                collect(Collectors.toMap(occupy -> (occupy.getPersonId() + ":" + occupy.getPostCode() + ":" + occupy.getDeptCode()), occupy -> occupy, (v1, v2) -> v2));
        Map<String, List<OccupyDto>> result = new HashMap<>();
        occupiesFromSSOMap.forEach((key, val) -> {
            // 对比出需要修改的occupy
            if (occupyDtoFromUpstream.containsKey(key) &&
                    occupyDtoFromUpstream.get(key).getCreateTime().isAfter(val.getUpdateTime())) {
                //
                boolean flag = false;
                boolean delFlag = true;
                OccupyDto newOccupy = occupyDtoFromUpstream.get(key);
                List<UpstreamTypeField> fields = DataBusUtil.typeFields.get(newOccupy.getUpstreamType());

                // 如果字段上游不提供，则不进行更新
                //    字段值没有发生改变，不进行更新
                if (null != fields && fields.size() > 0) {
                    for (UpstreamTypeField field : fields) {
                        String sourceField = field.getSourceField();
                        if("personCardType".equals(sourceField)||"personCardNo".equals(sourceField)){
                            continue;
                        }
                        Object newValue = ClassCompareUtil.getGetMethod(newOccupy, sourceField);
                        Object oldValue = ClassCompareUtil.getGetMethod(val, sourceField);
                        if (null == oldValue && null == newValue) {
                            continue;
                        }
                        if (null != oldValue && oldValue.equals(newValue)) {
                            continue;
                        }
                        flag = true;
                        if (sourceField.equals("delMark") && (Integer) oldValue == 1 && (Integer) newValue == 0) {
                            delFlag = false;
                            log.info("人员身份信息{}从删除恢复", val.getOccupyId());
                        }
                        if (sourceField.equals("delMark") && (Integer) oldValue == 0 && (Integer) newValue == 1) {
                            log.info("人员身份信息{}删除", val.getOccupyId());
                        }
                        ClassCompareUtil.setValue(val, val.getClass(), sourceField, oldValue, newValue);
                        log.info("人员身份信息更新{}:字段{}：{} -> {}", val.getOccupyId(), sourceField, oldValue, newValue);

                    }
                }
                if (val.getDelMark().equals(1) && delFlag) {
                    flag = true;
                    val.setDelMark(0);
                    log.info("人员身份信息{}从删除恢复", val.getOccupyId());
                }
                if (flag) {
                    val.setSource(newOccupy.getSource());
                    val.setUpdateTime(newOccupy.getUpdateTime());
                    if (result.containsKey("update")) {
                        result.get("update").add(val);
                    } else {
                        result.put("update", new ArrayList<OccupyDto>() {{
                            this.add(val);
                        }});
                    }
                }
                log.debug("人员身份对比后需要修改{}-{}", val, occupyDtoFromUpstream.get(key));
            } else if (!occupyDtoFromUpstream.containsKey(key) && 1 != val.getDelMark() && "PULL".equalsIgnoreCase(val.getDataSource())) {
                val.setUpdateTime(LocalDateTime.now());
                if (result.containsKey("delete")) {
                    result.get("delete").add(val);
                } else {
                    result.put("delete", new ArrayList<OccupyDto>() {{
                        this.add(val);
                    }});
                }
                log.debug("人员身份对比后删除{}", val);
            }

        });


        occupyDtoFromUpstream.forEach((key, val) -> {
            if (!occupiesFromSSOMap.containsKey(key)) {
                val.setOccupyId(UUID.randomUUID().toString());
                if (result.containsKey("install")) {
                    result.get("install").add(val);
                } else {
                    result.put("install", new ArrayList<OccupyDto>() {{
                        this.add(val);
                    }});
                }
                log.debug("人员身份对比后新增{}", val.toString());
            }
        });

        occupyDao.saveToSso(result, tenant.getId());

        return result;
    }


    @Override
    public OccupyConnection findOccupies(Map<String, Object> arguments, DomainInfo domain) throws Exception {
        List<OccupyEdge> upstreamDept = new ArrayList<>();
        String upstreamTypeId = (String) arguments.get("upstreamTypeId");
        Integer offset = (Integer) arguments.get("offset");
        Integer first = (Integer) arguments.get("first");
        UpstreamType upstreamType = upstreamTypeDao.findById(upstreamTypeId);
        if (null != upstreamType && upstreamType.getIsPage()) {
            Map dataMap = dataBusUtil.getDataByBus(upstreamType, offset, first);

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
            throw new Exception("数据类型不合法,请检查");
        }
    }
}
