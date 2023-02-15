package com.qtgl.iga.config;


import org.springframework.stereotype.Component;

import java.util.concurrent.*;

@Component
public class AvatarTaskThreadPool {

    public static ExecutorService avatarTaskThreadPool = null;


    public static ConcurrentHashMap<String, ExecutorService> executorServiceMap = new ConcurrentHashMap();


    /**
     * 静态方法
     */
    public static ExecutorService builderExecutor(String domain) {
        avatarTaskThreadPool = new ThreadPoolExecutor(1, 1,
                0L, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<Runnable>(1, true));
        return avatarTaskThreadPool;
    }
}
