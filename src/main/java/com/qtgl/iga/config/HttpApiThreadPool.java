package com.qtgl.iga.config;

import org.springframework.stereotype.Component;

import java.util.concurrent.*;

@Component
public class HttpApiThreadPool {


	public static ExecutorService httpApiThreadPool = null;


	public static ConcurrentHashMap<String,ExecutorService> executorServiceMap=new  ConcurrentHashMap();


	/**
	 * 静态方法
	 */
	public static void builderExecutor(String domain){
		httpApiThreadPool = Executors.newSingleThreadExecutor();
		executorServiceMap.put(domain,httpApiThreadPool);
	}

}
