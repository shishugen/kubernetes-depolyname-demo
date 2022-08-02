package com.c3stones.entity;

import lombok.Data;
import lombok.ToString;

/**
 * @ClassName: Config
 * @Description: TODO
 * @Author: stone
 * @Date: 2021/4/6 14:14
 */
@Data
@ToString
public class Config {

    private Integer dockerPort ;
    private String harborUser ;
    private String harborPassword ;
    private String harborUrl;
    private String harborImagePrefix ;
    private String harborImageProjectName ;
    private String harborImageEnvProjectName ;
    private String nfsStorageClassName;

    private String nfsMySqlStorageSize ;
    private String nfsNacosStorageSize ;
    private String nfsMqStorageSize ;
    private String nfsFdfsStorageSize ;
    private String nfsNeo4jStorageSize ;
    private String pythonRely ;
    /**
     * 集群外网IP
     */
    private String bindK8sIP ;

    private String defaultNamespace ;
    /**
     * K8S群集端口
     */
    private String k8sNetPort;

    /**
     * K8S群集端口 子节点
     */
    private String k8sNetNodePort ;


    /**
     * 邮件地址
     */
    private String mailHost ;

    /**
     * 邮件端口
     */
    private Integer mailPort ;

    private String mailUser ;

    private String mailPass;

    private String mailPerson;

    private String checkHarborIp;

    private String checkNfsIp;

    /**
     * 主题
     */
    private String mailSubject;

    /**
     * 检查命名空间
     */
    private String checkNamespaces;


    public Config(Integer dockerPort, String harborUser, String harborPassword, String harborUrl,
                  String harborImagePrefix, String harborImageProjectName, String harborImageEnvProjectName,
                  String nfsStorageClassName, String nfsMySqlStorageSize, String nfsNacosStorageSize,
                  String nfsMqStorageSize, String nfsFdfsStorageSize,String nfsNeo4jStorageSize,
                  String pythonRely,String bindK8sIP,String defaultNamespace,String k8sNetPort,
                  String k8sNetNodePort,String mailHost,Integer mailPort,String mailUser,String mailPass,String
                          mailPerson,String mailSubject,String checkHarborIp,String checkNfsIp,String checkNamespaces) {
        this.dockerPort = dockerPort;
        this.harborUser = harborUser;
        this.harborPassword = harborPassword;
        this.harborUrl = harborUrl;
        this.harborImagePrefix = harborImagePrefix;
        this.harborImageProjectName = harborImageProjectName;
        this.harborImageEnvProjectName = harborImageEnvProjectName;
        this.nfsStorageClassName = nfsStorageClassName;
        this.nfsMySqlStorageSize = nfsMySqlStorageSize;
        this.nfsNacosStorageSize = nfsNacosStorageSize;
        this.nfsMqStorageSize = nfsMqStorageSize;
        this.nfsFdfsStorageSize = nfsFdfsStorageSize;
        this.nfsNeo4jStorageSize = nfsNeo4jStorageSize;
        this.pythonRely = pythonRely;
        this.bindK8sIP = bindK8sIP;
        this.defaultNamespace = defaultNamespace;
        this.k8sNetPort = k8sNetPort;
        this.k8sNetNodePort = k8sNetNodePort;
        this.mailHost = mailHost;
        this.mailUser = mailUser;
        this.mailPort = mailPort;
        this.mailPass = mailPass;
        this.mailPerson = mailPerson;
        this.mailSubject = mailSubject;
        this.checkHarborIp = checkHarborIp;
        this.checkNfsIp = checkNfsIp;
        this.checkNamespaces = checkNamespaces;
    }
}
