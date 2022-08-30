package com.qtgl.iga.config;

import com.qtgl.iga.service.PreViewTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PreViewRefresh implements ApplicationRunner {

    @Autowired
    PreViewTaskService preViewTaskService;

    @Override
    public void run(ApplicationArguments args) throws Exception {

        Integer byTaskId = preViewTaskService.makeTaskDone();
        log.info("---------------初始化人员,人员身份预览任务数据");
    }
}
