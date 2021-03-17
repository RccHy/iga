package com.qtgl.iga.controller;

import com.qtgl.iga.bo.Dept;
import com.qtgl.iga.service.DeptService;
import com.qtgl.iga.utils.DataBusUtil;
import org.springframework.beans.factory.annotation.Autowired;
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

    @GetMapping("/depts")
    public List<Dept> getAllDepts() throws Exception {
        deptService.buildDept();

        return null;
    }


    @GetMapping("/url")
    @ResponseBody
    public Object getUrl(@RequestParam(required = false) String url) {
        return busUtil.getDataByBus(url);
    }
}