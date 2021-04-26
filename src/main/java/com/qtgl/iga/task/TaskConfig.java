package com.qtgl.iga.task;

import com.qtgl.iga.bean.OccupyDto;
import com.qtgl.iga.bean.TreeBean;
import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.bo.Person;
import com.qtgl.iga.bo.TaskLog;
import com.qtgl.iga.config.TaskThreadPool;
import com.qtgl.iga.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

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


    /**
     * 根据租户区分线程池
     * 串行执行 部门、岗位、人员、三元组 同步
     */

    // @Scheduled(cron = "${dept.task.cron}")
    public void deptTask() {
        try {
            // k8环境多节点部署环境下 仅01节点会执行定时任务
            if (hostname.contains("-01")) {
                List<DomainInfo> domainInfos = domainInfoService.findAll();
                domainInfos.forEach(domainInfo -> {
                            if (TaskThreadPool.executorServiceMap.containsKey(domainInfo.getDomainName())) {
                                ExecutorService executorService = TaskThreadPool.executorServiceMap.get(domainInfo.getDomainName());
                                executorService.execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        TaskLog taskLog = new TaskLog();
                                        try {
                                            taskLog.setId(UUID.randomUUID().toString());
                                            log.info("{}开始同步,task:{}", domainInfo.getDomainName(), taskLog.getId());
                                            taskLogService.save(taskLog, domainInfo.getId(), "save");
                                            //部门数据同步至sso
                                            final Map<TreeBean, String> deptResult = deptService.buildDeptUpdateResult(domainInfo);
                                            log.info(Thread.currentThread().getName() + ": 部门同步完成：{}==={}", deptResult.size(), System.currentTimeMillis());
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
                                            taskLog.setPersonNo(occupyResult.size());
                                            taskLog.setStatus(1);
                                            taskLogService.save(taskLog, domainInfo.getId(), "update");
                                            log.info("{}同步结束,task:{}", domainInfo.getDomainName(), taskLog.getId());
                                        } catch (Exception e) {
                                            log.error("定时同步异常：" + e);
                                            taskLog.setStatus(2);
                                            taskLogService.save(taskLog, domainInfo.getId(), "update");
                                            e.printStackTrace();
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
