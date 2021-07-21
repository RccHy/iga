package com.qtgl.iga.controller;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author 1
 */
@Controller
public class StatusController {


    public static Logger logger = LoggerFactory.getLogger(StatusController.class);



    @GetMapping("/livez")
    @ResponseBody
    public String livez() {
        return "200";
    }

    @GetMapping("/readyz")
    @ResponseBody
    public String readyz() {

        return "200";
    }

    @GetMapping("/status")
    @ResponseBody
    public Map status() {
        Map map = new HashMap();
        map.putAll(getSystemRuntime());
        return map;
    }

    @GetMapping("/version")
    @ResponseBody
    public String version() {

        return getClassVersion("yyyy-MM-dd");
    }

    public static String getClassVersion(String pattern) {
        Date created = new Date();
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try {
            String path = cl.getResource("application.yml").getPath();
            if ("\\".equalsIgnoreCase(File.separator)) {
                path = path.replaceAll("/", "\\\\");
                if (path.substring(0, 1).equals("\\")) {
                    path = path.substring(1);
                }
            } else if ("/".equalsIgnoreCase(File.separator)) {
                path = path.replaceAll("\\\\", "/");
            }
            if (null != path) {
                long time = Files.readAttributes(Paths.get(path), BasicFileAttributes.class).creationTime().toMillis();
                created = new Date(time);
            }
        } catch (Exception e) {
            logger.error("Failed to obtain class attributes.", e);
        }
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        return sdf.format(created);
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
