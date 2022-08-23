package com.c3stones.controller;

import com.c3stones.client.BaseConfig;
import com.c3stones.client.Kubes;
import com.c3stones.client.pod.*;
import com.c3stones.common.Response;
import com.c3stones.entity.HarborImage;
import com.c3stones.entity.Pages;
import com.c3stones.entity.PodParameter;
import com.c3stones.exception.KubernetesException;
import com.c3stones.file.PodFile;
import com.c3stones.http.HttpHarbor;
import com.c3stones.util.OpenFileUtils;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.DoneableConfigMap;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @ClassName: EnvController
 * @Description: TODO
 * @Author: stone
 * @Date: 2021/1/27 10:00
 */
@Controller
@RequestMapping(value = "env")
public class EnvController extends BaseConfig {

    @Autowired
    private HttpHarbor httpHarbor ;

    @Autowired
    private Kubes kubes ;

    @Autowired
    private MySQLPod mySQLPod ;

    @Autowired
    private NacosPod nacosPod ;

    @Autowired
    private Neo4JPod neo4JPod ;

    @Autowired
    private KKfileViewPod kKfileViewPod ;

    @Autowired
    private RabbitMQPod rabbitMQPod ;

    @Autowired
    private FastdfsPod fastdfsPod ;

    @Autowired
    private SeataPod2 seataPod ;

    @Autowired
    private MySQLBackPod mySQLBackPod ;

    @Autowired
    private PodFile podFile ;

    @Autowired
    private RedisPod redisPod ;
    @Autowired
    private VsFtpPod vsFtpPod;
    @Autowired
    private LibreofficePod libreofficePod ;

    @Autowired
    private GuacdPod guacdPod ;

    @Value("${pod.namespace.prefix}")
    private String podNamespacePrefix;
    @Value("${pod.env.prefix}")
    private String podEnvPrefix;

    /***
     * labels
     */
    private  static  String LABELS_KEY = "app";


    /**
     * 查询列表
     *
     * @return
     */
    @RequestMapping(value = "list")
    public String list(Model model) {
        String defaultNamespace = BaseConfig.defaultNamespace;
        model.addAttribute("defaultNamespace",defaultNamespace);
        return "pages/env/list";
    }

    /**
     * 新增
     *
     * @return
     */
    @RequestMapping(value = "add")
    public String add() {
        return "pages/env/add";
    }
    /**
     * 新增
     *
     * @return
     */
    @RequestMapping(value = "addSeata")
    public String addSeata() {
        return "pages/env/addSeata";
    }
    /**
     * 新增
     *
     * @return
     */
    @RequestMapping(value = "addMySQLBack")
    public String addMySQLBack() {
        return "pages/env/addMySQLBack";
    }

     @RequestMapping(value = "download")
    public String download() {
        return "pages/env/download";
    }

    @RequestMapping(value = "updateNginxConfig")
    public String updateNginxConfig(String namespace,String name,Model model) {
        model.addAttribute("namespace",namespace);
        model.addAttribute("name",name);
        return "pages/pod/updateNginxConfig";
    }
    /**
     * 新增
     *
     * @return
     */
    @RequestMapping(value = "reMySQL")
    public String reMySQL() {
        return "pages/env/reMySQL";
    }
    /**
     * 新增
     *
     * @return
     */
    @RequestMapping(value = "localReMySQL")
    public String localReMySQL() {
        return "pages/env/localReMySQL";
    }
    /**
     * 批量
     *
     * @return
     */
    @RequestMapping(value = "addMulti")
    public String addMulti() {
        return "pages/env/addMulti";
    }


    @RequestMapping(value = "listData")
    @ResponseBody
    public Response<Pages<HarborImage>> listData() {
        List<HarborImage> harborList = httpHarbor.harborList(harborImageEnvProjectName,null,false);
        Pages page = new Pages();
        page.setRecords(harborList);
        page.setTotal(harborList.size());
        return Response.success(page);
    }

    @RequestMapping(value = "findPodFile")
    @ResponseBody
    public Response<List<String>> findPodFile(
            String namespace,String podName
    ) {
        List<String> podFile = null;
        try {
            podFile = this.podFile.findPodFile(namespace, podName, "/db");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Response.success(podFile);
    }


    /**
     *
     * @param name
     * @return
     */
    @RequestMapping(value = "create")
    @ResponseBody
    public Response<Boolean> create(String name,String namespace,Integer nodePort,String guacamoleName,
                                    Integer MySQLnodePort,Integer nacosNodePort,Integer kkfileviewPort) {
        System.out.println("创建 名称为 : "+name+namespace);
        Assert.notNull(namespace, "name不能为空");
        Assert.notNull(name, "name不能为空");
        if(kubes.checkSvc(nodePort)){
            return Response.error("端口已存在"+nodePort);
        }
        if(kubes.checkSvc(MySQLnodePort)){
            return Response.error("端口已存在"+MySQLnodePort);
        }
        if(kubes.checkSvc(nacosNodePort)){
            return Response.error("端口已存在"+nacosNodePort);
        }
        if(kubes.checkSvc(kkfileviewPort)){
            return Response.error("端口已存在"+kkfileviewPort);
        }
        try {


        }catch (Exception e){
            e.printStackTrace();
            kubes.getKubeclinet().pods().inNamespace(namespace).withName(podEnvPrefix+name).delete();
            kubes.getKubeclinet().services().inNamespace(namespace).withName(podEnvPrefix+name).delete();
        }

        return Response.success(true);
    }


    /**
     *
     * @param name
     * @return
     */
    @RequestMapping(value = "createSeata")
    @ResponseBody
    public Response<Boolean> createSeata(PodParameter podParameter) {
        Assert.notNull(podParameter.getNamespace(), "name不能为空");
        Assert.notNull(podParameter.getName(), "name不能为空");
        try {
            seataPod.createSeata(podParameter);
        }catch (Exception e){
            e.printStackTrace();
          //  kubes.getKubeclinet().pods().inNamespace(namespace).withName(podEnvPrefix+name).delete();
           // kubes.getKubeclinet().services().inNamespace(namespace).withName(podEnvPrefix+name).delete();
        }
        return Response.success(true);
    }

    /**
     *
     * @param name
     * @return
     */
    @RequestMapping(value = "createMySQLBack")
    @ResponseBody
    public Response<Boolean> createMySQLBack(PodParameter podParameter) {
        System.out.println("podParameter==="+podParameter.getNfsPath());
        Assert.notNull(podParameter.getNamespace(), "name不能为空");
        try {
            mySQLBackPod.create(podParameter);
        }catch (KubernetesException e){
            return Response.error(e.getMessage());
        }catch (Exception e){
            e.printStackTrace();
            return Response.error("失败");
        }
        return Response.success(true);
    }    /**
     *
     * @param name
     * @return
     */
    @RequestMapping(value = "reMySQLBack")
    @ResponseBody
    public Response<Boolean> reMySQLBack(PodParameter podParameter) {
        Assert.notNull(podParameter.getNamespace(), "name不能为空");
        try {
            mySQLBackPod.reMySQLBack(podParameter);
        }catch (Exception e){
            e.printStackTrace();
        }
        return Response.success(true);
    }
    @RequestMapping(value = "localReMySQLBack")
    @ResponseBody
    public Response<Boolean> localReMySQLBack( PodParameter podParameter  ) {
        try {
            podParameter.setPodName("remysql2");
            mySQLBackPod.localReMySQLBack(podParameter);
        }catch (Exception e){
            e.printStackTrace();
            return Response.error("失败");
        }
        return Response.success(true);
    }
    /**
     *
     * @param name
     * @return
     */
    @RequestMapping(value = "downloadFile")
    public void downloadFile(String namespace, String podName,String fileName, HttpServletResponse response , HttpServletRequest request ) {
        Assert.notNull(namespace, "name不能为空");
        try {
            podFile.downloadFile(namespace,podName,"/db",fileName,response,request);
        }catch (Exception e){
            e.printStackTrace();
        }

    }


    /**
     *
     * @param name
     * @return
     */
    @RequestMapping(value = "createMulti")
    @ResponseBody
    public Response<Boolean> createMulti(String envList , String namespace,Integer fdfsPort,String guacamoleName,
                                         Integer mysqlNodePort,Integer nacosNodePort,Integer kkfileviewPort,
                                         Integer isUpdateImage,String kkfileviewHttps,Integer vsftpdPort,Integer vsftpdNginxPort,
                                         Integer vsftpdSize ,PodParameter podParameter) {
        Assert.notNull(namespace, "name不能为空");
        Assert.notNull(envList, "envList不能为空");
        boolean isAnew = isUpdateImage == 1 ? true : false;
        if(kubes.checkSvc(fdfsPort)){
            return Response.error("端口已存在"+fdfsPort);
        }
        if(kubes.checkSvc(mysqlNodePort)){
            return Response.error("端口已存在"+mysqlNodePort);
        }
        if(kubes.checkSvc(nacosNodePort)){
            return Response.error("端口已存在"+nacosNodePort);
        }
        if(kubes.checkSvc(vsftpdPort)){
            return Response.error("端口已存在"+vsftpdPort);
        }
        if(kubes.checkSvc(vsftpdNginxPort)){
            return Response.error("端口已存在"+vsftpdNginxPort);
        }
        if(kubes.checkSvc(vsftpdNginxPort)){
            return Response.error("端口已存在"+podParameter.getNeo4jNodePort());
        }
        Arrays.stream(envList.split(",")).forEach(name->{
            switch (name){
                case "mysql":
                    mySQLPod.createMySQL(namespace,mysqlNodePort,isAnew);
                    break;
                case "nacos":
                    nacosPod.createNacos(namespace,nacosNodePort,isAnew);
                    break;
                case "redis":
                    redisPod.createRedis(namespace,isAnew);
                    break;
                case "rabbitMq":
                    rabbitMQPod.createRabbitmq(namespace,isAnew);
                    break;
                case "fdfs":
                    fastdfsPod.createT(namespace,fdfsPort,isAnew);
                    break;
                case "libreoffice":
                    libreofficePod.createlibreoffice(namespace,isAnew);
                    break;
                case "guacamole":
                    guacdPod.createGuacamole(namespace,guacamoleName,isAnew);
                    break;
                case "neo4j":
                    neo4JPod.create(namespace,isAnew,podParameter.getNeo4jNodePort());
                    break;
                case "kkfileview":
                    kKfileViewPod.create(namespace,kkfileviewPort,isAnew,kkfileviewHttps,podParameter);
                    break;
                case "vsftpd":
                    if (vsftpdSize > 500){
                        System.err.println("存储不能大于500G");
                    }else{
                        vsFtpPod.create(namespace,vsftpdPort,vsftpdNginxPort,vsftpdSize,isAnew);
                    }
                    break;
            }
        });

        return Response.success(true);
    }

    /** 删除
     *
     * @return
     */
    @RequestMapping(value = "delDeploy")
    @ResponseBody
    public Response<Boolean> delDeploys(String  name,String namespace,String configName) {
        KubernetesClient kubeclinet = kubes.getKubeclinet();
            Boolean delete = kubeclinet.apps().deployments().inNamespace(namespace).withName(name).delete();
            kubes.deleteService(namespace,name);
            kubes.deleteConf(namespace,configName);
            if ("seata".equals(configName)){
                kubes.deleteConf(namespace,configName+"2");
            }
        return Response.success(true);
    }
    /** 删除
     *
     * @return
     */
    @RequestMapping(value = "updateComfig")
    @ResponseBody
    public Response<String> updateComfig(String name,String namespace) {
        KubernetesClient kubeclinet = kubes.getKubeclinet();
        ConfigMap configMap = kubeclinet.configMaps().inNamespace(namespace).withName(name).get();
        Map<String, String> data = configMap.getData();
        return Response.success("OK",data.get("nginx.conf"));
    }
    /** whnt
     *
     * @return
     */
    @RequestMapping(value = "updateNginxConfigFile")
    @ResponseBody
    public Response<String> updateNginxConfigFile(String name,String namespace,String configData) {
        KubernetesClient kubeclinet = kubes.getKubeclinet();
        Map<String,String> data = new HashMap<>(1);
        data.put("nginx.conf",configData);
        kubeclinet.configMaps().inNamespace(namespace).withName(name).edit().withData(data).done();
        kubeclinet.apps().deployments().inNamespace(namespace).withName(name).rolling().restart();
        return Response.success("ok");
    }


}
