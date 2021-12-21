package com.qtgl.iga.utils;

import com.qtgl.iga.utils.enumerate.ResultCode;
import com.qtgl.iga.utils.exception.CustomException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

@Slf4j
@Component
public class UrlUtil implements InitializingBean {

    private static String originUrl;
    @Value("${server.origin}")
    String origin;

    /**
     * 可根据相对路径转成完整路径
     *
     * @param url
     * @return
     */
    public static String getUrl(String url) {

        String personalUrl = url;

        if (!isUrl(personalUrl)) {
            ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            //如果为外部请求则通过request对象转化绝对路径
            if (null != requestAttributes) {
                HttpServletRequest request = requestAttributes.getRequest();
                String port = ":" + request.getServerPort();
                if (("http".equalsIgnoreCase(request.getScheme()) && request.getServerPort() == 80)
                        || ("https".equalsIgnoreCase(request.getScheme()) && request.getServerPort() == 443)) {
                    port = "";
                }
                String base = request.getScheme() + "://" + request.getServerName() + port;
                if("10.168.3.115".equals(request.getServerName())){
                    base=originUrl;
                }

                personalUrl = personalUrl.startsWith("/") ? personalUrl : ("/" + personalUrl);
                personalUrl = base + personalUrl;
            } else {
                //如果为内部请求则通过配置属性转化绝对路径
                if ((originUrl.contains("http"))
                        || (originUrl.contains("https"))) {
                    personalUrl = personalUrl.startsWith("/") ? personalUrl : ("/" + personalUrl);
                    personalUrl = originUrl + personalUrl;
                } else {
                    log.error("配置路径{}有问题,请检查对应配置文件", originUrl);
                    throw new CustomException(ResultCode.FAILED, "配置路径有问题,请检查对应配置文件");
                }
            }
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

    @Override
    public void afterPropertiesSet() throws Exception {
        UrlUtil.originUrl = this.origin;
    }

    public void setOriginUrl(String originUrl) {
        UrlUtil.originUrl = originUrl;
    }
}
