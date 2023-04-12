package com.qtgl.iga.config;

import org.springframework.stereotype.Component;

import java.util.concurrent.*;

@Component
public class ShadowCopyThreadPool {

    public static ExecutorService shadowCopyThreadPool = null;


    public static ConcurrentHashMap<String, ExecutorService> executorServiceMap = new ConcurrentHashMap<>();


    /**
     * 静态方法
     */
    public static void builderExecutor(String domain) {
        //httpApiThreadPool = Executors.newSingleThreadExecutor();
        shadowCopyThreadPool = new ThreadPoolExecutor(1, 1,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(1, true));
        executorServiceMap.put(domain, shadowCopyThreadPool);
    }
}
