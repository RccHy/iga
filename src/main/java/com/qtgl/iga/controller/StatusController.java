package com.qtgl.iga.controller;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

@Controller
public class StatusController {


    public static Logger logger = LoggerFactory.getLogger(StatusController.class);


    @GetMapping("/status")
    @ResponseBody
    public Map event() {
        Map map=new HashMap();
        map.putAll(getSystemRuntime());
        return map;
    }



    private Map getSystemRuntime() {
        try {
            Map rt = new HashMap();
            Runtime runtime = Runtime.getRuntime();
            rt.put("vm_total", runtime.totalMemory() / (1024 * 1024));
            rt.put("vm_free", runtime.freeMemory() / (1024 * 1024));
            rt.put("vm_max", runtime.maxMemory() / (1024 * 1024));
            ThreadGroup parentThread;
            int totalThread = 0;
            for (parentThread = Thread.currentThread().getThreadGroup(); parentThread
                    .getParent() != null; parentThread = parentThread.getParent()) {
                totalThread = parentThread.activeCount();
            }
            rt.put("vm_total_thread", totalThread);
            return rt;
        } catch (Exception e) {
            return new HashMap();
        }
    }


}
