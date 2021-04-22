package com.qtgl.iga.controller;

import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.config.TaskThreadPool;
import com.qtgl.iga.service.DeptService;
import com.qtgl.iga.service.DomainInfoService;
import com.qtgl.iga.service.PostService;
import com.qtgl.iga.utils.DataBusUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

@Controller
@RequestMapping("/test")
public class HelloController {

    @Autowired
    DeptService deptService;
    @Autowired
    PostService postService;


    @Autowired
    DataBusUtil busUtil;

    @Autowired
    DomainInfoService domainInfoService;


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

    @RequestMapping("/cs")
    @ResponseBody
    public void testDb() {
        HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();
        DomainInfo byDomainName = domainInfoService.getByDomainName(request.getServerName());

        deptService.buildDeptUpdateResult(byDomainName);

    }

    @RequestMapping("/post")
    @ResponseBody
    public void testPost() {
        HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();
        DomainInfo byDomainName = domainInfoService.getByDomainName(request.getServerName());

        postService.buildPostUpdateResult(byDomainName);

    }


}