package com.c3stones.controller;

import com.c3stones.client.Dockers;
import com.c3stones.client.Kubes;
import com.c3stones.common.Response;
import com.c3stones.entity.DockerConfigs;
import com.c3stones.entity.K8sConfig;
import com.c3stones.entity.Pages;
import com.c3stones.entity.SSLConfig;
import com.c3stones.util.OpenFileUtils;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
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
public class SSLController {


    /**
     * 环境
     *
     * @return
     */
    @RequestMapping(value = "ssl/list")
    public String list() {
        return "pages/app/ssl/list";
    }


    /**
     * 环境
     *
     * @return
     */
    @RequestMapping(value = "ssl/data/list")
    @ResponseBody
    public Response<Pages<K8sConfig>> k8sList() {
         List<SSLConfig> list = new ArrayList<>();
        FileInputStream stream = null;
        try {
            File files = new File(Kubes.getHomeSSLDir());
            File[] files1 = files.listFiles();
            for (File f :files1){
                SSLConfig sslConfig = new SSLConfig();
                sslConfig.setDomain(f.getName());
                list.add(sslConfig);
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
     * delete
     *
     * @return
     */
    @RequestMapping(value = "ssl/config/delete")
    @ResponseBody
    public Response<Pages<Config>> Delete(String domain) {
        try {
            File files = new File(Kubes.getHomeSSLDir());
            File[] files1 = files.listFiles();
            File filePa = new File(Kubes.getHomeSSLDir()+File.separator+domain);
            OpenFileUtils.deleteFile(filePa);
           return Response.success("删除成功");
        } catch (Exception e) {
            e.printStackTrace();
            return Response.error("删除失败");
        }
    }

    /**
     * 删除服务
     *
     * @return
     */
    @RequestMapping(value = "ssl/deleteConfigFile")
    @ResponseBody
    public Response<Boolean> deleteConfigFile(String domain,String fileName) {
        System.out.println("domain=="+domain);
        System.out.println("fileName=="+fileName);
        try {
            File filePa = new File(Kubes.getHomeSSLDir()+File.separator+domain+File.separator+fileName);
            OpenFileUtils.deleteFile(filePa);
        }catch (Exception e){
            e.printStackTrace();
            return Response.error("失败");
        }
        return Response.success("OK");
    }



    /**
     * 环境 添加
     *
     * @return
     */
    @RequestMapping(value = "ssl/addView")
    public String addConfig() {
        return "pages/app/ssl/add";
    }

    /**
     * 环境
     *
     * @return
     */
    @RequestMapping(value = "ssl/addConfigFileView")
    public String addConfigFileView(String domain , Model model) {
        model.addAttribute("domain",domain);
        return "pages/app/ssl/addConfigFile";
    }

    /**
     * 添加服务
     *
     * @return
     */
    @RequestMapping(value = "ssl/add")
    @ResponseBody
    public Response<Boolean> add( String domain) {
        System.out.println("domain=="+domain);
        try {
            File filePa = new File(Kubes.getHomeSSLDir()+File.separator+domain);
            filePa.mkdirs();
        }catch (Exception e){
            e.printStackTrace();
            return Response.error("失败");
        }
        return Response.success("OK");
    }

    /**
     * pod
     *
     * @return
     */
    @RequestMapping(value = "ssl/addConfig")
    @ResponseBody
    public Response<Boolean> addConfigFile( MultipartFile file,String domain) {
        System.out.println("file=="+file);
        System.out.println("domain=="+domain);
        try {
            File filePa = new File(Kubes.getHomeSSLDir()+File.separator+domain+File.separator+file.getOriginalFilename());
            filePa.createNewFile();
            OpenFileUtils.writeFile(file,filePa);
        }catch (Exception e){
            e.printStackTrace();
            return Response.error("失败");
        }
        return Response.success("OK");
    }

    /**
     * configFile
     *
     * @return
     */
    @RequestMapping(value = "ssl/configFile/list")
    @ResponseBody
    public Response<Pages<SSLConfig>> configFile(String domain) {
        List<SSLConfig> list = new ArrayList<>();
        try {
            String homeConfigDir = Kubes.getHomeSSLDir();
            File files = new File(homeConfigDir);
            File[] files1 = files.listFiles();
            for (File file:files1 ) {
                if(file.isDirectory() && file.getName().equals(domain)){
                    File[] files2 = file.listFiles();
                    for (File f:files2 ) {
                        SSLConfig dockerConfigs = new SSLConfig();
                        dockerConfigs.setFileName(f.getName());
                        dockerConfigs.setDomain(domain);
                        list.add(dockerConfigs);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Response.error("失败");
        }finally {
        }
        Pages page = new Pages();
        page.setRecords(list);
        page.setTotal(list.size());
        return Response.success(page);
    }

    public static void main(String[] args) {
        String str ="https://10.49.0.11:6443";
        int i = str.lastIndexOf("/");
        System.out.println(str.substring(i+1).replaceAll("\\.","-").replaceAll(":","-PORT-"));
    }
}
