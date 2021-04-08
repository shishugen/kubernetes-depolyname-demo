package com.c3stones.client;

import com.c3stones.entity.Config;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @ClassName: BaseConfig
 * @Description: TODO
 * @Author: stone
 * @Date: 2021/4/6 15:38
 */
public class BaseConfig {

    protected static Integer dockerPort=2375;
    protected static String harborUser ;
    protected static String harborPassword ;
    protected static String harborUrl;
    protected static String harborImagePrefix ;
    protected static String harborImageProjectName ;
    protected static String harborImageEnvProjectName ;
    protected static String nfsStorageClassName;
    protected static Integer nfsMySqlStorageSize ;
    protected static Integer nfsNacosStorageSize ;
    protected  static Integer nfsMqStorageSize ;
    protected static Integer nfsFdfsStorageSize ;

    private static void getConfig(){
        Properties pro = new Properties();
        InputStream inputStream = null;
        try {
            File file = new File(getHomeConfigFile());
            if (file.isFile()) {
                //存在
                inputStream = new FileInputStream(file);
                 pro.load(inputStream);
                 setConfig(pro);
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if(inputStream != null){
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static void setConfig(Properties pro){
        dockerPort =Integer.valueOf(pro.getProperty("docker.port"));
         harborUser =pro.getProperty("harbor.user");
         harborPassword = pro.getProperty("harbor.password");
         harborUrl = pro.getProperty("harbor.url");
         harborImagePrefix = pro.getProperty("harbor.image.prefix");
         harborImageProjectName = pro.getProperty("harbor.image.project.name");
         harborImageEnvProjectName = pro.getProperty("harbor.image.env.project.name");
         nfsStorageClassName = pro.getProperty("nfs.storage.className");
         nfsMySqlStorageSize = Integer.valueOf(pro.getProperty("nfs.storage.MySql.size"));
         nfsNacosStorageSize =Integer.valueOf( pro.getProperty("nfs.storage.nacos.size"));
         nfsMqStorageSize = Integer.valueOf(pro.getProperty("nfs.storage.mq.size"));
         nfsFdfsStorageSize = Integer.valueOf(pro.getProperty("nfs.storage.fdfs.size"));
    }

    /**
     * docker 当前 配置
     * @return
     */
    public static String getHomeConfigFile() {
        return System.getProperty("user.home") + File.separator + ".kube-deployment"+ File.separator +"config";
    }

}
