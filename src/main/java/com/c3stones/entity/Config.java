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



    public Config(Integer dockerPort, String harborUser, String harborPassword, String harborUrl,
                  String harborImagePrefix, String harborImageProjectName, String harborImageEnvProjectName,
                  String nfsStorageClassName, String nfsMySqlStorageSize, String nfsNacosStorageSize,
                  String nfsMqStorageSize, String nfsFdfsStorageSize,String nfsNeo4jStorageSize) {
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
    }
}
