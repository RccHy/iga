package com.qtgl.iga.utils;

import org.apache.commons.lang3.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

public class UrlUtil {


    /**
     * 可根据相对路径转成完整路径
     *
     * @param url
     * @return
     */
    static String getUrl(String url) {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String personalUrl = url;
        String port = ":" + request.getServerPort();
        if (("http".equalsIgnoreCase(request.getScheme()) && request.getServerPort() == 80)
                || ("https".equalsIgnoreCase(request.getScheme()) && request.getServerPort() == 443)) {
            port = "";
        }

        String base = request.getScheme() + "://" + request.getServerName() + port;
        if (!isUrl(personalUrl)) {
            personalUrl = personalUrl.startsWith("/") ? personalUrl : ("/" + personalUrl);
            personalUrl = base + personalUrl;
        }

        if (StringUtils.isEmpty(url)) {
            return "";
        }

        return personalUrl;
    }

    private static boolean isUrl(String pInput) {
        if (pInput == null) {
            return false;
        }
        pInput = pInput.trim().toLowerCase();
        if (pInput.startsWith("http://") || pInput.startsWith("https://")) {
            return true;
        }
        return false;
    }
}
