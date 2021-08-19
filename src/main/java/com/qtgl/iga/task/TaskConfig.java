package com.qtgl.iga.task;

import com.qtgl.iga.bean.OccupyDto;
import com.qtgl.iga.bean.TreeBean;
import com.qtgl.iga.bo.*;
import com.qtgl.iga.config.TaskThreadPool;
import com.qtgl.iga.dao.TenantDao;
import com.qtgl.iga.service.*;
import com.qtgl.iga.utils.DataBusUtil;
import com.qtgl.iga.utils.FileUtil;
import com.qtgl.iga.utils.exception.CustomException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;


@Slf4j
@Configuration
public class TaskConfig {


    @Value("${iga.hostname}")
    String hostname;

    @Autowired
    DomainInfoService domainInfoService;


    @Autowired
    DeptService deptService;

    @Autowired
    PersonService personService;

    @Autowired
    PostService postService;
    @Autowired
    OccupyService occupyService;
    @Autowired
    TaskLogService taskLogService;
    @Autowired
    NodeRulesService nodeRulesService;
    @Autowired
    DataBusUtil dataBusUtil;
    @Autowired
    FileUtil fileUtil;
    @Autowired
    DsConfigService dsConfigService;
    @Autowired
    TenantDao tenantDao;

    public static Map<String, String> errorData = new HashMap<>();

    public static final String SYNC_WAY_BRIDGE = "BRIDGE";
    public static final String SYNC_WAY_ENTERPRISE = "ENTERPRISE";
    public static final String SYNC_WAY_ENTERPRISE_GRAPHQL = "ENTERPRISE_GRAPHQL";

    /**
     * 根据租户区分线程池
     * 串行执行 部门、岗位、人员、三元组 同步
     */
    @Scheduled(cron = "${task.cron}")
    public void deptTask() {
        try {
            // k8环境多节点部署环境下 仅01节点会执行定时任务
            if (hostname.contains("-0")) {
                List<DomainInfo> domainInfos = domainInfoService.findAll();
                List<DsConfig> dsConfigs = dsConfigService.findAll();
                Map<String, DsConfig> collect = dsConfigs.stream().collect(Collectors.toMap(DsConfig::getTenantId, v -> v));

                for (DomainInfo domainInfo : domainInfos) {
                    Tenant tenant = tenantDao.findByDomainName(domainInfo.getDomainName());
                    //判断sso是否有定时任务
                    if (collect.containsKey(tenant.getId())) {
                        DsConfig dsConfig = collect.get(tenant.getId());
                        String syncWay = dsConfigService.getSyncWay(dsConfig);
                        if (StringUtils.isNotBlank(syncWay) && (syncWay.equals(SYNC_WAY_BRIDGE) || syncWay.equals(SYNC_WAY_ENTERPRISE) || syncWay.equals(SYNC_WAY_ENTERPRISE_GRAPHQL))) {
                            log.info("{}sso正在进行同步,跳过本次同步任务", domainInfo.getId());
                            continue;
                        }
                    }
                    if (TaskThreadPool.executorServiceMap.containsKey(domainInfo.getDomainName())) {
                        ExecutorService executorService = TaskThreadPool.executorServiceMap.get(domainInfo.getDomainName());
                        executorService.execute(() -> {
                            // 如果 获取最近一次同步任务状况
                            TaskLog lastTaskLog = taskLogService.last(domainInfo.getId());
                            //  最近一次同步任务 状态成功后才能继续同步
                            if ((null == lastTaskLog) || (null != lastTaskLog.getId() && !lastTaskLog.getStatus().equals("failed"))) {
                                errorData.remove(domainInfo.getId());
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
                                        if (personResult.size() > 0 && personResult.size() < 500) {
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
                                        if (occupyResult.size() > 0 && occupyResult.size() < 500) {
                                            pubResult = dataBusUtil.pub(null, null, occupyResult, "occupy", domainInfo);
                                            log.info("occupy pub:{}", pubResult);
                                        }
                                        //数据上传
                                        if (StringUtils.isNotBlank(TaskConfig.errorData.get(domainInfo.getId()))) {
                                            try {
                                                String fileName = LocalDateTime.now() + ".txt";
                                                String utf8 = fileUtil.putFile(TaskConfig.errorData.get(domainInfo.getId()).getBytes("UTF8"), fileName, domainInfo);
                                                if (null != utf8) {
                                                    taskLog.setData(utf8);
                                                    taskLogService.save(taskLog, domainInfo.getId(), "update");
                                                    log.info("上传文件{}成功", fileName);
                                                }
                                            } catch (Exception e) {
                                                log.error("上传文件失败:{}", e);
                                                e.printStackTrace();
                                            }
                                        } else {
                                            log.info("{}本次同步无异常数据", domainInfo.getDomainName());
                                        }
                                        log.info("{}同步结束,task:{}", domainInfo.getDomainName(), taskLog.getId());
                                    } catch (CustomException e) {
                                        log.error("定时同步异常：" + e);
                                        taskLog.setStatus("failed");
                                        taskLog.setReason(e.getErrorMsg());
                                        if (errorData.containsKey(domainInfo.getId())) {
                                            taskLog.setData(errorData.get(domainInfo.getId()));
                                        }
                                        String fileName = LocalDateTime.now() + ".txt";
                                        String utf8 = null;
                                        try {
                                            utf8 = fileUtil.putFile(TaskConfig.errorData.get(domainInfo.getId()).getBytes("UTF8"), fileName, domainInfo);
                                            log.info("上传文件{}成功", fileName);
                                        } catch (UnsupportedEncodingException unsupportedEncodingException) {
                                            log.error("上传文件失败:{}", e);
                                            unsupportedEncodingException.printStackTrace();
                                        }
                                        taskLog.setData(utf8);
                                        taskLogService.save(taskLog, domainInfo.getId(), "update");
                                        e.printStackTrace();
                                    } catch (Exception e) {
                                        log.error("定时同步异常：" + e);
                                        taskLog.setStatus("failed");
                                        taskLog.setReason(e.getMessage());
                                        if (errorData.containsKey(domainInfo.getId())) {
                                            taskLog.setData(errorData.get(domainInfo.getId()));
                                        }
                                        String fileName = LocalDateTime.now() + ".txt";
                                        String utf8 = null;
                                        try {
                                            utf8 = fileUtil.putFile(TaskConfig.errorData.get(domainInfo.getId()).getBytes("UTF8"), fileName, domainInfo);
                                            log.info("上传文件{}成功", fileName);
                                        } catch (UnsupportedEncodingException unsupportedEncodingException) {
                                            log.error("上传文件失败:{}", e);
                                            unsupportedEncodingException.printStackTrace();
                                        }
                                        taskLog.setData(utf8);
                                        taskLogService.save(taskLog, domainInfo.getId(), "update");
                                        e.printStackTrace();
                                    }
                                } else {
                                    taskLog.setReason("有编辑中规则，跳过数据同步");
                                    taskLogService.save(taskLog, domainInfo.getId(), "skip");
                                    log.info("编辑中规则数:{}", nodeRules.size());
                                    log.info("有编辑中规则，跳过数据同步");
                                }
                            }
                        });

                    } else {
                        TaskThreadPool.builderExecutor(domainInfo.getDomainName());
                        deptTask();
                    }
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
