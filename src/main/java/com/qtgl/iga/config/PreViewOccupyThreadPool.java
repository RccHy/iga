package com.qtgl.iga.config;

import org.springframework.stereotype.Component;

import java.util.concurrent.*;

/**
 * @author 1
 */
@Component
public class PreViewOccupyThreadPool {

    public static ExecutorService preViewOccupyThreadPool = null;


    public static ConcurrentHashMap<String, ExecutorService> executorServiceMap = new ConcurrentHashMap();


    /**
     * 静态方法
     */
    public static void builderExecutor(String domain) {
        //httpApiThreadPool = Executors.newSingleThreadExecutor();
        preViewOccupyThreadPool = new ThreadPoolExecutor(1, 1,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<Runnable>(1, true));
        executorServiceMap.put(domain, preViewOccupyThreadPool);
    }

}
