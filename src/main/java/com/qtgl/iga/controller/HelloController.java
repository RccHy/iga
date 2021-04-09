package com.qtgl.iga.controller;

import com.qtgl.iga.bo.UpstreamType;
import com.qtgl.iga.config.TaskThreadPool;
import com.qtgl.iga.service.DeptService;
import com.qtgl.iga.utils.DataBusUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

@Controller
@RequestMapping("/test")
public class HelloController {

    @Autowired
    DeptService deptService;


    @Autowired
    DataBusUtil busUtil;


    @RequestMapping("/xc")
    @ResponseBody
    public String getXc() {
        ExecutorService executorService = TaskThreadPool.executorServiceMap.get("cloud.ketanyun.cn");
        ThreadPoolExecutor tpe = ((ThreadPoolExecutor) executorService);
        int queueSize = tpe.getQueue().size();
        System.out.println(Thread.currentThread().getName() + "当前排队线程数：" + queueSize);

        int activeCount = tpe.getActiveCount();
        System.out.println(Thread.currentThread().getName() + "当前活动线程数：" + activeCount);

        return Thread.currentThread().getName() + "当前排队线程数：" + queueSize+"；当前活动线程数：" + activeCount;

    }


    @RequestMapping("/url")
    @ResponseBody
    public Object getUrl(@RequestBody UpstreamType upstreamType) {
        return busUtil.getDataByBus(upstreamType);
    }
}