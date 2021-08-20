package com.qtgl.iga.controller;


import com.qtgl.iga.bo.DomainInfo;
import com.qtgl.iga.service.TaskLogService;
import com.qtgl.iga.utils.CertifiedConnector;
import com.qtgl.iga.utils.UrlUtil;
import com.qtgl.iga.utils.enumerate.ResultCode;
import com.qtgl.iga.utils.exception.CustomException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;

/**
 * @author rcc
 */
@Controller
@RequestMapping("/download")
public class DownLoadController {

    public static Logger logger = LoggerFactory.getLogger(DownLoadController.class);

    @Resource
    TaskLogService taskLogService;

    @GetMapping("/file{id}")
    public void downloadFile(@RequestParam("id") String id, HttpServletResponse response) throws Exception {
        if (id == null) throw new CustomException(ResultCode.FAILED, "日志ID不能为空");
        DomainInfo domain = CertifiedConnector.getDomain();
        Map<String, String> map = taskLogService.downLog(id, domain.getId());
        String uri = map.get("uri");
        response.setHeader("Content-Disposition", "attachment; filename=" + map.get("fileName"));
        //拼接请求路径
        uri = UrlUtil.getUrl(uri);
        System.out.println(uri);
        sendGet(uri, response);
    }


    /**
     * 向指定URL发送GET方法的请求
     *
     * @param url 发送请求的URL
     * @return URL 所代表远程资源的响应结果
     */
    public static String sendGet(String url, HttpServletResponse response) {
        String result = "";
        BufferedReader in = null;
        try {
            String urlNameString = url;
            URL realUrl = new URL(urlNameString);
            // 打开和URL之间的连接
            URLConnection connection = realUrl.openConnection();
            // 设置通用的请求属性
            connection.setRequestProperty("accept", "*/*");
            connection.setRequestProperty("connection", "Keep-Alive");
            connection.setRequestProperty("user-agent",
                    "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            // 建立实际的连接
            connection.connect();
            StreamUtils.copy(connection.getInputStream(), response.getOutputStream());
        } catch (Exception e) {
            logger.error("发送GET请求出现异常{}", e);
            e.printStackTrace();
        }
        // 使用finally块来关闭输入流
        finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
        return result;
    }


}
