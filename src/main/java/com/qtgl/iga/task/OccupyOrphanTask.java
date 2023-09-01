package com.qtgl.iga.task;

import com.qtgl.iga.bean.OccupyDto;
import com.qtgl.iga.bean.TreeBean;
import com.qtgl.iga.bo.Tenant;
import com.qtgl.iga.dao.DeptDao;
import com.qtgl.iga.dao.PostDao;
import com.qtgl.iga.service.OccupyService;
import com.qtgl.iga.service.UserLogService;
import com.qtgl.iga.service.impl.OccupyServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class OccupyOrphanTask {

    @Resource
    OccupyService occupyService;
    @Resource
    DeptDao deptDao;
    @Resource
    PostDao postDao;
    @Resource
    UserLogService userLogService;

    public void occupyOrphanTask(Tenant tenant) {
        log.info("租户:{}---------本次孤儿身份监控任务开始----------------", tenant.getDomain());
        List<OccupyDto> occupyDtos = occupyService.findAll(tenant.getId(), null, null);
        //获取所有的组织机构
        List<TreeBean> depts = deptDao.findByTenantId(tenant.getId(), null, null);
        //获取所有的岗位
        List<TreeBean> posts = postDao.findByTenantId(tenant.getId());

        List<OccupyDto> resultOccupies = new ArrayList<>();

        resultOccupies = this.handleByDept(occupyDtos, depts, resultOccupies);

        resultOccupies = this.handleByUserType(occupyDtos, posts, resultOccupies);

        if (!CollectionUtils.isEmpty(resultOccupies)) {
            Map<String, List<OccupyDto>> octResult = new HashMap<>();
            octResult.put("update", occupyDtos);
            occupyService.saveToSso(octResult, tenant.getId(), null, null);
            userLogService.saveUserLog(occupyDtos, tenant.getId());
            log.info("因 组织机构 岗位 变动 导致:{}条,身份有效期发生变化", occupyDtos.size());
        }


        log.info("租户:{}--------本次孤儿身份监控任务结束----------------", tenant.getDomain());

    }

    public List<OccupyDto> handleByUserType(List<OccupyDto> occupyDtos, List<TreeBean> posts, List<OccupyDto> resultOccupies) {
        Map<String, List<OccupyDto>> collect = occupyDtos.stream().collect(Collectors.groupingBy(OccupyDto::getPostCode));

        LocalDateTime now = LocalDateTime.now();
        for (TreeBean treeBean : posts) {
            if (collect.containsKey(treeBean.getCode())) {
                List<OccupyDto> dtoList = collect.get(treeBean.getCode());
                if (!CollectionUtils.isEmpty(dtoList)) {
                    for (OccupyDto dto : dtoList) {
                        dto.setUpdateTime(now);
                        if (treeBean.getActive() == 1) {
                            //新增或恢复的岗位  将orphan为2(因岗位无效导致的无效身份) 的有效期重新计算
                            if (2 == dto.getOrphan()) {

                                dto.setValidStartTime(now);
                                dto.setValidEndTime(null != dto.getEndTime() ? dto.getEndTime() : OccupyServiceImpl.DEFAULT_END_TIME);
                                dto.setOrphan(0);
                                dto.setActive(1);
                                OccupyServiceImpl.checkValidTime(dto, now, true);
                                resultOccupies.add(dto);
                            } else if (3 == dto.getOrphan()) {
                                //orphan为3(因组织机构和岗位无效导致的无效身份)的改为 orphan 1
                                dto.setOrphan(1);
                                dto.setActive(0);
                                resultOccupies.add(dto);
                            }

                        } else {
                            //岗位置为无效  将orphan为0的置为2 有效期重新计算
                            if (0 == dto.getOrphan() || null == dto.getOrphan()) {
                                dto.setValidEndTime(now);
                                dto.setOrphan(2);
                                dto.setActive(0);
                                resultOccupies.add(dto);
                            } else if (1 == dto.getOrphan()) {
                                dto.setOrphan(3);
                                dto.setActive(0);
                                resultOccupies.add(dto);
                            }

                        }

                    }
                }
            }
        }
        return resultOccupies;
    }

    public List<OccupyDto> handleByDept(List<OccupyDto> occupyDtos, List<TreeBean> depts, List<OccupyDto> resultOccupies) {
        Map<String, List<OccupyDto>> collect = occupyDtos.stream().collect(Collectors.groupingBy(OccupyDto::getDeptCode));

        LocalDateTime now = LocalDateTime.now();
        for (TreeBean treeBean : depts) {
            if (collect.containsKey(treeBean.getCode())) {
                List<OccupyDto> dtoList = collect.get(treeBean.getCode());
                if (!CollectionUtils.isEmpty(dtoList)) {
                    for (OccupyDto dto : dtoList) {
                        dto.setUpdateTime(now);

                        if (treeBean.getActive() == 1) {
                            //新增或恢复的组织机构  将orphan为1(因部门无效导致的无效身份) 的有效期重新计算
                            if (1 == dto.getOrphan()) {
                                dto.setValidStartTime(now);
                                dto.setValidEndTime(null != dto.getEndTime() ? dto.getEndTime() : OccupyServiceImpl.DEFAULT_END_TIME);
                                dto.setOrphan(0);
                                dto.setActive(1);
                                OccupyServiceImpl.checkValidTime(dto, now, true);
                                resultOccupies.add(dto);
                            } else if (3 == dto.getOrphan()) {
                                //orphan为3(因组织机构和岗位无效导致的无效身份)的改为 orphan 2
                                dto.setOrphan(2);
                                dto.setActive(0);
                                resultOccupies.add(dto);
                            }

                        } else {
                            //组织机构置为无效  将orphan为0的置为1 有效期重新计算
                            if (0 == dto.getOrphan() || null == dto.getOrphan()) {
                                dto.setValidEndTime(now);
                                dto.setOrphan(1);
                                dto.setActive(0);
                                resultOccupies.add(dto);
                            } else if (2 == dto.getOrphan()) {
                                dto.setOrphan(3);
                                dto.setActive(0);
                                resultOccupies.add(dto);
                            }

                        }

                    }
                }
            }
        }
        return resultOccupies;
    }
}
