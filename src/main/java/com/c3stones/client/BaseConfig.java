package com.c3stones.client;


import com.c3stones.entity.Config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

/**
 * @ClassName: BaseConfig
 * @Description: TODO
 * @Author: stone
 * @Date: 2021/4/6 15:38
 */
public class BaseConfig<T> {

    /**
     * libs
     */
    public final static String PVC_LIBS_LABEL ="JAVA-LIBS";

    public final static  String LABELS_JAR_NAME = "JAR_NAME";


    /**
     * 判断pod是否为网关
     */
    public static String GATEWAY_API_KEY="GATEWAY_API";
    /**
     * 判断pod是否为网关
     */
    public static String GATEWAY_API_VALUE="GATEWAY_API_VALUE";

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
    protected static Integer nfsNeo4jStorageSize ;
    protected static String harborImageEnvPrefix ;
    protected static String pythonRely ;
    protected static String bindK8sIP ;
    protected static String defaultNamespace ;

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

    public static Config setConfig(Properties pro){
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
         nfsNeo4jStorageSize = Integer.valueOf(pro.getProperty("nfs.storage.neo4j.size"));
         pythonRely = pro.getProperty("python.rely");
        bindK8sIP = pro.getProperty("bindK8sIP");
        defaultNamespace = pro.getProperty("defaultNamespace");
         //harborImageEnvPrefix = harborImagePrefix + "/" + harborImageEnvProjectName;
         harborImageEnvPrefix = harborImageEnvProjectName;
        return new Config(dockerPort,harborUser,harborPassword,harborUrl,harborImagePrefix,harborImageProjectName
                ,harborImageEnvProjectName,nfsStorageClassName,nfsMySqlStorageSize.toString(),nfsNacosStorageSize.toString()
                ,nfsMqStorageSize.toString(),nfsFdfsStorageSize.toString(),nfsNeo4jStorageSize.toString(),pythonRely,bindK8sIP,defaultNamespace);

    }

    /**
     * docker 当前 配置
     * @return
     */
    public static String getHomeConfigFile() {
        return System.getProperty("user.home") + File.separator + ".kube-deployment"+ File.separator +"config";
    }

    public static Config initConfig(){
        Properties pro = new Properties();
        InputStream inputStream = null;
        Config config = null;
        try {
            File file = new File(BaseConfig.getHomeConfigFile());
            if(file.isFile()){
                //存在
                inputStream = new FileInputStream(file);
                pro.load(inputStream);
                config = setConfig(pro);
            }else{
                ClassLoader classloader = Thread.currentThread().getContextClassLoader();
                inputStream = classloader.getResourceAsStream("application.properties");
                pro.load(inputStream);
                config = setConfig(pro);
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            try {
                if(inputStream != null){
                    inputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return config;
    }
/*
    private static Config propertiesToConfig(Properties pro){
        Integer dockerPort =Integer.valueOf(pro.getProperty("docker.port"));
        String harborUser =pro.getProperty("harbor.user");
        String harborPassword = pro.getProperty("harbor.password");
        String harborUrl = pro.getProperty("harbor.url");
        String harborImagePrefix = pro.getProperty("harbor.image.prefix");
        String harborImageProjectName = pro.getProperty("harbor.image.project.name");
        String harborImageEnvProjectName = pro.getProperty("harbor.image.env.project.name");
        String nfsStorageClassName = pro.getProperty("nfs.storage.className");
        String nfsMySqlStorageSize = pro.getProperty("nfs.storage.MySql.size");
        String nfsNacosStorageSize = pro.getProperty("nfs.storage.nacos.size");
        String nfsMqStorageSize = pro.getProperty("nfs.storage.mq.size");
        String nfsFdfsStorageSize = pro.getProperty("nfs.storage.fdfs.size");
        return new Config(dockerPort,harborUser,harborPassword,harborUrl,harborImagePrefix,harborImageProjectName
                ,harborImageEnvProjectName,nfsStorageClassName,nfsMySqlStorageSize,nfsNacosStorageSize,nfsMqStorageSize,nfsFdfsStorageSize);
    }*/

    /**
     * 设置分页
     * @param list
     * @param limit
     * @param page
     * @return
     */
    public List<T> setPage(List<T> list, Integer limit, Integer page){
        if (list != null && list.size() >0){
            int count = list.size();
            int pageNo=(page-1)*limit;
            if (pageNo+limit > count) {
                list = list.subList(pageNo,count);
            }else {
                list = list.subList(pageNo,pageNo+limit);
            }
        }
        return list;
    }

}
