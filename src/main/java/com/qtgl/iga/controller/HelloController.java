package com.qtgl.iga.controller;

import com.qtgl.iga.bo.Dept;
import com.qtgl.iga.service.DeptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


import java.util.List;

@RestController
public class HelloController {

    @Autowired
    DeptService deptService;

    @GetMapping("/depts")
    public List<Dept> getAllDepts() {
        List<Dept> allDepts = deptService.getAllDepts();

        return allDepts;
    }

}