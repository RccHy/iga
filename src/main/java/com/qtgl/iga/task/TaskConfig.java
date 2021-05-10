package com.qtgl.iga.task;

import com.qtgl.iga.bean.OccupyDto;
import com.qtgl.iga.bean.TreeBean;
import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.bo.NodeRules;
import com.qtgl.iga.bo.Person;
import com.qtgl.iga.bo.TaskLog;
import com.qtgl.iga.config.TaskThreadPool;
import com.qtgl.iga.service.*;
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
                                        // 如果有编辑中的规则，则不进行数据同步
                                        Map<String, Object> arguments = new HashMap<>();
                                        arguments.put("status", 1);
                                        final List<NodeRules> nodeRules = nodeRulesService.findNodeRules(arguments, domainInfo.getId());
                                        if (null==nodeRules||nodeRules.size() == 0) {
                                            TaskLog taskLog = new TaskLog();
                                            try {
                                                taskLog.setId(UUID.randomUUID().toString());
                                                log.info("{}开始同步,task:{}", domainInfo.getDomainName(), taskLog.getId());
                                                taskLogService.save(taskLog, domainInfo.getId(), "save");
                                                //部门数据同步至sso
                                                final Map<TreeBean, String> deptResult = deptService.buildDeptUpdateResult(domainInfo);
                                                log.info(Thread.currentThread().getName() + ": 部门同步完成：{}==={}", deptResult.size(), System.currentTimeMillis());
                                                taskLog.setStatus("doing");
                                                taskLog.setDeptNo(deptResult.size());
                                                taskLogService.save(taskLog, domainInfo.getId(), "update");
                                                //岗位数据同步至sso
                                                final Map<TreeBean, String> treeBeanStringMap = postService.buildPostUpdateResult(domainInfo);
                                                log.info(Thread.currentThread().getName() + ": 岗位同步完成：{}==={}", treeBeanStringMap.size(), System.currentTimeMillis());

                                                taskLog.setPersonNo(treeBeanStringMap.size());
                                                taskLogService.save(taskLog, domainInfo.getId(), "update");
                                                //人员数据同步至sso
                                                Map<String, List<Person>> personResult = personService.buildPerson(domainInfo);
                                                log.info(Thread.currentThread().getName() + ": 人员同步完成{}==={}", personResult.size(), System.currentTimeMillis());
                                                taskLog.setPersonNo(personResult.size());
                                                taskLogService.save(taskLog, domainInfo.getId(), "update");
                                                //人员身份同步至sso
                                                final Map<String, List<OccupyDto>> occupyResult = occupyService.buildPerson(domainInfo);
                                                log.info(Thread.currentThread().getName() + ": 人员身份同步完成{}==={}", occupyResult.size(), System.currentTimeMillis());
                                                taskLog.setStatus("done");
                                                taskLog.setOccupyNo(occupyResult.size());
                                                taskLogService.save(taskLog, domainInfo.getId(), "update");
                                                log.info("{}同步结束,task:{}", domainInfo.getDomainName(), taskLog.getId());
                                            } catch (Exception e) {
                                                log.error("定时同步异常：" + e);
                                                taskLog.setStatus("failed");
                                                taskLogService.save(taskLog, domainInfo.getId(), "update");
                                                e.printStackTrace();
                                            }
                                        }else{
                                            log.info("编辑中规则数:{}",nodeRules.size());
                                            log.info("有编辑中规则，跳过数据同步");
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
