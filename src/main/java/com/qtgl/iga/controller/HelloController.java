package com.qtgl.iga.controller;

import com.qtgl.iga.bean.OccupyDto;
import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.bo.Person;
import com.qtgl.iga.bo.TaskLog;
import com.qtgl.iga.config.TaskThreadPool;
import com.qtgl.iga.service.*;
import com.qtgl.iga.utils.DataBusUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author 1
 */
//@Controller
//@RequestMapping("/test")
@Slf4j
public class HelloController {

    @Autowired
    DeptService deptService;
    @Autowired
    PostService postService;
    @Autowired
    PersonService personService;
    @Autowired
    OccupyService occupyService;


    @Autowired
    DataBusUtil dataBusUtil;

    @Autowired
    DomainInfoService domainInfoService;
    @Autowired
    TaskLogService taskLogService;

    @RequestMapping("/xc")
    @ResponseBody
    public String getXc() {
        ExecutorService executorService = TaskThreadPool.executorServiceMap.get("cloud.ketanyun.cn");
        ThreadPoolExecutor tpe = ((ThreadPoolExecutor) executorService);
        int queueSize = tpe.getQueue().size();
        System.out.println(Thread.currentThread().getName() + "当前排队线程数：" + queueSize);

        int activeCount = tpe.getActiveCount();
        System.out.println(Thread.currentThread().getName() + "当前活动线程数：" + activeCount);

        return Thread.currentThread().getName() + "当前排队线程数：" + queueSize + "；当前活动线程数：" + activeCount;

    }

    @RequestMapping("/saveToSSO")
    @ResponseBody
    public void testDb() {
        {
            Map<String, Object> arguments = new HashMap<>();
            arguments.put("status", 1);
            TaskLog taskLog = new TaskLog();
            taskLog.setId(UUID.randomUUID().toString());
            final DomainInfo domainInfo = domainInfoService.findAll().get(1);
            try {
                taskLogService.save(taskLog, domainInfo.getId(), "save");
                TaskLog lastTaskLog = taskLogService.last(domainInfo.getId());
              /*  //部门数据同步至sso

                Map<TreeBean, String> deptResult = deptService.buildDeptUpdateResult(domainInfo, lastTaskLog);
                Map<String, List<Map.Entry<TreeBean, String>>> deptResultMap = deptResult.entrySet().stream().collect(Collectors.groupingBy(Map.Entry::getValue));
                String deptNo = (deptResultMap.containsKey("insert") ? String.valueOf(deptResultMap.get("insert").size()) : "0") + "/"
                        + (deptResultMap.containsKey("delete") ? String.valueOf(deptResultMap.get("delete").size()) : "0") + "/"
                        + (deptResultMap.containsKey("update") ? String.valueOf(deptResultMap.get("update").size()) : "0");

                log.info(Thread.currentThread().getName() + ": 部门同步完成：{}==={}", deptNo, System.currentTimeMillis());
                taskLog.setStatus("doing");
                taskLog.setDeptNo(deptNo);
                taskLogService.save(taskLog, domainInfo.getId(), "update");


                //=============岗位数据同步至sso=================
                final Map<TreeBean, String> postResult = postService.buildPostUpdateResult(domainInfo, lastTaskLog);
                Map<String, List<Map.Entry<TreeBean, String>>> postResultMap = postResult.entrySet().stream().collect(Collectors.groupingBy(Map.Entry::getValue));
                String postNo = (postResultMap.containsKey("insert") ? String.valueOf(postResultMap.get("insert").size()) : "0") + "/"
                        + (postResultMap.containsKey("delete") ? String.valueOf(postResultMap.get("delete").size()) : "0") + "/"
                        + (postResultMap.containsKey("update") ? String.valueOf(postResultMap.get("update").size()) : "0");
                log.info(Thread.currentThread().getName() + ": 岗位同步完成：{}==={}", postNo, System.currentTimeMillis());
                taskLog.setPersonNo(postNo);
                taskLogService.save(taskLog, domainInfo.getId(), "update");

               */



                //=============人员数据同步至sso=============
                Map<String, List<Person>> personResult = personService.buildPerson(domainInfo, lastTaskLog,null);
                String personNo = (personResult.containsKey("insert") ? String.valueOf(personResult.get("insert").size()) : "0") + "/"
                        + (personResult.containsKey("delete") ? String.valueOf(personResult.get("delete").size()) : "0") + "/"
                        + (personResult.containsKey("update") ? String.valueOf(personResult.get("update").size()) : "0");
                log.info(Thread.currentThread().getName() + ": 人员同步完成{}==={}", personNo, System.currentTimeMillis());
                taskLog.setPersonNo(personNo);
                taskLogService.save(taskLog, domainInfo.getId(), "update");


                //人员身份同步至sso
                final Map<String, List<OccupyDto>> occupyResult = occupyService.buildOccupy(domainInfo, lastTaskLog,null);
                String occupyNo = (occupyResult.containsKey("insert") ? String.valueOf(occupyResult.get("insert").size()) : "0") + "/"
                        + (occupyResult.containsKey("delete") ? String.valueOf(occupyResult.get("delete").size()) : "0") + "/"
                        + (occupyResult.containsKey("update") ? String.valueOf(occupyResult.get("update").size()) : "0");
                log.info(Thread.currentThread().getName() + ": 人员身份同步完成{}==={}", occupyNo, System.currentTimeMillis());
                taskLog.setStatus("done");
                taskLog.setOccupyNo(occupyNo);
                taskLogService.save(taskLog, domainInfo.getId(), "update");



                log.info("{}同步结束,task:{}", domainInfo.getDomainName(), taskLog.getId());
            } catch (Exception e) {
                log.error("定时同步异常：" + e);
                taskLog.setStatus("failed");
                taskLog.setReason(e.getMessage());
                taskLogService.save(taskLog, domainInfo.getId(), "update");
                e.printStackTrace();
            }

        }

    }

}