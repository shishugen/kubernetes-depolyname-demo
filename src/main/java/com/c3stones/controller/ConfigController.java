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
        model.addAttribute("config",BaseConfig.initConfig());
        return "pages/app/config/list";
    }
    /**
     * 环境   restart
     *
     * @return
     */

    @RequestMapping(value = "restart")
    @ResponseBody
    public Response<Config>   restart(Model model) {
        new File(BaseConfig.getHomeConfigFile()).delete();
      //  model.addAttribute("config",BaseConfig.initConfig());
        return Response.success(BaseConfig.initConfig());
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
            pro.setProperty("nfs.storage.neo4j.size",config.getNfsNeo4jStorageSize());
            pro.setProperty("python.rely",config.getPythonRely());
            pro.setProperty("bindK8sIP",config.getBindK8sIP());
            pro.setProperty("k8sNetPort",config.getK8sNetPort());
            pro.setProperty("k8sNetNodePort",config.getK8sNetNodePort());
            pro.setProperty("defaultNamespace",config.getDefaultNamespace());
            pro.setProperty("mail.host",config.getMailHost());
            pro.setProperty("mail.port",config.getMailPort().toString());
            pro.setProperty("mail.user",config.getMailUser());
            pro.setProperty("mail.pass",config.getMailPass());
            pro.setProperty("mail.subject",config.getMailSubject());
            pro.setProperty("mail.person",config.getMailPerson());
            pro.setProperty("check.harbor.ip",config.getCheckHarborIp());
            pro.setProperty("check.nfs.ip",config.getCheckNfsIp());
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
