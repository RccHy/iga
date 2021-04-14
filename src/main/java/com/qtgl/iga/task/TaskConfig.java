package com.qtgl.iga.task;

import com.qtgl.iga.bean.TreeBean;
import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.bo.Person;
import com.qtgl.iga.config.TaskThreadPool;
import com.qtgl.iga.service.DeptService;
import com.qtgl.iga.service.DomainInfoService;
import com.qtgl.iga.service.PersonService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

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

    /**
     * 根据租户区分线程池
     * 串行执行 部门、岗位、人员、三元组 同步
     */

    // @Scheduled(cron = "${dept.task.cron}")
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
                                            log.info(Thread.currentThread().getName() + ": 开始" + System.currentTimeMillis());
                                            //todo 部门数据同步至sso
                                            final Map<TreeBean, String> deptResult = deptService.buildDeptUpdateResult(domainInfo);
                                            //todo 岗位数据同步至sso
                                            //
                                            //todo 人员数据同步至sso
                                            Map<String, List<Person>> personResult = personService.buildPerson(domainInfo);
                                            //todo 人员身份数据同步至sso

                                            //todo 发送消息 MQ。
                                            Thread.sleep(20000);
                                            log.info(Thread.currentThread().getName() + ": 结束" + System.currentTimeMillis());
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
