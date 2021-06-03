package com.qtgl.iga.task;

import com.qtgl.iga.bean.OccupyDto;
import com.qtgl.iga.bean.TreeBean;
import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.bo.NodeRules;
import com.qtgl.iga.bo.Person;
import com.qtgl.iga.bo.TaskLog;
import com.qtgl.iga.config.TaskThreadPool;
import com.qtgl.iga.service.*;
import com.qtgl.iga.utils.DataBusUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

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

    public static Map<String, String> errorData = new HashMap<>();


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
                domainInfos.forEach(domainInfo -> {
                            if (TaskThreadPool.executorServiceMap.containsKey(domainInfo.getDomainName())) {
                                ExecutorService executorService = TaskThreadPool.executorServiceMap.get(domainInfo.getDomainName());
                                executorService.execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        // 如果 获取最近一次同步任务状况
                                        TaskLog lastTaskLog = taskLogService.last(domainInfo.getId());
                                        //  最近一次同步任务 状态不为 失败 。 如果是失败应该先解决问题
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
                                                    String deptNo = (deptResultMap.containsKey("insert") ? String.valueOf(deptResultMap.get("insert").size()) : "0") + "/"
                                                            + (deptResultMap.containsKey("delete") ? String.valueOf(deptResultMap.get("delete").size()) : "0") + "/"
                                                            + (deptResultMap.containsKey("update") ? String.valueOf(deptResultMap.get("update").size()) : "0");

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
                                                    String postNo = (postResultMap.containsKey("insert") ? String.valueOf(postResultMap.get("insert").size()) : "0") + "/"
                                                            + (postResultMap.containsKey("delete") ? String.valueOf(postResultMap.get("delete").size()) : "0") + "/"
                                                            + (postResultMap.containsKey("update") ? String.valueOf(postResultMap.get("update").size()) : "0");
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
                                                    String personNo = (personResult.containsKey("insert") ? String.valueOf(personResult.get("insert").size()) : "0") + "/"
                                                            + (personResult.containsKey("delete") ? String.valueOf(personResult.get("delete").size()) : "0") + "/"
                                                            + (personResult.containsKey("update") ? String.valueOf(personResult.get("update").size()) : "0");
                                                    log.info(Thread.currentThread().getName() + ": 人员同步完成{}==={}", personNo, System.currentTimeMillis());
                                                    taskLog.setPersonNo(personNo);
                                                    taskLogService.save(taskLog, domainInfo.getId(), "update");

                                                    // PUT   MQ
                                                    if (personResult.size() > 0) {
                                                        pubResult = dataBusUtil.pub(null, personResult, null, "person", domainInfo);
                                                        log.info("person pub:{}", pubResult);
                                                    }

                                                    //人员身份同步至sso
                                                    final Map<String, List<OccupyDto>> occupyResult = occupyService.buildPerson(domainInfo, lastTaskLog);
                                                    String occupyNo = (occupyResult.containsKey("insert") ? String.valueOf(occupyResult.get("insert").size()) : "0") + "/"
                                                            + (occupyResult.containsKey("delete") ? String.valueOf(occupyResult.get("delete").size()) : "0") + "/"
                                                            + (occupyResult.containsKey("update") ? String.valueOf(occupyResult.get("update").size()) : "0");
                                                    log.info(Thread.currentThread().getName() + ": 人员身份同步完成{}==={}", occupyNo, System.currentTimeMillis());
                                                    taskLog.setStatus("done");
                                                    taskLog.setOccupyNo(occupyNo);
                                                    taskLogService.save(taskLog, domainInfo.getId(), "update");

                                                    // PUT   MQ
                                                    if (occupyResult.size() > 0) {
                                                        pubResult = dataBusUtil.pub(null, null, occupyResult, "occupy", domainInfo);
                                                        log.info("occupy pub:{}", pubResult);
                                                    }

                                                    log.info("{}同步结束,task:{}", domainInfo.getDomainName(), taskLog.getId());
                                                } catch (Exception e) {
                                                    log.error("定时同步异常：" + e);
                                                    taskLog.setStatus("failed");
                                                    taskLog.setReason(e.getMessage());
                                                    if (errorData.containsKey(domainInfo.getId())) {
                                                        taskLog.setData(errorData.get(domainInfo.getId()));
                                                    }
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
                                    }
                                });

                            } else {
                                TaskThreadPool.builderExecutor(domainInfo.getDomainName());
                                deptTask();
                            }

                        }
                );
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
