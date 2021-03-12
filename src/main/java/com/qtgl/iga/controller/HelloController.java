package com.qtgl.iga.controller;

import com.qtgl.iga.bo.Dept;
import com.qtgl.iga.service.DeptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import java.util.List;

@Controller
@RequestMapping("/test")
public class HelloController {

    @Autowired
    DeptService deptService;

    @GetMapping("/depts")
    public List<Dept> getAllDepts() throws Exception {
         deptService.buildDept();

        return null;
    }

}