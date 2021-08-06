package com.c3stones.controller;

import com.c3stones.client.BaseConfig;
import com.c3stones.client.Kubes;
import com.c3stones.common.Response;
import com.c3stones.entity.K8sConfig;
import com.c3stones.entity.NginxConfig;
import com.c3stones.entity.Pages;
import com.c3stones.entity.Pods;
import com.c3stones.util.OpenFileUtils;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.*;

/**
 * @ClassName: AppEnvController
 * @Description: TODO 应用环境
 * @Author: stone
 * @Date: 2021/4/1 9:48
 */
@Controller
@Slf4j
@RequestMapping(value = "app/env")
public class NginxConfigController {

    @Autowired
    private Kubes kubes;

    /**
     * 环境
     *
     * @return
     */
    @RequestMapping(value = "nginx/list")
    public String list() {
        return "pages/app/nginx/list";
    }

    /**
     * 环境
     *
     * @return
     */
    @RequestMapping(value = "nginx/config/k8sHostIP")
    @ResponseBody
    public  String  k8sIP() {
        return kubes.getK8SNodeIp();

    }
    /**
     * 环境
     *
     * @return
     */
    @RequestMapping(value = "nginx/config/gatewayApi")
    @ResponseBody
    public   List<Pods>  gatewayApi() {
        List<Pod> podList = kubes.getKubeclinet().pods().withLabel(BaseConfig.GATEWAY_API_KEY, BaseConfig.GATEWAY_API_VALUE).list().getItems();
        List<Pods> podsList = new ArrayList<>();
       for (Pod pod : podList){
           String   podName = pod.getSpec().getContainers().get(0).getName();
           String   namespace = pod.getMetadata().getNamespace();
            Map<String, String> labels = pod.getMetadata().getLabels();
            Set<String> strings = labels.keySet();
            for (String key :strings ) {
                if (key.equals(BaseConfig.GATEWAY_API_KEY)){
                    String app = labels.get("app");
                    Map<String ,String> map = new HashMap<>();
                    map.put(key,labels.get(key));
                    map.put("app",app);
                    Pods pods = new Pods();
                    List<Service> items = kubes.getKubeclinet().services().withLabels(map).list().getItems();
                    for (Service a : items) {
                        pods.setPodName(podName);
                        pods.setNamespace(namespace);
                        pods.setPorts(a.getSpec().getPorts().get(0).getPort().toString());
                        pods.setServiceName(a.getMetadata().getName());
                        podsList.add(pods);
                    }
                }
            }
        }
        return podsList;
    }
    /**
     * 环境
     *
     * @return
     */
    @RequestMapping(value = "nginx/config/listAll")
    @ResponseBody
    public  List<NginxConfig> nginxList() {
        List<NginxConfig> list = new ArrayList<>();
        FileInputStream stream = null;
        try {
            File files = new File(Kubes.getHomeNginxConfigDir());
            File[] files1 = files.listFiles();
            for (File f :files1){
                stream = new FileInputStream(f);
                NginxConfig nginxConfig = new NginxConfig();
                nginxConfig.setFileName(f.getName());
                list.add(nginxConfig);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            if (stream !=null){
                try {
                    stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return list;
    }


    /**
     * 环境
     *
     * @return
     */
    @RequestMapping(value = "nginx/config/list")
    @ResponseBody
    public Response<Pages<NginxConfig>> List() {
         List<NginxConfig> list = new ArrayList<>();
        FileInputStream stream = null;
        try {
            File files = new File(Kubes.getHomeNginxConfigDir());
            File[] files1 = files.listFiles();
            for (File f :files1){
                 stream = new FileInputStream(f);
                 NginxConfig nginxConfig = new NginxConfig();
                nginxConfig.setFileName(f.getName());
                list.add(nginxConfig);
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
    @RequestMapping(value = "nginx/config/delete")
    @ResponseBody
    public Response<Pages<Config>> k8sDelete(String fileName) {
        try {
            File files = new File(Kubes.getHomeNginxConfigDir());
            File[] files1 = files.listFiles();
            for (File f :files1){
               if(f.getName().equals(fileName)){
                   System.gc();//启动jvm垃圾回收
                   f.delete();
                   return Response.success("删除成功");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Response.error("删除失败");
        }
        return Response.success("删除成功");
    }

    /**
     * 环境 添加
     *
     * @return
     */
    @RequestMapping(value = "nginx/add")
    public String addK8sConfig() {
        return "pages/app/nginx/add";
    }

    /**
     * pod
     *
     * @return
     */
    @RequestMapping(value = "nginx/addConfig")
    @ResponseBody
    public Response<Boolean> addK8sConfig( MultipartFile file ,String fileName) {
        System.out.println("conf=="+file);
        try {
            if (StringUtils.isBlank(fileName)){
                fileName = file.getOriginalFilename();
            }
            //形参里面可追加true参数，表示在原有文件末尾追加信息
           // String configFile = OpenFileUtils.fileConverString(file);
            File filePa = new File(Kubes.getHomeNginxConfigDir()+File.separator+fileName);
            filePa.createNewFile();
            OpenFileUtils.writeFile(file,filePa);
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
    @RequestMapping(value = "nginx/download")
    public void download(String fileName, HttpServletResponse response , HttpServletRequest request) {
        System.out.println("fileName=="+fileName);
        File file = new File(Kubes.getHomeNginxConfigDir()+File.separator+fileName);
        response.setContentType("text/plain");
        String filename = request.getParameter("filename");
        response.setHeader("Content-disposition", "attachment; filename="+fileName);
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        try {
            bis = new BufferedInputStream(new FileInputStream(file));
            bos = new BufferedOutputStream(response.getOutputStream());
            byte[] buff = new byte[bis.available()];
            int bytesRead = 0;
            while (-1 != (bytesRead = (bis.read(buff, 0, buff.length)))) {
                bos.write(buff, 0, buff.length);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    public static void main(String[] args) {
        String str ="https://10.49.0.11:6443";
        int i = str.lastIndexOf("/");
        System.out.println(str.substring(i+1).replaceAll("\\.","-").replaceAll(":","-PORT-"));
    }
}
