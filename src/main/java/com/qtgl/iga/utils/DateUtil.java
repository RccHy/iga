package com.qtgl.iga.utils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DateUtil {

    public final static String DEFAULT_STANDARD_DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";
    public final static String DEFAULT_SHORT_DATE_PATTERN = "yyyy-MM-dd";
    public final static String DEFAULT_STRING_DATE_PATTERN = "yyyyMMdd";
    public final static String DEFAULT_STRING_DATETIME_PATTERN = "yyyyMMddHHmmss";
    public final static String DEFAULT_TIME_PATTERN = "HH:mm:ss";
    public final static String DEFAULT_YEAR_PATTERN = "yyyy";
    public final static String DEFAULT_MONTH_PATTERN = "yyyy-MM";

    public final static DateFormat DEFAULT_STANDARD_DATE_FORMAT = new SimpleDateFormat(DEFAULT_STANDARD_DATE_PATTERN);
    public final static DateFormat DEFAULT_SHORT_DATE_FORMAT = new SimpleDateFormat(DEFAULT_SHORT_DATE_PATTERN);
    public final static DateFormat DEFAULT_STRING_DATE_FORMAT = new SimpleDateFormat(DEFAULT_STRING_DATE_PATTERN);
    public final static DateFormat DEFAULT_STRING_DATETIME_FORMAT = new SimpleDateFormat(DEFAULT_STRING_DATETIME_PATTERN);
    public final static DateFormat DEFAULT_TIME_FORMAT = new SimpleDateFormat(DEFAULT_TIME_PATTERN);
    public final static DateFormat DEFAULT_YEAR_FORMAT = new SimpleDateFormat(DEFAULT_MONTH_PATTERN);
    public final static DateFormat DEFAULT_MONTH_FORMAT = new SimpleDateFormat(DEFAULT_MONTH_PATTERN);

    public static Date getNow() {
        Calendar calendar = Calendar.getInstance();
        return calendar.getTime();
    }

    public static Date getToday() {
        return getStartDate(getNow());
    }

    public static Date getYesterday() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, -1);
        setStartDay(calendar);
        return calendar.getTime();
    }

    public static String getDateFormat(String strDate, String inPattern, String outPattern) {
        Date date = parseDate(strDate, inPattern);
        if (date != null) {
            return formatDate(date, outPattern);
        }
        return strDate;
    }

    public static Date getStartDate(Date date) {
        if (date != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            setStartDay(cal);
            return cal.getTime();
        }

        return null;
    }

    //year, month, day都是自然日期
    public static Date getDate(int year, int month, int day) {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month - 1);
        cal.set(Calendar.DATE, day);
        return cal.getTime();
    }

    public static Date getEndDate(Date date) {
        if (date != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            setEndDay(cal);
            return cal.getTime();
        }

        return null;
    }

    public static Calendar setStartDay(Calendar cal) {
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal;
    }

    public static Calendar setEndDay(Calendar cal) {
        cal = setStartDay(cal);
        cal.add(Calendar.DAY_OF_MONTH, 1);
        cal.add(Calendar.MILLISECOND, -1);
        return cal;
    }

    public static String formatDate(Date date, String pattern) {
        return formatDate(date, new SimpleDateFormat(pattern));
    }

    private static String formatDate(Date date, DateFormat dateFormat) {
        if (date != null) {
            try {
                return dateFormat.format(date);
            } catch (Exception e) {
                return date.toString();
            }
        }

        return "";
    }

    public static String formatDate(Date date) {
        return formatDate(date, DEFAULT_STANDARD_DATE_PATTERN);
    }

    public static String formatShortDate(Date date) {
        return formatDate(date, DEFAULT_SHORT_DATE_PATTERN);
    }

    public static Date parseDate(String date, String pattern) {
        return parseDate(date, new SimpleDateFormat(pattern));
    }

    private static Date parseDate(String strDate, DateFormat dateFormat) {
        try {
            return dateFormat.parse(strDate);
        } catch (Exception e) {
            return null;
        }
    }

    public static Date parseDate(String date) {
        return parseDate(date, DEFAULT_STANDARD_DATE_PATTERN);
    }

    public static Date parseShortDate(String date) {
        return parseDate(date, DEFAULT_SHORT_DATE_PATTERN);
    }

    public static Calendar getCalendar(Calendar cal, int day) {
        cal = setStartDay(cal);
        cal.add(Calendar.DAY_OF_MONTH, day);
        return cal;
    }

    public static boolean isDate(String date, String pattern) {
        try {
            DateFormat sdf = new SimpleDateFormat(pattern);
            sdf.parse(date);
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    private static Date add(Calendar cal, int field, int amount) {
        cal.add(field, amount);
        return cal.getTime();
    }

    public static Date addNow(int field, int amount) {
        return add(Calendar.getInstance(), field, amount);
    }

    public static Date add(Date date, int field, int amount) {
        if (date != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            return add(cal, field, amount);
        }

        return null;
    }

    public static String add(String dateStr, int field, int amount) {
        Date date = parseShortDate(dateStr);
        date = add(date, field, amount);
        return formatShortDate(date);
    }

    public static Date getBefore(String actualYear, String actualMonth, int beforeMonths, int day) {
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(Calendar.YEAR, Integer.parseInt(actualYear));
        cal.set(Calendar.MONTH, Integer.parseInt(actualMonth) - 1);
        cal.set(Calendar.DAY_OF_MONTH, day);
        cal.add(Calendar.MONTH, -beforeMonths);
        return cal.getTime();
    }

    public static String getYearByLastMonth() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, -1);
        return String.valueOf(calendar.get(Calendar.YEAR));
    }

    public static String getLastMonth() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, -1);
        return formatDate(calendar.getTime(), "MM");
    }

    public static Date getStandardDate(String strDate) {
        return parseDate(strDate, DEFAULT_STANDARD_DATE_FORMAT);
    }

    public static Date getShortDate(String strDate) {
        return parseDate(strDate, DEFAULT_SHORT_DATE_FORMAT);
    }

    public static Date getYearDate(String strDate) {
        return parseDate(strDate, DEFAULT_YEAR_FORMAT);
    }

    public static Date getMonthDate(String strDate) {
        return parseDate(strDate, DEFAULT_MONTH_FORMAT);
    }

    public static String getStringStandard(Date date) {
        return formatDate(date, DEFAULT_STANDARD_DATE_FORMAT);
    }

    public static int getYear(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.YEAR);
    }

    public static int getCurrentYear() {
        Calendar calendar = Calendar.getInstance();
        return calendar.get(Calendar.YEAR);
    }

    public static int getMonth(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.MONTH) + 1;
    }

    /**
     * 获取今天是几月
     *
     * @return 1-12
     */
    public static int getCurrentMonth() {
        Calendar calendar = Calendar.getInstance();
        return calendar.get(Calendar.MONTH) + 1;
    }

    public static int getDay(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar.get(Calendar.DAY_OF_MONTH);
    }

    /**
     * 获取今天是几号
     *
     * @return 1-31
     */
    public static int getCurrentDay() {
        Calendar calendar = Calendar.getInstance();
        return calendar.get(Calendar.DATE);
    }

    public static int intervalWeeks(Date date1, Date date2) {
        long interval = date1.getTime() - date2.getTime();
        return Long.valueOf(interval / 604800000l).intValue();
    }


    public static Date getMonday(Date date) {
        Calendar calendar = Calendar.getInstance(Locale.CHINA);
        calendar.setTimeInMillis(date.getTime());
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        return calendar.getTime();
    }


    public static String timeStamp2Date(String seconds, String format) {
        if (seconds == null || seconds.isEmpty() || seconds.equals("null")) {
            return "";
        }
        if (format == null || format.isEmpty()) {
            format = "yyyy-MM-dd HH:mm:ss";
        }
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        return sdf.format(new Date(Long.valueOf(seconds + "000")));
    }

}
