package com.c3stones.controller;

import com.c3stones.client.Kubes;
import com.c3stones.common.Response;
import com.c3stones.entity.K8sConfig;
import com.c3stones.entity.Pages;
import com.c3stones.util.OpenFileUtils;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName: AppEnvController
 * @Description: TODO 应用环境
 * @Author: stone
 * @Date: 2021/4/1 9:48
 */
@Controller
@Slf4j
@RequestMapping(value = "app/env")
public class AppEnvController {


    /**
     * 环境
     *
     * @return
     */
    @RequestMapping(value = "k8s/list")
    public String list() {
        return "pages/app/k8s/list";
    }
    /**
     * 环境
     *
     * @return
     */
    @RequestMapping(value = "k8s/config/list")
    @ResponseBody
    public Response<Pages<K8sConfig>> k8sList() {
         List<K8sConfig> list = new ArrayList<>();
        FileInputStream stream = null;
        try {
            File files = new File(Kubes.getHomeConfigDir());
            File[] files1 = files.listFiles();
            for (File f :files1){
                 stream = new FileInputStream(f);
                String file = OpenFileUtils.readFile(stream);
                stream.close();
                if(StringUtils.isNotBlank(file)){
                    K8sConfig k8sConfig = new K8sConfig();
                    Config config = Config.fromKubeconfig(file);
                    File file1  = Kubes.getHomeConfigFile();
                    FileInputStream stream1 = new FileInputStream(file1);
                    String s = OpenFileUtils.readFile(stream1);
                    if(StringUtils.isNotBlank(s)){
                        Config config1 = Config.fromKubeconfig(s);
                        if(config1.getMasterUrl().equals(config.getMasterUrl())){
                            k8sConfig.setStatus("当前环境");
                        } else {
                            k8sConfig.setStatus("未启用");
                        }
                    }
                    k8sConfig.setMasterUrl(config.getMasterUrl());
                    list.add(k8sConfig);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Response.error("失败");
        }finally {
            if (stream !=null){
                try {
                    stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        Pages page = new Pages();
        page.setRecords(list);
        page.setTotal(list.size());
        return Response.success(page);
    }
    /**
     * verify
     *
     * @return
     */
    @RequestMapping(value = "k8s/config/verify")
    @ResponseBody
    public Response<Pages<Config>> k8sVerify(String masterUrl) {
        try {
         KubernetesClient kubernetesClient = new DefaultKubernetesClient(Kubes.getMasterConfig(masterUrl));
            kubernetesClient.apps().deployments().list();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.error("连接失败");
        }
        return Response.success("连接成功");
    }

    /**
     * delete
     *
     * @return
     */
    @RequestMapping(value = "k8s/config/delete")
    @ResponseBody
    public Response<Pages<Config>> k8sDelete(String masterUrl) {
        try {
            File files = new File(Kubes.getHomeConfigDir());
            File[] files1 = files.listFiles();
            for (File f :files1){
                FileInputStream  stream = new FileInputStream(f);
                String file = OpenFileUtils.readFile(stream);
                stream.close();
                if(StringUtils.isNotBlank(file)){
                    Config config = Config.fromKubeconfig(file);
                   if(config.getMasterUrl().equals(masterUrl)){
                       f.delete();
                       return Response.success("删除成功");
                   }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Response.error("删除失败");
        }
        return Response.success("删除成功");
    }
    /**
     * setEnv
     *
     * @return
     */
    @RequestMapping(value = "k8s/config/setEnv")
    @ResponseBody
    public Response<Pages<Config>> k8sSetEnv(String masterUrl) {
        try {
            Kubes.setMasterConfig(masterUrl);
        } catch (Exception e) {
            e.printStackTrace();
            return Response.error("设置失败");
        }
        return Response.success("设置成功");
    }
    /**
     * 环境 添加
     *
     * @return
     */
    @RequestMapping(value = "k8s/add")
    public String addK8sConfig() {
        return "pages/app/k8s/add";
    }

    /**
     * pod
     *
     * @return
     */
    @RequestMapping(value = "k8s/addK8sConfig")
    @ResponseBody
    public Response<Boolean> addK8sConfig( MultipartFile  file) {
        System.out.println("conf=="+file);
        try {
            //形参里面可追加true参数，表示在原有文件末尾追加信息
            String configFile = OpenFileUtils.fileConverString(file);
            Config config = Config.fromKubeconfig(configFile);
            int i = config.getMasterUrl().lastIndexOf("/");
            String s = config.getMasterUrl().substring(i + 1).replaceAll("\\.", "-").replaceAll(":", "-PORT-");
            File filePa = new File(Kubes.getHomeConfigDir()+File.separator+s);
            filePa.createNewFile();
            OpenFileUtils.writeFile(file,filePa);
        }catch (Exception e){
            e.printStackTrace();
            return Response.error("失败");
        }
        return Response.success("OK");
    }


    public static void main(String[] args) {
        String str ="https://10.49.0.11:6443";
        int i = str.lastIndexOf("/");
        System.out.println(str.substring(i+1).replaceAll("\\.","-").replaceAll(":","-PORT-"));
    }
}
