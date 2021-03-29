package com.qtgl.iga.controller;

import com.qtgl.iga.bo.Dept;
import com.qtgl.iga.bo.UpstreamType;
import com.qtgl.iga.service.DeptService;
import com.qtgl.iga.utils.DataBusUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;


import java.util.List;

@Controller
@RequestMapping("/test")
public class HelloController {

    @Autowired
    DeptService deptService;


    @Autowired
    DataBusUtil busUtil;








    @RequestMapping("/url")
    @ResponseBody
    public Object getUrl(@RequestBody UpstreamType upstreamType) {
        return busUtil.getDataByBus(upstreamType);
    }
}