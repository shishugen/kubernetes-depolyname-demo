package com.c3stones.service;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

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



    public static void main(String[] args) {
        FTPClient fc = null;
        try {
            //创建ftp客户端
            fc = new FTPClient();
            //设置连接地址和端口
            // fc.connect("10.49.0.12", 31368);
             fc.connect("10.49.0.12", 30021);
           // fc.connect("139.9.46.153", 30021);
            //设置用户和密码
            boolean login = fc.login("myuser", "mypass");

            //设置文件类型
            fc.setFileType(FTP.BINARY_FILE_TYPE);
            System.out.println("login=="+login);

            //上传
            boolean flag = fc.storeFile("test3377.jpg", new FileInputStream(new File("E:/test.jpg")));
          //  fc.enterLocalPassiveMode();
            // boolean b = fc.deleteFile("test.jpg");
            FTPFile[] ftpFiles = fc.listFiles();
            System.out.println(ftpFiles.length);
            if (flag)
                System.out.println("上传成功...");
            else
                System.out.println("上传失败...");
        }catch (Exception e){
            e.printStackTrace();
        }


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
