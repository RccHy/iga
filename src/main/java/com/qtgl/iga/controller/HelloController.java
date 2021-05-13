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

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author 1
 */
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
            System.out.println("=======START=======" + DateUtil.getNow() + "=================");
            final List<DomainInfo> all = domainInfoService.findAll();
            final DomainInfo domainInfo = all.get(0);
            //部门数据同步至sso
            final Map<TreeBean, String> deptResult = deptService.buildDeptUpdateResult(domainInfo);
            log.info(Thread.currentThread().getName() + ": 部门同步完成：{}==={}", deptResult.size(), System.currentTimeMillis());
            String pubResult=busUtil.pub(deptResult, null,null,"dept",domainInfo);
            log.info("dept pub:{}",pubResult);

            //岗位数据同步至sso
            final Map<TreeBean, String> treeBeanStringMap = postService.buildPostUpdateResult(domainInfo);
            log.info(Thread.currentThread().getName() + ": 岗位同步完成：{}==={}", treeBeanStringMap.size(), System.currentTimeMillis());
              pubResult=busUtil.pub(treeBeanStringMap,null,null,"post",domainInfo);
            log.info("post pub:{}",pubResult);
            //人员数据同步至sso
            Map<String, List<Person>> personResult = personService.buildPerson(domainInfo);
            log.info(Thread.currentThread().getName() + ": 人员同步完成{}==={}", personResult.size(), System.currentTimeMillis());
            pubResult=busUtil.pub(null,personResult,null,"person",domainInfo);
            log.info("person pub:{}",pubResult);
            //人员身份同步至sso
            final Map<String, List<OccupyDto>> occupyResult = occupyService.buildPerson(domainInfo);
            log.info(Thread.currentThread().getName() + ": 人员身份同步完成{}==={}", occupyResult.size(), System.currentTimeMillis());
            pubResult=busUtil.pub(null,null,occupyResult,"occupy",domainInfo);
            log.info("person pub:{}",pubResult);

            System.out.println("=======END=======" + DateUtil.getNow() + "=================");
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

    public static void main(String[] args) throws ScriptException {

        String str = "=red(1*)";
        String pattern = "=[a-zA-Z0-9_]*([a-zA-Z0-9_]*)";

        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(str);
        System.out.println(m.matches());
        System.out.println(Pattern.matches("=[a-zA-Z0-9_*]*\\([a-zA-Z0-9_*+\\[ \\]]*\\)", "=rrr(1[0-9]*)"));


        //        SimpleBindings bindings = new SimpleBindings();
//        bindings.put("$code", "1000");
//        String reg="(1+2)+$code";
//        ScriptEngineManager sem = new ScriptEngineManager();
//        ScriptEngine engine = sem.getEngineByName("js");
//        final Object eval = engine.eval(reg, bindings);
//        System.out.println(eval);
//        String pattern = "\\$[a-zA-Z0-9_]+";
//        String reg = "=[a-zA-Z0-9_]+";
//
//
//        Pattern r = Pattern.compile(reg);
//        Matcher m = r.matcher("=$Parent");
//
//        if (m.find()) {
//            System.out.println("Found value: " + m.group(0));
//        } else {
//            System.out.println("NO MATCH");
//        }

    }


}