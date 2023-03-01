package com.qtgl.iga.service.impl;

import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.bo.NodeRules;
import com.qtgl.iga.bo.Person;
import com.qtgl.iga.bo.TaskLog;
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
public class SubTaskServiceImpl {
    @Resource
    DeptServiceImpl deptService;
    @Resource
    PersonServiceImpl personService;
    @Resource
    TaskLogService taskLogService;

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
                        //租户,最后一次日志情况, 当前日志  记录为pub, 需要添加入参   当前规则
                        Map<String, List<Person>> personResult = personService.buildPerson(domain, lastTaskLog, taskLog, nodeRules);
                        Integer insertPerson = (personResult.containsKey("insert") ? personResult.get("insert").size() : 0);
                        Integer deletePerson = personResult.containsKey("delete") ? personResult.get("delete").size() : 0;
                        Integer updatePerson = (personResult.containsKey("update") ? personResult.get("update").size() : 0);
                        Integer invalidPerson = personResult.containsKey("invalid") ? personResult.get("invalid").size() : 0;
                        String personNo = insertPerson + "/" + deletePerson + "/" + updatePerson + "/" + invalidPerson;
                        taskLog.setPersonNo(personNo);
                        taskLogService.save(taskLog, domain.getId(), "update");
                        log.info(Thread.currentThread().getName() + ": 人员同步完成{}==={}", personNo, System.currentTimeMillis());

                    } catch (Exception e) {
                        e.printStackTrace();

                    }
                    break;
                case "OCCUPY":
                    System.out.println(4);
                    break;
            }
        }

    }
}
