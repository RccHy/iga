package com.qtgl.iga.utils;

import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.converters.*;

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.util.Map;

public class MyBeanUtils extends org.apache.commons.beanutils.BeanUtils {


    public static void populate(Object bean, Map<String, ? extends Object> properties) throws IllegalAccessException, InvocationTargetException {
        ConvertUtils.register(new LongConverter(null), Long.class);
        ConvertUtils.register(new ShortConverter(null), Short.class);
        ConvertUtils.register(new IntegerConverter(null), Integer.class);
        ConvertUtils.register(new DoubleConverter(null), Double.class);
        ConvertUtils.register(new BigDecimalConverter(null), BigDecimal.class);
        ConvertUtils.register(new ByteConverter(null), Byte.class);
        BeanUtilsBean.getInstance().populate(bean, properties);
    }

}
