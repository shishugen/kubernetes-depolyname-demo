package com.c3stones.controller;

import com.c3stones.client.BaseConfig;
import com.c3stones.common.Response;
import com.c3stones.entity.Config;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.*;
import java.util.Properties;

/**
 * @ClassName: ConfigController
 * @Description: TODO 配置
 * @Author: stone
 * @Date: 2021/4/6 9:55
 */
@Controller
@Slf4j
@RequestMapping(value = "app/env/config/")
public class ConfigController {

    /**
     * 环境
     *
     * @return
     */
    @RequestMapping(value = "list")
    public String listView(Model model) {
        Properties pro = new Properties();
        InputStream inputStream = null;
        Config config = null;
        try {
            File file = new File(BaseConfig.getHomeConfigFile());
            if(file.isFile()){
                //存在
                inputStream = new FileInputStream(file);
                pro.load(inputStream);
                config = propertiesToConfig(pro);
            }else{
                ClassLoader classloader = Thread.currentThread().getContextClassLoader();
                inputStream = classloader.getResourceAsStream("application.properties");
                pro.load(inputStream);
                config = propertiesToConfig(pro);
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
        model.addAttribute("config",config);
        return "pages/app/config/list";
    }

    private Config propertiesToConfig(Properties pro){
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
    }


    @RequestMapping(value = "save")
    public  String listView(Model model,Config config){
        Properties pro = new Properties();
        OutputStream outputStream = null;
        File file = null;
        try {
             file = new File(BaseConfig.getHomeConfigFile());
            if(!file.isFile()){
                file.createNewFile();
            }
            outputStream = new FileOutputStream(file);
            pro.setProperty("docker.port",config.getDockerPort().toString());
            pro.setProperty("harbor.user",config.getHarborUser());
            pro.setProperty("harbor.password",config.getHarborPassword());
            pro.setProperty("harbor.url",config.getHarborUrl());
            pro.setProperty("harbor.image.prefix",config.getHarborImagePrefix());
            pro.setProperty("harbor.image.project.name",config.getHarborImageProjectName());
            pro.setProperty("harbor.image.env.project.name",config.getHarborImageEnvProjectName());
            pro.setProperty("nfs.storage.className",config.getNfsStorageClassName());

            pro.setProperty("nfs.storage.MySql.size",config.getNfsMySqlStorageSize());
            pro.setProperty("nfs.storage.nacos.size",config.getNfsNacosStorageSize());
            pro.setProperty("nfs.storage.mq.size",config.getNfsMqStorageSize());
            pro.setProperty("nfs.storage.fdfs.size",config.getNfsFdfsStorageSize());


            pro.store(outputStream,"保存");
            BaseConfig.setConfig(pro);
        } catch (Exception e) {
            e.printStackTrace();
            if(file.isFile()){
                file.delete();
            }
        }finally {
            if(outputStream != null){
                try {
                    outputStream.flush();
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
        System.out.println(config);
        model.addAttribute("config",config);
        return "pages/app/config/list";
    }







}
