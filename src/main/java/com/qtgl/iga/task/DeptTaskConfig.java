package com.qtgl.iga.task;

import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.config.HttpApiThreadPool;
import com.qtgl.iga.service.DeptService;
import com.qtgl.iga.service.DomainInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;
import java.util.concurrent.ExecutorService;


@Slf4j
@Configuration
public class DeptTaskConfig {


    @Value("${iga.hostname}")
    String hostname;

    @Autowired
    DomainInfoService domainInfoService;

    @Autowired
    DeptService deptService;


    @Scheduled(cron = "${dept.task.cron}")
    public void deptTask() {
        try {
            log.info("hostname{}", hostname);
            // k8环境多节点部署环境下 仅01节点会执行定时任务
            if (hostname.contains("-01")) {
                List<DomainInfo> domainInfos = domainInfoService.findAll();
                domainInfos.forEach(domainInfo -> {
                            if (HttpApiThreadPool.executorServiceMap.containsKey(domainInfo.getDomainName())) {
                                ExecutorService executorService = HttpApiThreadPool.executorServiceMap.get(domainInfo.getDomainName());
                                executorService.execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        //log.info();
                                        deptService.buildDeptByDomain(domainInfo);
                                    }
                                });

                            } else {
                                HttpApiThreadPool.builderExecutor(domainInfo.getDomainName());
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
