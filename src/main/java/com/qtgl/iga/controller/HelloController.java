package com.qtgl.iga.controller;

import com.qtgl.iga.bean.OccupyDto;
import com.qtgl.iga.bean.TreeBean;
import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.bo.Person;
import com.qtgl.iga.config.TaskThreadPool;
import com.qtgl.iga.dao.PersonDao;
import com.qtgl.iga.service.*;
import com.qtgl.iga.utils.DataBusUtil;
import com.qtgl.iga.utils.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

@Controller
@RequestMapping("/test")
@Slf4j
public class HelloController {

    @Autowired
    DeptService deptService;
    @Autowired
    PostService postService;
    @Autowired
    PersonService personService;
    @Autowired
    OccupyService occupyService;


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

    @RequestMapping("/saveToSSO")
    @ResponseBody
    public void testDb() {
        try {
            System.out.println("=======START======="+ DateUtil.getNow() +"=================");
            final List<DomainInfo> all = domainInfoService.findAll();
            final DomainInfo domainInfo = all.get(0);
            //部门数据同步至sso
            final Map<TreeBean, String> deptResult = deptService.buildDeptUpdateResult(domainInfo);
            log.info(Thread.currentThread().getName() + ": 部门同步完成：{}==={}", deptResult.size(), System.currentTimeMillis());

            //岗位数据同步至sso
            final Map<TreeBean, String> treeBeanStringMap = postService.buildPostUpdateResult(domainInfo);
            log.info(Thread.currentThread().getName() + ": 岗位同步完成：{}==={}", treeBeanStringMap.size(), System.currentTimeMillis());

            //人员数据同步至sso
            Map<String, List<Person>> personResult = personService.buildPerson(domainInfo);
            log.info(Thread.currentThread().getName() + ": 人员同步完成{}==={}", personResult.size(), System.currentTimeMillis());

            //人员身份同步至sso
            final Map<String, List<OccupyDto>> occupyResult = occupyService.buildPerson(domainInfo);
            log.info(Thread.currentThread().getName() + ": 人员身份同步完成{}==={}", occupyResult.size(), System.currentTimeMillis());

            System.out.println("=======END======="+ DateUtil.getNow() +"=================");
        } catch (Exception e) {
            log.error("定时同步异常：" + e);
            e.printStackTrace();
        }

    }

    @RequestMapping("/post")
    @ResponseBody
    public void testPost() {
        HttpServletRequest request = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();
        DomainInfo byDomainName = domainInfoService.getByDomainName(request.getServerName());

        postService.buildPostUpdateResult(byDomainName);

    }


}