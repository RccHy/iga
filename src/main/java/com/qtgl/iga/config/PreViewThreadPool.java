package com.qtgl.iga.config;

import org.springframework.stereotype.Component;

import java.util.concurrent.*;

/**
 * @author 1
 */
@Component
public class PreViewThreadPool {


    public static ExecutorService taskThreadPool = null;


    public static ConcurrentHashMap<String, ExecutorService> executorServiceMap = new ConcurrentHashMap();


    /**
     * 静态方法
     */
    public static void builderExecutor(String domain) {
        //httpApiThreadPool = Executors.newSingleThreadExecutor();
        taskThreadPool = new ThreadPoolExecutor(1, 1,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<Runnable>(1, true));
        executorServiceMap.put(domain, taskThreadPool);
    }

}