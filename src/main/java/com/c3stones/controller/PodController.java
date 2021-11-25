package com.c3stones.controller;

import cn.hutool.db.Page;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.c3stones.client.BaseConfig;
import com.c3stones.client.Kubes;
import com.c3stones.client.pod.MySQLPod;
import com.c3stones.client.pod.NacosPod;
import com.c3stones.client.pod.RabbitMQPod;
import com.c3stones.client.pod.RedisPod;
import com.c3stones.common.Response;
import com.c3stones.entity.HarborImage;
import com.c3stones.entity.Pages;
import com.c3stones.entity.Pods;
import com.c3stones.http.HttpHarbor;
import com.c3stones.util.KubeUtils;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.LogWatch;
import io.fabric8.kubernetes.client.dsl.PodResource;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @ClassName: PodController
 * @Description: TODO
 * @Author: stone
 * @Date: 2021/1/27 10:00
 */
@Controller
@Slf4j
@RequestMapping(value = "pod")
public class PodController extends BaseConfig{

    /***
     * labels
     */
    private  static  String LABELS_KEY = "app";

    @Value("${pod.namespace.prefix}")
    private String podNamespacePrefix;

    @Value("${pod.app.prefix}")
    private String podAppPrefix;

    @Value("${pod.env.prefix}")
    private String podEnvPrefix;

    @Value("${pod.nginx.prefix}")
    private String podNginxPrefix;


    @Autowired
    private  Kubes kubes;
    /**
     * 查询列表
     *
     * @return
     */
    @RequestMapping(value = "list")
    public String list(Model model) {
        String defaultNamespace = BaseConfig.defaultNamespace;
        model.addAttribute("defaultNamespace",defaultNamespace);
        return "pages/pod/list";
    }

    /**
     * 新增
     *
     * @return
     */
    @RequestMapping(value = "add")
    public String add() {
        return "pages/pod/add";
    }


    @RequestMapping(value = "downloadNginxConf")
    public String downloadNginxConf() {
        return "pages/pod/downloadNginxConf";
    }

    /**
     * 日志
     *
     * @return
     */
    @RequestMapping(value = "logs")
    public String logs(Model model,String namespace, String podName) {
        System.out.println(namespace);
        System.out.println(podName);
        model.addAttribute("namespace",namespace);
        model.addAttribute("podName",podName);
        return "pages/pod/logs";
    }
    @RequestMapping(value = "getNginxPod")
    @ResponseBody
    public List<Pods> getNginxPod() {
        List<Pods> podsList = new ArrayList<>();
        List<Pod> podList =  kubes.findPod();
        for (Pod pod:podList){
            Pods pods = new Pods();
            if (pod.getMetadata().getNamespace().startsWith(podNamespacePrefix)
                    && pod.getMetadata().getName().startsWith(podNginxPrefix)){
                podsList.add(podConverEntity(pods,pod,pod.getMetadata().getName()));
            }
        }
        return podsList;
    }

    @RequestMapping(value = "isEnable/downloadJar")
    @ResponseBody
    public boolean isEnable() {
        boolean isEnable = false;
        String jarDownload = System.getProperty("jarDownload");
        if (StringUtils.isNotBlank(jarDownload)
                &&jarDownload.equals("true")){
             isEnable = true;
        }
        return isEnable;
    }




    @RequestMapping(value = "downloadLogs")
    public void downloadLogs( String namespace, String podName, HttpServletResponse response ,HttpServletRequest request ) {
       if (!isEnable()){
           return;
       }
        String logs = kubes.getKubeclinet().pods().inNamespace(namespace)
                .withName(podName).getLog();
        response.setContentType("text/plain");
        String filename = request.getParameter("filename");
        response.setHeader("Content-disposition", "attachment; filename="+podName+".log");
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        try {
            bis = new BufferedInputStream(new ByteArrayInputStream(logs.getBytes("utf-8")));
            bos = new BufferedOutputStream(response.getOutputStream());
            byte[] buff = new byte[logs.length()];
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

    @SneakyThrows
    @RequestMapping(value = "downloadpodNginx")
    public void downloadpodNginx( String podNginx, HttpServletResponse response ,HttpServletRequest request ) {
        String[] split = podNginx.split("----");
        String podName = split[0];
        String namespace = split[1];
        InputStream read = kubes.getKubeclinet().pods().inNamespace(namespace)
                .withName(podName).file("/home/nginx.conf").read();
        response.setContentType("application/octet-stream");
        String filename = request.getParameter("filename");
        response.setHeader("Content-disposition", "attachment; filename=" +new String((podName+".conf").getBytes("gb2312"),"ISO8859-1"));
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        try {
           // bis = new BufferedInputStream(new ByteArrayInputStream(logs.getBytes("utf-8")));
            bos = new BufferedOutputStream(response.getOutputStream());
            byte[] buff = new byte[1024];
            int len = 0;
            while((len = read.read(buff)) != -1) {
                bos.write(buff, 0, len);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(read != null){
                read.close();
            }
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

    @SneakyThrows
    @RequestMapping(value = "downloadJar")
    public void downloadJar(String namespace, String podName, HttpServletResponse response ,HttpServletRequest request ) {
        InputStream read = kubes.getKubeclinet().pods().inNamespace(namespace)
                .withName(podName).file("/app.jar").read();
        response.setContentType("application/octet-stream");
        String filename = request.getParameter("filename");
        response.setHeader("Content-disposition", "attachment; filename=" +new String((podName+".jar").getBytes("gb2312"),"ISO8859-1"));
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        try {
            bos = new BufferedOutputStream(response.getOutputStream());
            byte[] buff = new byte[1024];
            int len = 0;
            while((len = read.read(buff)) != -1) {
                bos.write(buff, 0, len);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(read != null){
                read.close();
            }
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

    @RequestMapping(value = "listAppData")
    @ResponseBody
    public Response<Pages<Pods>> listAppData(String namespace,String podName ,Integer limit,Integer page) {
        if (StringUtils.isBlank(namespace)){
            namespace= BaseConfig.defaultNamespace;
        }
        List<Pods> podsList = new ArrayList<>();
            List<Pod> podList =  kubes.findPod(namespace);
            for (Pod pod:podList){
                Pods pods = new Pods();
                if (pod.getMetadata().getNamespace().startsWith(podNamespacePrefix)
                        && pod.getMetadata().getName().startsWith(podAppPrefix)
                      ){
                    if(StringUtils.isNotBlank(podName)&&pod.getMetadata().getName().startsWith(podName)){
                       podsList.add(podConverEntity2(pods,pod,pod.getMetadata().getLabels().get(LABELS_KEY)));
                    }
                    if(StringUtils.isBlank(podName)){
                       podsList.add(podConverEntity2(pods,pod,pod.getMetadata().getLabels().get(LABELS_KEY)));
                    }
                }
            }
        Pages pages= new Pages();
        pages.setRecords(super.setPage(podsList,limit,page));
        pages.setTotal(podsList.size());
        return Response.success(pages);
    }

    @RequestMapping(value = "listEnvData")
    @ResponseBody
    public Response<Pages<Pods>> listEnvData(String namespace,Integer limit,Integer page) {
        if (StringUtils.isBlank(namespace)){
            namespace= BaseConfig.defaultNamespace;
        }
        List<Pods> podsList = new ArrayList<>();
            List<Pod> podList =  kubes.findPod(namespace);
            for (Pod pod:podList){
                Pods pods = new Pods();
                if (pod.getMetadata().getNamespace().startsWith(podNamespacePrefix)
                        && pod.getMetadata().getName().startsWith(podEnvPrefix)
                        && pod.getMetadata().getLabels() != null){
                        String app = pod.getMetadata().getLabels().get(LABELS_KEY);
                        podsList.add(podConverEntity(pods,pod,app));
                }
            }
        Pages pages= new Pages();
        pages.setRecords(super.setPage(podsList,limit,page));
        pages.setTotal(podsList.size());
        return Response.success(pages);
    }

    @RequestMapping(value = "listNginxData")
    @ResponseBody
    public Response<Pages<Pods>> listNginxData(String namespace,Integer limit,Integer page) {
        if (StringUtils.isBlank(namespace)){
            namespace= BaseConfig.defaultNamespace;
        }
        List<Pods> podsList = new ArrayList<>();
            List<Pod> podList =  kubes.findPod(namespace);
            for (Pod pod:podList){
                Pods pods = new Pods();
                if (pod.getMetadata().getNamespace().startsWith(podNamespacePrefix)
                        && pod.getMetadata().getName().startsWith(podNginxPrefix)){
                    podsList.add(podConverEntity2(pods,pod,pod.getMetadata().getLabels().get(LABELS_KEY)));
                }
            }
        Pages pages= new Pages();
        pages.setRecords(super.setPage(podsList,limit,page));
        pages.setTotal(podsList.size());
        return Response.success(pages);
    }


    /**
     * api pod转换
     * @param pod
     * @param kubePod
     * @return
     */
    public  Pods podConverEntity2(Pods pod , io.fabric8.kubernetes.api.model.Pod kubePod,String svcName){

        PodStatus status = kubePod.getStatus();
        ObjectMeta metadata = kubePod.getMetadata();
        PodSpec podSpec = kubePod.getSpec();
        String hostIp = status.getHostIP();
        pod.setPodIp(status.getPodIP());
        pod.setHostIp(hostIp);
        pod.setPodName(metadata.getName());
        pod.setPodStatus(getPodStatus(kubePod).getReason());

        pod.setNamespace(metadata.getNamespace());
        pod.setDate(KubeUtils.StringFormatDate(status.getStartTime()));

        ///metadata.getLabels().get("app");
        Container container = podSpec.getContainers().get(0);
        String image = container.getImage();

        String split[] =image.split("/");
        String images = split[split.length - 1];
        String[] split1 = images.split(":");
        pod.setImages(podAppPrefix+split1[0]);
        List<ContainerPort> ports1 = container.getPorts();

        if(ports1 != null && ports1.size() > 0 ){
            Service service = kubes.findService(metadata.getNamespace(),svcName);
            if(service != null) {
                ServiceSpec spec = service.getSpec();
                List<ServicePort> ports = spec.getPorts();
                if (ports != null && ports.size() > 0) {
                    StringBuffer buffer = new StringBuffer(ports.size());
                    for (ServicePort port : ports) {
                        Integer port1 = port.getPort();
                        Integer nodePort = port.getNodePort();
                        if (buffer.toString().length() > 0) {
                            buffer.append(",");
                        }
                        buffer.append(port1 + ":" + nodePort);
                    }
                    pod.setPorts(buffer.toString());
                }
            }
        }
        return pod;
    }



    /**
     * api pod转换
     * @param pod
     * @param kubePod
     * @return
     */
    public  Pods podConverEntity(Pods pod , io.fabric8.kubernetes.api.model.Pod kubePod,String svcName){

        PodStatus status = kubePod.getStatus();
        ObjectMeta metadata = kubePod.getMetadata();
        PodSpec podSpec = kubePod.getSpec();
        String hostIp = status.getHostIP();
        pod.setPodIp(status.getPodIP());
        pod.setHostIp(hostIp);
        pod.setPodName(metadata.getName());
        pod.setPodStatus(getPodStatus(kubePod).getReason());

        pod.setNamespace(metadata.getNamespace());
        pod.setDate(KubeUtils.StringFormatDate(status.getStartTime()));

        ///metadata.getLabels().get("app");
        Container container = podSpec.getContainers().get(0);
        String image = container.getImage();

        String split[] =image.split("/");
        String images = split[split.length - 1];
        String[] split1 = images.split(":");
        pod.setImages(podAppPrefix+split1[0]);
        List<ContainerPort> ports1 = container.getPorts();

        if(ports1 != null && ports1.size() > 0 ){
        Service service = kubes.findService(metadata.getNamespace(),podEnvPrefix+svcName);
            if(service != null) {
                ServiceSpec spec = service.getSpec();
                List<ServicePort> ports = spec.getPorts();
                if (ports != null && ports.size() > 0) {
                    StringBuffer buffer = new StringBuffer(ports.size());
                    for (ServicePort port : ports) {
                        Integer port1 = port.getPort();
                        Integer nodePort = port.getNodePort();
                        if (buffer.toString().length() > 0) {
                            buffer.append(",");
                        }
                        buffer.append(port1 + ":" + nodePort);
                    }
                    pod.setPorts(buffer.toString());
                }
            }
        }
        return pod;
    }


    /**
     * 获取pod的状态
     * @param pod
     */
    public KubePodStatus getPodStatus(io.fabric8.kubernetes.api.model.Pod pod){
        PodStatus status = pod.getStatus();
        List<ContainerStatus> containerStatuses = status.getContainerStatuses();
        if(containerStatuses.size()== 0){
            List<PodCondition> conditions = status.getConditions();
            if(conditions != null && conditions.size() > 0){
                PodCondition podCondition = conditions.get(0);
                return  new KubePodStatus(podCondition.getStatus(),podCondition.getReason(), podCondition.getMessage());
            }else{
                return  new KubePodStatus("fail","fail", "未知异常！");

            }
        }
        ContainerState state = containerStatuses.get(0).getState();
        KubePodStatus podStatus = new KubePodStatus("Running","running","运行中");
        if(state.getRunning() != null){
            //运行
        }else if(state.getWaiting() != null){
            //等待
            ContainerStateWaiting waiting = state.getWaiting();
            podStatus = new KubePodStatus("waiting",waiting.getReason(),waiting.getMessage());
        }else if(state.getTerminated() != null){
            //完成
            ContainerStateTerminated terminated = state.getTerminated();
            podStatus = new KubePodStatus("terminated",terminated.getReason(),terminated.getMessage());
        }
        return podStatus;
    }


    /**
     * 状态 类
     */
    @Data
    public  class  KubePodStatus {
        private String status; //状态
        private String reason; //原因
        private String message; //信息

        public KubePodStatus(String status, String reason, String message) {
            this.status = status;
            this.reason = reason;
            this.message = message;
        }

    }
    /**
     * 删除
     *
     * @return
     */
    @RequestMapping(value = "delete")
    @ResponseBody
    public Response<Boolean> delete(String namespace ,String podName) {
        Assert.notNull(namespace, "namespace不能为空");
        Assert.notNull(podName, "podName不能为空");
        Boolean aBoolean = kubes.deletePod(namespace, podName);
            kubes.deleteService(namespace,podName);
            kubes.deleteConf(namespace,podName);
        return Response.success(true);
    }


    /** 删除
     *
     * @return
     */
    @RequestMapping(value = "delDeploy")
    @ResponseBody
    public Response<Boolean> delDeploy(String namespace ,String podName) {
        Assert.notNull(namespace, "namespace不能为空");
        Assert.notNull(podName, "podName不能为空");
        System.out.println("podName===>"+podName);
        KubernetesClient kubeclinet = kubes.getKubeclinet();
        List<Deployment> items = kubeclinet.apps().deployments().inNamespace(namespace).list().getItems();
        items.forEach(a->{
            String name = a.getMetadata().getName();
            System.out.println("name===>"+name);
            if(podName.startsWith(name)){
                Integer replicas = a.getSpec().getReplicas();
                if(replicas == 1){
                    Boolean delete = kubeclinet.apps().deployments().inNamespace(namespace).withName(name).delete();
                    kubes.deleteService(namespace,a.getSpec().getTemplate().getMetadata().getLabels().get(LABELS_KEY));
                    System.out.println("delete==>"+delete);
                }else{
                    kubeclinet.apps().deployments().inNamespace(namespace).withName(name).edit()
                            .editSpec().withReplicas(replicas-1)
                            .endSpec().done();
                }
            }
        });
        return Response.success(true);
    }

    /** 删除
     *
     * @return
     */
    @RequestMapping(value = "delDeploys")
    @ResponseBody
    public Response<Boolean> delDeploys(String  names,String namespaces) {
        String[] name = names.split(",");
        String[] namespace = namespaces.split(",");
        KubernetesClient kubeclinet = kubes.getKubeclinet();
        for (int i = 0 ; i < name.length; i++){
            Deployment items = kubeclinet.apps().deployments().inNamespace(namespace[i]).withName((name[i])).get();
            kubes.deleteService(namespace[i],items.getSpec().getTemplate().getMetadata().getLabels().get(LABELS_KEY));
            kubes.deletePvc(namespace[i],name[i]);
            Boolean delete = kubeclinet.apps().deployments().inNamespace(namespace[i]).withName(name[i]).delete();
        }
        return Response.success(true);
    }

    /** restartDeploys
     *
     * @return
     */
    @RequestMapping(value = "restartDeploys")
    @ResponseBody
    public Response<Boolean> restartDeploys(String  names,String namespaces) {
        String[] name = names.split(",");
        String[] namespace = namespaces.split(",");
        KubernetesClient kubeclinet = kubes.getKubeclinet();
        for (int i = 0 ; i < name.length; i++){
            Deployment items = kubeclinet.apps().deployments().inNamespace(namespace[i]).withName((name[i])).rolling().restart();
        }
        return Response.success(true);
    }



    /**
     * 重启
     * @param namespace
     * @param podName
     * @return
     */
    @RequestMapping(value = "restart")
    @ResponseBody
    public Response<Boolean> restart(String namespace ,String deployname) {
        Assert.notNull(namespace, "namespace不能为空");
        Assert.notNull(deployname, "podName不能为空");
        log.info("重启-deployname=={}",deployname);
        KubernetesClient kubeclinet = kubes.getKubeclinet();
         kubeclinet.apps().deployments().inNamespace(namespace).withName(deployname).rolling().restart();
        return Response.success(true);
    }

    /**
     *
     *
     * @param namespace
     * @param podName
     * @return
     */
    @RequestMapping(value = "rollback")
    @ResponseBody
    public Response<Boolean> rollback(String namespace ,String deployname) {
        Assert.notNull(namespace, "namespace不能为空");
        Assert.notNull(deployname, "podName不能为空");
        log.info("重启-deployname=={}",deployname);
        KubernetesClient kubeclinet = kubes.getKubeclinet();
         kubeclinet.apps().deployments().inNamespace(namespace).withName(deployname).rolling().undo();
        return Response.success(true);
    }

}
