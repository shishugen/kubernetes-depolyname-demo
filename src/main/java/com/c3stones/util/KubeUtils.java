package com.c3stones.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

/**
 * @ClassName: KubeUtils
 * @Description: TODO
 * @Author: stone
 * @Date: 2021/1/26 18:01
 */
@Slf4j
public class KubeUtils {

    private static final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
    private static final SimpleDateFormat formatDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");


    public static void main(String[] args) {
     String date = "2021-01-27T02:34:50Z";
    }

    /**
     * 返回 分钟
     * @param date
     * @return
     */
    public static String StringFormatDate(String date){
        if(StringUtils.isNotBlank(date)){
            //注意是空格+UTC
            date = date.replace("Z", " UTC");
            try {
                return  formatDate.format(format.parse(date));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * 30000-32767
     * @return
     */
    public static String randomPortName(){
        int max=10327670;
        int min=30000;
        Random random = new Random();
        return "pod-"+(random.nextInt(max)%(max-min+1) + min);
    }

    /**
     * 删除文件与目录
     * @param filePath
     */
    public static void removeTempFile(String... filePath)throws Exception {
        if(filePath.length > 0){
            for (int i = 0; i < filePath.length; i++){
                log.info("删除目录 : {} "+filePath[i]);
                File tempFilePath=new File(filePath[i]);
                if(tempFilePath.exists() && tempFilePath.isDirectory()){
                    for (File chunk : tempFilePath.listFiles()) {
                        if(chunk.isDirectory()){
                            removeTempFile(chunk.getPath());
                        }else{
                            chunk.delete();
                        }
                    }
                }
                tempFilePath.delete();
            }
        }
    }



}

