package com.qtgl.iga.task;

import com.qtgl.iga.bean.TreeBean;
import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.bo.Person;
import com.qtgl.iga.config.TaskThreadPool;
import com.qtgl.iga.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;
import java.util.Map;
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

    /**
     * 根据租户区分线程池
     * 串行执行 部门、岗位、人员、三元组 同步
     */

    @Scheduled(cron = "${dept.task.cron}")
    public void deptTask() {
        try {
            log.info("hostname{}", hostname);
            // k8环境多节点部署环境下 仅01节点会执行定时任务
            if (hostname.contains("-01")) {
                List<DomainInfo> domainInfos = domainInfoService.findAll();
                domainInfos.forEach(domainInfo -> {
                            if (TaskThreadPool.executorServiceMap.containsKey(domainInfo.getDomainName())) {
                                ExecutorService executorService = TaskThreadPool.executorServiceMap.get(domainInfo.getDomainName());
                                executorService.execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            //部门数据同步至sso
                                            final Map<TreeBean, String> deptResult = deptService.buildDeptUpdateResult(domainInfo);
                                            log.info(Thread.currentThread().getName() + ": 部门同步完成：{}==={}", deptResult.size(), System.currentTimeMillis());

                                            //岗位数据同步至sso
                                            final Map<TreeBean, String> treeBeanStringMap = postService.buildPostUpdateResult(domainInfo);
                                            log.info(Thread.currentThread().getName() + ": 岗位同步完成：{}==={}", treeBeanStringMap.size(), System.currentTimeMillis());

                                            //人员数据同步至sso
                                            Map<String, List<Person>> personResult = personService.buildPerson(domainInfo);
                                            log.info(Thread.currentThread().getName() + ": 人员同步完成{}==={}", personResult.size(), System.currentTimeMillis());

                                            // todo 人员身份同步

                                        } catch (Exception e) {
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
            // e.printStackTrace();
        }
    }

}
