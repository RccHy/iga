package com.qtgl.iga.config;

import org.springframework.stereotype.Component;

import java.util.concurrent.*;

/**
 * @author 1
 */
@Component
public class TaskThreadPool {


    public static ExecutorService taskThreadPool = null;


    public static ConcurrentHashMap<String, ExecutorService> executorServiceMap = new ConcurrentHashMap();


    /**
     * 静态方法
     */
    public static ExecutorService builderExecutor(String domain) {
       return new ThreadPoolExecutor(1, 1,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<Runnable>(1, true));
    }

}
