package com.c3stones.service;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @ClassName: Test
 * @Description: TODO
 * @Author: stone
 * @Date: 2021/3/23 10:11
 */
public class Test {





    static long whiteLine = 0;
    static long comentLine = 0;
    static long sormaLine = 0;

    /**
     * 有效代码行数: 733
     * 注释行数: 19000
     * 空白行数: 1535
     * 总代码行数: 21267
     * @param args 14468 301611 638588
     */

    public static void main(String[] args) {
        File f = new File("E:\\xuanyuan_project\\xuanyuan-base-spring-cloud"); // 在这里输入需要统计的文件夹路径
        File[] codeFiles = f.listFiles();
        for(File child:codeFiles){
            //System.out.println(child);
            preas(child);
        }
        System.out.println("空行："+whiteLine);
        System.out.println("注释行："+comentLine);
        System.out.println("有效行："+sormaLine);
    }

    private static void preas(File f){
        BufferedReader br = null;
        Boolean comPd = false;
        try {
            br = new BufferedReader(new FileReader(f));
            String readLine = null;
            while((readLine = br.readLine())!=null){
                readLine = readLine.trim();
                if(readLine.matches("^[\\s&&[^\\n]]*$")){
                    whiteLine ++;
                }else if(readLine.startsWith("/*")&&!readLine.endsWith("*/")){
                    comentLine ++;
                    comPd = true;
                }else if(readLine.startsWith("/*")&&!readLine.endsWith("*/")){
                    comentLine ++;
                }else if(comPd){
                    comentLine ++;
                    if(readLine.endsWith("*/")){
                        comPd = false;
                    }
                }else if(readLine.startsWith("//")){
                    comentLine ++;
                }else{
                    sormaLine++;
                }
            }
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
