package com.qtgl.iga.service.impl;

import com.alibaba.druid.util.StringUtils;
import com.alibaba.fastjson.JSONArray;
import com.qtgl.iga.bean.OccupyDto;
import com.qtgl.iga.bo.*;
import com.qtgl.iga.dao.*;
import com.qtgl.iga.service.OccupyService;
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

        List<Person> personFromSSO = personDao.getAll(tenant.getId());
        Map<String, Person> personFromSSOMap = personFromSSO.stream().filter(person -> !StringUtils.isEmpty(person.getCardType()) && !StringUtils.isEmpty(person.getCardNo())).collect(Collectors.toMap(person -> (person.getCardType() + ":" + person.getCardNo()), person -> person, (v1, v2) -> v2));

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
                occupyDto.setUserId(personId);
                occupyDto.setCreateTime(now);
                if (null == occupyDto.getUpdateTime()) {
                    occupyDto.setUpdateTime(now);
                }
                String key = personId + ":" + occupyDto.getPostCode() + ":" + occupyDto.getDeptCode();
                if (occupyDtoFromUpstream.containsKey(key)) {
                    log.warn("上游源人员身份数据覆盖:{}->{}", occupyDtoFromUpstream.get(key).toString(), occupyDto);
                }
                occupyDtoFromUpstream.put(key, occupyDto);
            }

        });
        log.info("所有人员身份数据获取完成:{}", occupyDtoFromUpstream.size());
        final List<OccupyDto> occupyDtos = (List<OccupyDto>) occupyDtoFromUpstream.values();
        // 获取sso中人员身份信息
        final List<Occupy> occupiesFromSSO = occupyDao.findAll(tenant.getId());
        log.info("数据库中人员身份数据获取完成:{}", occupiesFromSSO.size());
        Map<String, Occupy> occupiesFromSSOMap = occupiesFromSSO.stream().
                collect(Collectors.toMap(occupy -> (occupy.getPersonId() + ":" + occupy.getPostCode() + ":" + occupy.getDeptCode()), occupy -> occupy, (v1, v2) -> v2));
        Map<String, List<OccupyDto>> result = new HashMap<>();
        occupiesFromSSOMap.forEach((key, val) -> {
            // 对比出需要修改的occupy
            if (occupyDtoFromUpstream.containsKey(key) &&
                    occupyDtoFromUpstream.get(key).getCreateTime().isAfter(val.getUpdateTime())) {
                if (result.containsKey("update")) {
                    result.get("update").add(occupyDtoFromUpstream.get(key));
                } else {
                    result.put("update", new ArrayList<OccupyDto>() {{
                        this.add(occupyDtoFromUpstream.get(key));
                    }});
                }
                log.debug("人员身份对比后需要修改{}-{}", val, occupyDtoFromUpstream.get(key));
            } else if (!occupyDtoFromUpstream.containsKey(key) && 1 != val.getDelMark() && "pull".equals(val.getDataSource())) {
                OccupyDto occupyDto = new OccupyDto();
                occupyDto.setOccupyId(val.getOccupyId());
                if (result.containsKey("delete")) {
                    result.get("delete").add(occupyDto);
                } else {
                    result.put("delete", new ArrayList<OccupyDto>() {{
                        this.add(occupyDto);
                    }});
                }
                log.debug("人员身份对比后删除{}", val);
            }

        });


        occupyDtoFromUpstream.forEach((key, val) -> {
            if (!occupiesFromSSOMap.containsKey(key)) {
                if (result.containsKey("install")) {
                    val.setOccupyId(UUID.randomUUID().toString());
                    result.get("install").add(val);
                } else {
                    result.put("install", new ArrayList<OccupyDto>() {{
                        this.add(val);
                    }});
                }
                log.debug("人员身份对比后新增{}", val.toString());
            }
        });


        return null;
    }
}
