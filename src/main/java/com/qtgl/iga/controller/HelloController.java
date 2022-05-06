package com.qtgl.iga.controller;

import com.qtgl.iga.bean.OccupyDto;
import com.qtgl.iga.bean.TreeBean;
import com.qtgl.iga.bo.*;
import com.qtgl.iga.config.TaskThreadPool;
import com.qtgl.iga.dao.TenantDao;
import com.qtgl.iga.service.*;
import com.qtgl.iga.task.TaskConfig;
import com.qtgl.iga.utils.CertifiedConnector;
import com.qtgl.iga.utils.DataBusUtil;
import com.qtgl.iga.utils.exception.CustomException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

/**
 * @author 1
 */
@Controller
@RequestMapping("/test")
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
    @Autowired
    TenantDao tenantDao;
    @Autowired
    DsConfigService dsConfigService;
    @Autowired
    NodeRulesService nodeRulesService;
    @Autowired
    TaskConfig taskConfig;

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



                //=============人员数据同步至sso=============
                Map<String, List<Person>> personResult = personService.buildPerson(domainInfo, lastTaskLog);
                String personNo = (personResult.containsKey("insert") ? String.valueOf(personResult.get("insert").size()) : "0") + "/"
                        + (personResult.containsKey("delete") ? String.valueOf(personResult.get("delete").size()) : "0") + "/"
                        + (personResult.containsKey("update") ? String.valueOf(personResult.get("update").size()) : "0");
                log.info(Thread.currentThread().getName() + ": 人员同步完成{}==={}", personNo, System.currentTimeMillis());
                taskLog.setPersonNo(personNo);
                taskLogService.save(taskLog, domainInfo.getId(), "update");

*/
                //人员身份同步至sso
                final Map<String, List<OccupyDto>> occupyResult = occupyService.buildOccupy(domainInfo, lastTaskLog);
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

    @RequestMapping("/invokeTask")
    @ResponseBody
    public void  invokeTask(){
        try {
            DomainInfo domainInfo = CertifiedConnector.getDomain();
            List<DsConfig> dsConfigs = dsConfigService.findAll();
            Map<String, DsConfig> collect = dsConfigs.stream().collect(Collectors.toMap(DsConfig::getTenantId, v -> v));
            // 如果 获取最近一次同步任务状况
            TaskLog lastTaskLog = taskLogService.last(domainInfo.getId());
            //  最近一次同步任务 状态成功后才能继续同步
            if ((null == lastTaskLog) || (null != lastTaskLog.getId() && !lastTaskLog.getStatus().equals("failed"))) {
                TaskConfig.errorData.remove(domainInfo.getId());
                Tenant tenant = tenantDao.findByDomainName(domainInfo.getDomainName());
                //判断sso是否有定时任务
                if (collect.containsKey(tenant.getId())) {
                    DsConfig dsConfig = collect.get(tenant.getId());
                    String syncWay = dsConfigService.getSyncWay(dsConfig);
                    if (StringUtils.isNotBlank(syncWay) && (syncWay.equals(TaskConfig.SYNC_WAY_BRIDGE) || syncWay.equals(TaskConfig.SYNC_WAY_ENTERPRISE) || syncWay.equals(TaskConfig.SYNC_WAY_ENTERPRISE_GRAPHQL))) {
                        log.info("{}sso正在进行同步,跳过本次同步任务", domainInfo.getId());
                        TaskLog taskLog = new TaskLog();
                        taskLog.setId(UUID.randomUUID().toString());
                        taskLog.setReason("sso开启了同步配置，请先关闭！");
                        taskLogService.save(taskLog, domainInfo.getId(), "skip-error");
                        return;
                    }
                }
                // 如果有编辑中的规则，则不进行数据同步
                Map<String, Object> arguments = new HashMap<>();
                arguments.put("status", 1);
                final List<NodeRules> nodeRules = nodeRulesService.findNodeRules(arguments, domainInfo.getId());
                TaskLog taskLog = new TaskLog();
                taskLog.setId(UUID.randomUUID().toString());
                // 如果有编辑中的规则，则不进行数据同步 &&
                if ((null == nodeRules || nodeRules.size() == 0)) {
                    try {
                        log.info("{}开始同步,task:{}", domainInfo.getDomainName(), taskLog.getId());
                        taskLogService.save(taskLog, domainInfo.getId(), "save");
                        //部门数据同步至sso
                        Map<TreeBean, String> deptResult = deptService.buildDeptUpdateResult(domainInfo, lastTaskLog);
                        Map<String, List<Map.Entry<TreeBean, String>>> deptResultMap = deptResult.entrySet().stream().collect(Collectors.groupingBy(Map.Entry::getValue));
                        //处理数据
                        Integer recoverDept = deptResultMap.containsKey("recover") ? deptResultMap.get("recover").size() : 0;
                        Integer insertDept = (deptResultMap.containsKey("insert") ? deptResultMap.get("insert").size() : 0) + recoverDept;
                        Integer deleteDept = deptResultMap.containsKey("delete") ? deptResultMap.get("delete").size() : 0;
                        Integer updateDept = (deptResultMap.containsKey("update") ? deptResultMap.get("update").size() : 0);
                        Integer invalidDept = deptResultMap.containsKey("invalid") ? deptResultMap.get("invalid").size() : 0;
                        String deptNo = insertDept + "/" + deleteDept + "/" + updateDept + "/" + invalidDept;

                        log.info(Thread.currentThread().getName() + ": 部门同步完成：{}==={}", deptNo, System.currentTimeMillis());
                        taskLog.setStatus("doing");
                        taskLog.setDeptNo(deptNo);
                        taskLogService.save(taskLog, domainInfo.getId(), "update");
                        // PUT   MQ
                        String pubResult = "";
                        if (deptResult.size() > 0) {
                            pubResult = dataBusUtil.pub(deptResult, null, null, "dept", domainInfo);
                            log.info("dept pub:{}", pubResult);
                        }


                        //=============岗位数据同步至sso=================
                        final Map<TreeBean, String> postResult = postService.buildPostUpdateResult(domainInfo, lastTaskLog);
                        Map<String, List<Map.Entry<TreeBean, String>>> postResultMap = postResult.entrySet().stream().collect(Collectors.groupingBy(Map.Entry::getValue));
                        Integer recoverPost = postResultMap.containsKey("recover") ? postResultMap.get("recover").size() : 0;
                        Integer insertPost = (postResultMap.containsKey("insert") ? postResultMap.get("insert").size() : 0) + recoverPost;
                        Integer deletePost = postResultMap.containsKey("delete") ? postResultMap.get("delete").size() : 0;
                        Integer updatePost = (postResultMap.containsKey("update") ? postResultMap.get("update").size() : 0);
                        Integer invalidPost = postResultMap.containsKey("invalid") ? postResultMap.get("invalid").size() : 0;
                        String postNo = insertPost + "/" + deletePost + "/" + updatePost + "/" + invalidPost;
                        log.info(Thread.currentThread().getName() + ": 岗位同步完成：{}==={}", postNo, System.currentTimeMillis());
                        taskLog.setPostNo(postNo);
                        taskLogService.save(taskLog, domainInfo.getId(), "update");

                        // PUT   MQ
                        if (postResult.size() > 0) {
                            pubResult = dataBusUtil.pub(postResult, null, null, "post", domainInfo);
                            log.info("post pub:{}", pubResult);
                        }


                        //=============人员数据同步至sso=============
                        Map<String, List<Person>> personResult = personService.buildPerson(domainInfo, lastTaskLog);
                        Integer insertPerson = (personResult.containsKey("insert") ? personResult.get("insert").size() : 0);
                        Integer deletePerson = personResult.containsKey("delete") ? personResult.get("delete").size() : 0;
                        Integer updatePerson = (personResult.containsKey("update") ? personResult.get("update").size() : 0);
                        Integer invalidPerson = personResult.containsKey("invalid") ? personResult.get("invalid").size() : 0;
                        String personNo = insertPerson + "/" + deletePerson + "/" + updatePerson + "/" + invalidPerson;
                        log.info(Thread.currentThread().getName() + ": 人员同步完成{}==={}", personNo, System.currentTimeMillis());
                        taskLog.setPersonNo(personNo);
                        //    taskLog.setData(errorData.get(domainInfo.getId()));
                        taskLogService.save(taskLog, domainInfo.getId(), "update");

                        // PUT   MQ
                        if (personResult.size() > 0 && (insertPerson + deletePerson + updatePerson + invalidPerson) < 100) {
                            pubResult = dataBusUtil.pub(null, personResult, null, "person", domainInfo);
                            log.info("person pub:{}", pubResult);
                        }


                        //人员身份同步至sso
                        final Map<String, List<OccupyDto>> occupyResult = occupyService.buildOccupy(domainInfo, lastTaskLog);
                        //Integer recoverOccupy = occupyResult.containsKey("recover") ? deptResultMap.get("recover").size() : 0;
                        Integer insertOccupy = (occupyResult.containsKey("insert") ? occupyResult.get("insert").size() : 0);
                        Integer deleteOccupy = occupyResult.containsKey("delete") ? occupyResult.get("delete").size() : 0;
                        Integer updateOccupy = (occupyResult.containsKey("update") ? occupyResult.get("update").size() : 0);
                        Integer invalidOccupy = occupyResult.containsKey("invalid") ? occupyResult.get("invalid").size() : 0;
                        String occupyNo = insertOccupy + "/" + deleteOccupy + "/" + updateOccupy + "/" + invalidOccupy;
                        log.info(Thread.currentThread().getName() + ": 人员身份同步完成{}==={}", occupyNo, System.currentTimeMillis());
                        taskLog.setStatus("done");
                        taskLog.setOccupyNo(occupyNo);
                        taskLogService.save(taskLog, domainInfo.getId(), "update");

                        // PUT   MQ
                        if (occupyResult.size() > 0 && (insertOccupy + deleteOccupy + updateOccupy + invalidOccupy) < 500) {
                            pubResult = dataBusUtil.pub(null, null, occupyResult, "occupy", domainInfo);
                            log.info("occupy pub:{}", pubResult);
                        }
                        //数据上传
                        if (StringUtils.isNotBlank(TaskConfig.errorData.get(domainInfo.getId()))) {
                            taskConfig.upload(domainInfo, taskLog);
                        } else {
                            log.info("{}本次同步无异常数据", domainInfo.getDomainName());
                        }
                        log.info("{}同步结束,task:{}", domainInfo.getDomainName(), taskLog.getId());
                    } catch (CustomException e) {
                        log.error("定时同步异常：" + e);
                        taskLog.setStatus("failed");
                        taskLog.setReason(e.getErrorMsg());
                        if (StringUtils.isNotBlank(TaskConfig.errorData.get(domainInfo.getId()))) {
                            taskConfig.upload(domainInfo, taskLog);
                        }else{
                            taskLogService.save(taskLog,domainInfo.getDomainId(),"update");
                        }
                        e.printStackTrace();
                    } catch (Exception e) {
                        log.error("定时同步异常：" + e);
                        taskLog.setStatus("failed");
                        taskLog.setReason(e.getMessage());
                        if (StringUtils.isNotBlank(TaskConfig.errorData.get(domainInfo.getId()))) {
                            taskConfig.upload(domainInfo, taskLog);
                        }else{
                            taskLogService.save(taskLog,domainInfo.getDomainId(),"update");
                        }
                        e.printStackTrace();
                    }
                } else {
                    taskLog.setReason("有编辑中规则，跳过数据同步");
                    taskLogService.save(taskLog, domainInfo.getId(), "skip");
                    log.info("编辑中规则数:{}", nodeRules.size());
                    log.info("有编辑中规则，跳过数据同步");
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

}