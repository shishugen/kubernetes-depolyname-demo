package com.c3stones.client;


import com.c3stones.entity.Config;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * @ClassName: BaseConfig
 * @Description: TODO
 * @Author: stone
 * @Date: 2021/4/6 15:38
 */
@Slf4j
public class BaseConfig<T> {





    public  static  String LABELS_KEY_TEST = "app-test-memory2";

    /**
     * 节点选择器
     */
    public  static  String K8S_NODE_LABEL_KEY = "sys_x86_node_name";
    /**
     * 节点选择器
     */
    public  static  String K8S_NODE_LABEL_VALUE = "sys_x86";

    @Value("${k8s.arm}")
    public Boolean k8sArm;

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
    protected static String k8sNetPort ;
    protected static String k8sNetNodePort ;
    /**
     * 邮件地址
     */
    protected static String mailHost ;

    /**
     * 邮件端口
     */
    protected static Integer mailPort ;

    protected static String mailUser ;

    protected static String mailPass;

    protected static String mailPerson;

    protected static String mailSubject;

    protected static String checkHarborIp;

    protected static String checkNfsIp;

    protected static String checkNamespaces;

    public   Map<String,String> nodeSelectorMap = new HashMap<>();
    public  Map<String,String>  isk8sArm(){
        if (k8sArm){
            nodeSelectorMap.put(K8S_NODE_LABEL_KEY,K8S_NODE_LABEL_VALUE);
        }
        return nodeSelectorMap;
    }
    public void init(){
        //isk8sArm();
        log.info("初始化参数……",nodeSelectorMap.size());
    }

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
        String property = pro.getProperty("docker.port");
        if (StringUtils.isNotBlank(property)){
            dockerPort =Integer.valueOf(property);
        }
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
        k8sNetPort = pro.getProperty("k8sNetPort");
        k8sNetNodePort = pro.getProperty("k8sNetNodePort");
        mailHost = pro.getProperty("mail.host");
        mailPort = Integer.valueOf(pro.getProperty("mail.port","25"));
        mailUser = pro.getProperty("mail.user");
        mailPass = pro.getProperty("mail.pass");
        mailPerson = pro.getProperty("mail.person");
        mailSubject = pro.getProperty("mail.subject");
        checkNfsIp = pro.getProperty("check.nfs.ip");
        checkHarborIp = pro.getProperty("check.harbor.ip");
        checkNamespaces = pro.getProperty("check.namespaces");
         //harborImageEnvPrefix = harborImagePrefix + "/" + harborImageEnvProjectName;
         harborImageEnvPrefix = harborImageEnvProjectName;
        return new Config(dockerPort,harborUser,harborPassword,harborUrl,harborImagePrefix,harborImageProjectName
                ,harborImageEnvProjectName,nfsStorageClassName,nfsMySqlStorageSize.toString(),nfsNacosStorageSize.toString()
                ,nfsMqStorageSize.toString(),nfsFdfsStorageSize.toString(),nfsNeo4jStorageSize.toString(),pythonRely,bindK8sIP,defaultNamespace
        ,k8sNetPort,k8sNetNodePort,mailHost,mailPort,mailUser,mailPass,mailPerson,mailSubject,checkHarborIp,checkNfsIp,checkNamespaces);

    }

    /**
     * docker 当前 配置
     * @return
     */
    public static String getHomeConfigFile() {
        return System.getProperty("user.home") + File.separator + ".kube-deployment"+ File.separator +"config";
    }

    /**
     * docker 当前 配置
     * @return
     */
    public static String getHomeJarFile() {
        return System.getProperty("user.home") + File.separator + ".kube-deployment"+ File.separator +"jar";
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
