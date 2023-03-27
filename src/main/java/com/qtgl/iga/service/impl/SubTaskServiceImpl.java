package com.qtgl.iga.service.impl;

import com.qtgl.iga.bean.OccupyDto;
import com.qtgl.iga.bean.TreeBean;
import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.bo.NodeRules;
import com.qtgl.iga.bo.Person;
import com.qtgl.iga.bo.TaskLog;
import com.qtgl.iga.service.DeptService;
import com.qtgl.iga.service.PostService;
import com.qtgl.iga.service.SubTaskService;
import com.qtgl.iga.service.TaskLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SubTaskServiceImpl implements SubTaskService {
    @Resource
    OccupyServiceImpl occupyService;
    @Resource
    PersonServiceImpl personService;
    @Resource
    TaskLogService taskLogService;
    @Resource
    PostService postService;
    @Resource
    DeptService deptService;

    @Override
    public void subTask(String type, DomainInfo domain, List<NodeRules> nodeRules) {

        if (!CollectionUtils.isEmpty(nodeRules)) {
            TaskLog lastTaskLog = taskLogService.last(domain.getId());
            TaskLog taskLog = new TaskLog();
            taskLog.setId(UUID.randomUUID().toString());
            if (null != domain) {
                taskLog.setSynWay(1);
            }
            log.info(" sub{}开始同步,task:{}", domain.getDomainName(), taskLog.getId());
            taskLogService.save(taskLog, domain.getId(), "save");
            switch (type) {
                case "DEPT":
                    try {
                        //租户,最后一次日志情况, 当前日志  记录为sub, 需要添加入参   当前规则
                        Map<TreeBean, String> deptResult = deptService.buildDeptUpdateResult(domain, lastTaskLog, taskLog, null);
                        Map<String, List<Map.Entry<TreeBean, String>>> deptResultMap = deptResult.entrySet().stream().collect(Collectors.groupingBy(Map.Entry::getValue));
                        //处理数据
                        Integer recoverDept = deptResultMap.containsKey("recover") ? deptResultMap.get("recover").size() : 0;
                        Integer insertDept = (deptResultMap.containsKey("insert") ? deptResultMap.get("insert").size() : 0) + recoverDept;
                        Integer deleteDept = deptResultMap.containsKey("delete") ? deptResultMap.get("delete").size() : 0;
                        Integer updateDept = (deptResultMap.containsKey("update") ? deptResultMap.get("update").size() : 0);
                        Integer invalidDept = deptResultMap.containsKey("invalid") ? deptResultMap.get("invalid").size() : 0;
                        String deptNo = insertDept + "/" + deleteDept + "/" + updateDept + "/" + invalidDept;
                        taskLog.setStatus("done");
                        taskLog.setDeptNo(deptNo);
                        taskLogService.save(taskLog, domain.getId(), "update");
                        log.info(Thread.currentThread().getName() + ": sub 部门同步完成{}==={}", deptNo, System.currentTimeMillis());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                case "POST":
                    try {
                        //租户,最后一次日志情况, 当前日志  记录为sub, 需要添加入参   当前规则
                        final Map<TreeBean, String> postResult = postService.buildPostUpdateResult(domain, lastTaskLog, taskLog, null);
                        Map<String, List<Map.Entry<TreeBean, String>>> postResultMap = postResult.entrySet().stream().collect(Collectors.groupingBy(Map.Entry::getValue));
                        Integer recoverPost = postResultMap.containsKey("recover") ? postResultMap.get("recover").size() : 0;
                        Integer insertPost = (postResultMap.containsKey("insert") ? postResultMap.get("insert").size() : 0) + recoverPost;
                        Integer deletePost = postResultMap.containsKey("delete") ? postResultMap.get("delete").size() : 0;
                        Integer updatePost = (postResultMap.containsKey("update") ? postResultMap.get("update").size() : 0);
                        Integer invalidPost = postResultMap.containsKey("invalid") ? postResultMap.get("invalid").size() : 0;
                        String postNo = insertPost + "/" + deletePost + "/" + updatePost + "/" + invalidPost;
                        taskLog.setStatus("done");
                        taskLog.setPostNo(postNo);
                        taskLogService.save(taskLog, domain.getId(), "update");
                        log.info(Thread.currentThread().getName() + ": sub 岗位同步完成{}==={}", postNo, System.currentTimeMillis());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                case "PERSON":
                    try {
                        //租户,最后一次日志情况, 当前日志  记录为sub, 需要添加入参   当前规则
                        Map<String, List<Person>> personResult = personService.buildPerson(domain, lastTaskLog, taskLog, nodeRules);
                        Integer insertPerson = (personResult.containsKey("insert") ? personResult.get("insert").size() : 0);
                        Integer deletePerson = personResult.containsKey("delete") ? personResult.get("delete").size() : 0;
                        Integer updatePerson = (personResult.containsKey("update") ? personResult.get("update").size() : 0);
                        Integer invalidPerson = personResult.containsKey("invalid") ? personResult.get("invalid").size() : 0;
                        String personNo = insertPerson + "/" + deletePerson + "/" + updatePerson + "/" + invalidPerson;
                        taskLog.setStatus("done");
                        taskLog.setPersonNo(personNo);
                        taskLogService.save(taskLog, domain.getId(), "update");
                        log.info(Thread.currentThread().getName() + ": sub 人员同步完成{}==={}", personNo, System.currentTimeMillis());

                    } catch (Exception e) {
                        e.printStackTrace();

                    }
                    break;
                case "OCCUPY":
                    try {
                        final Map<String, List<OccupyDto>> occupyResult = occupyService.buildOccupy(domain, lastTaskLog, taskLog, nodeRules);
                        //租户,最后一次日志情况, 当前日志  记录为sub, 需要添加入参   当前规则
                        Integer insertOccupy = (occupyResult.containsKey("insert") ? occupyResult.get("insert").size() : 0);
                        Integer deleteOccupy = occupyResult.containsKey("delete") ? occupyResult.get("delete").size() : 0;
                        Integer updateOccupy = (occupyResult.containsKey("update") ? occupyResult.get("update").size() : 0);
                        Integer invalidOccupy = occupyResult.containsKey("invalid") ? occupyResult.get("invalid").size() : 0;
                        String occupyNo = insertOccupy + "/" + deleteOccupy + "/" + updateOccupy + "/" + invalidOccupy;
                        taskLog.setStatus("done");
                        taskLog.setOccupyNo(occupyNo);
                        taskLogService.save(taskLog, domain.getId(), "update");
                        log.info(Thread.currentThread().getName() + ": sub 人员身份同步完成{}==={}", occupyNo, System.currentTimeMillis());

                    } catch (Exception e) {
                        e.printStackTrace();

                    }
                    break;
            }
        }

    }
}
