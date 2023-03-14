package com.qtgl.iga.service.impl;

import com.qtgl.iga.bean.OccupyDto;
import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.bo.NodeRules;
import com.qtgl.iga.bo.Person;
import com.qtgl.iga.bo.TaskLog;
import com.qtgl.iga.service.SubTaskService;
import com.qtgl.iga.service.TaskLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class SubTaskServiceImpl implements SubTaskService {
    @Resource
    OccupyServiceImpl occupyService;
    @Resource
    PersonServiceImpl personService;
    @Resource
    TaskLogService taskLogService;
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

                    System.out.println(1);
                    break;
                case "POST":
                    System.out.println(2);
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
