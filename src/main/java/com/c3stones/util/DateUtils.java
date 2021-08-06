package com.c3stones.util;

import lombok.extern.slf4j.Slf4j;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * @ClassName: DateUtils
 * @Description: TODO 日期格式化
 * @Author: stone
 * @Date: 2020/11/30 16:11
 */
@Slf4j
public class DateUtils {

    private static final SimpleDateFormat  FORMAT  = new SimpleDateFormat("yyyy-MM-dd' : 'HH:mm:ss");
    private static final SimpleDateFormat FORMAT_DATE = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");


    /**
     * 返回 分钟
     * @param date
     * @return
     */
    public static Long StringFormatCurrentMinutes(String date){
        //注意是空格+UTC
            date = date.replace("Z", " UTC");
        Date d = null;
        try {
            d = FORMAT.parse(date);
        } catch (ParseException e) {
            log.error("时间转换异常! date : {}",date,e);
            e.printStackTrace();
        }
        long time =0L;
        if(d!=null){
            time  = d.getTime();
        }

            long timeMillis = System.currentTimeMillis();
            long con = timeMillis - time;
            return con / 1000 /60;
    }


    /**
     * 返回 分钟
     * @param date
     * @return
     */
    public static String StringFormat(String date){
        if (date == null){
            return null;
        }
        //注意是空格+UTC
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.CHINESE);
        Date parse = null;
        try {
            parse = format.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return FORMAT.format(parse);
    }

}
