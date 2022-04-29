package com.c3stones.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.c3stones.client.BaseConfig;
import com.c3stones.client.Kubes;
import com.c3stones.common.Response;
import com.c3stones.entity.HarborImage;
import com.c3stones.entity.Pages;
import com.c3stones.entity.Pvc;
import com.c3stones.exception.KubernetesException;
import com.c3stones.file.PodFile;
import com.c3stones.util.KubeUtils;
import com.c3stones.util.XYFileUtils;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimList;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @ClassName: SecretController
 * @Description: TODO
 * @Author: stone
 * @Date: 2022/1/10 16:24
 */
@Controller
@Slf4j
@RequestMapping(value = "nfsJar")
public class NfsJarController  extends BaseConfig {

    private final static String JAR_PATH ="/home/";

    @Autowired
    private Kubes kubes;

    @Autowired
    private PodFile podFile;

    /**
     * 批量
     *
     * @return
     */
    @RequestMapping(value = "list")
    public String list() {
        return "pages/nfsJar/list";
    }

    /**
     * 批量
     *
     * @return
     */
    @RequestMapping(value = "add")
    public String add() {
        return "pages/nfsJar/add";
    }

    @RequestMapping(value = "file")
    public String file(String namespace, String podName, Model model) {
            model.addAttribute("namespace",namespace);
            model.addAttribute("podName",podName);
        return "pages/nfsJar/file";
    }
      /**
     * 添加
     *
     * @return
     */
    @RequestMapping(value = "addNfs")
    @ResponseBody
    public Response addNfs(String namespace,String name,Integer nfsSize) {
        try {
             kubes.createPVC(namespace + "-" + name, namespace, PVC_LIBS_LABEL, nfsStorageClassName, nfsSize);
             kubes.createPod(namespace, name);
        }catch (Exception e){
            delete(namespace,name);
            e.printStackTrace();
        }
       return Response.success("OK");
    }

    @RequestMapping(value = "createPod")
    @ResponseBody
    public Response createPod(String namespace,String name) {
        try {
             kubes.createPod(namespace, name);
        }catch (Exception e){
            e.printStackTrace();
            return Response.error("异常");
        }
       return Response.success("OK");
    }


    @RequestMapping(value = "findPodFile")
    @ResponseBody
    public Response<JSONArray> findPodFile( String namespace,String podName) {
        JSONArray podJsonFile = new JSONArray();
        try {
            List<String>  podFile = this.podFile.findPodFile(namespace, namespace+podName, "/home");
            podFile.forEach(fileName ->{
                JSONObject json = new JSONObject();
                json.put("title",fileName);
                json.put("id",fileName);
                podJsonFile.add(json);
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Response.success(podJsonFile);
    }

    /**
     * 上传文件
     *
     * @param file
     * @return
     */
    @RequestMapping("/upload")
    @ResponseBody
    public Response upload(MultipartFile file,String namespace,String podName ) throws Exception {
        Assert.notNull(file, "file不能为空");
        String originalFilename = "";
            try {
                originalFilename = file.getOriginalFilename();
                File file1 = XYFileUtils.multipartFileToFile(file);
                kubes.getKubeclinet().pods().inNamespace(namespace).withName(podName).file(JAR_PATH+ originalFilename).upload(file1.toPath());
            }catch ( Exception e){
                e.printStackTrace();
                return Response.successCode(originalFilename,201);
            }

        return Response.success("OK",originalFilename);
    }


    @RequestMapping(value = "deleteFile")
    @ResponseBody
    public Response deleteFile(String fileNames,String namespace,String podName) {
        String[] file = fileNames.split(",");
        String[] comArr = new String[file.length+2];
        comArr[0]= "rm";
        comArr[1]= "-rf";
       for (int i = 0; i < file.length; i++){
           comArr[i+2]=JAR_PATH+file[i];
       }
        kubes.getKubeclinet().pods().inNamespace(namespace).withName(podName).redirectingInput().exec(comArr);
        return Response.success("OK");

    }

    @RequestMapping(value = "cmd")
    @ResponseBody
    public Response cmd(String cmd,String namespace,String podName) {
        String[] file = cmd.split(",");
        String[] comArr = new String[10];
        comArr[0]= "tar";
        comArr[1]= "-zxvf";
        comArr[2]= "/jdk.tar.gz";
        comArr[3]= "-C";
        comArr[4]= "/home/";
        kubes.getKubeclinet().pods().inNamespace(namespace).withName(podName).redirectingInput().exec("tar","-zxvf","/jdk.tar.gz","-C","/home/");
        return Response.success("OK");

    }

    @RequestMapping(value = "delPod")
    @ResponseBody
    public Response delPod(String namespace,String podName) {
        kubes.getKubeclinet().pods().inNamespace(namespace).withName(podName).delete();
        return Response.success("OK");
    }

    @RequestMapping(value = "delete")
    @ResponseBody
    public Response delete(String namespace,String name) {
        kubes.deletePvc(namespace,name);
        return Response.success("OK");
    }

    @RequestMapping(value = "listData")
    @ResponseBody
    public Response<Pages> listData() {
        List<Pvc> list = new ArrayList<>();
        KubernetesClient kubeclinet = kubes.getKubeclinet();
        PersistentVolumeClaimList list1 = kubeclinet.persistentVolumeClaims().withLabel(PVC_LIBS_LABEL).list();
        List<PersistentVolumeClaim> items1 = list1.getItems();
        for (PersistentVolumeClaim pvc:items1 ) {
            ObjectMeta metadata = pvc.getMetadata();
            String name = metadata.getName();
                String namespace = metadata.getNamespace();
                Quantity storage = pvc.getSpec().getResources().getRequests().get("storage");
                String sto = storage.getAmount() + storage.getFormat();
            String phase = pvc.getStatus().getPhase();
            Pvc pvc1 = new Pvc(sto,name,namespace,KubeUtils.StringFormatDate(metadata.getCreationTimestamp()),phase);
                list.add(pvc1);
        }
        Pages page = new Pages();
        page.setRecords(list);
        page.setTotal(list.size());
        return Response.success(page);
    }

    @RequestMapping(value = "getByNamespace")
    @ResponseBody
    public Response<List<Pvc>> getByNamespace(String namespace) {
        List<Pvc> list = new ArrayList<>();
        KubernetesClient kubeclinet = kubes.getKubeclinet();
        PersistentVolumeClaimList list1 = kubeclinet.persistentVolumeClaims().inNamespace(namespace).withLabel(PVC_LIBS_LABEL).list();
        List<PersistentVolumeClaim> items1 = list1.getItems();
        for (PersistentVolumeClaim pvc:items1 ) {
            ObjectMeta metadata = pvc.getMetadata();
            String name = metadata.getName();
                Quantity storage = pvc.getSpec().getResources().getRequests().get("storage");
                String sto = storage.getAmount() + storage.getFormat();
            String phase = pvc.getStatus().getPhase();
            Pvc pvc1 = new Pvc(sto,name,namespace,KubeUtils.StringFormatDate(metadata.getCreationTimestamp()),phase);
                list.add(pvc1);
        }
        return Response.success(list);
    }




    @RequestMapping(value = "findList")
    @ResponseBody
    public Response findList() {
        List<Pvc> list = new ArrayList<>();
        KubernetesClient kubeclinet = kubes.getKubeclinet();
        PersistentVolumeClaimList list1 = kubeclinet.persistentVolumeClaims().withLabel(PVC_LIBS_LABEL).list();
        List<PersistentVolumeClaim> items1 = list1.getItems();
        for (PersistentVolumeClaim pvc:items1 ) {
            ObjectMeta metadata = pvc.getMetadata();
            String name = metadata.getName();
            String namespace = metadata.getNamespace();
            Quantity storage = pvc.getSpec().getResources().getRequests().get("storage");
            String sto = storage.getAmount() + storage.getFormat();
            String phase = pvc.getStatus().getPhase();
            Pvc pvc1 = new Pvc(sto,name,namespace,KubeUtils.StringFormatDate(metadata.getCreationTimestamp()),phase);
            list.add(pvc1);
        }
        return Response.success(list);
    }


    /**
     * pod
     *
     * @return
     */
    @RequestMapping(value = "upload/file")
    @ResponseBody
    public Response deployNginx(String host,Integer port,String username,String pass,
                                         MultipartFile file) {
        FTPClient fc = null;
        Map<String,String> map= new HashMap<>();
        try {
            System.out.println("host="+host);
            System.out.println("port="+port);
            map.put("地址",host);
            map.put("端口",port+"");
            map.put("文件名",file.getOriginalFilename());
            map.put("username",username);
            map.put("pass",pass);
            //创建ftp客户端
            fc = new FTPClient();
            //设置连接地址和端口
            // fc.connect("10.49.0.12", 30021);
            //  fc.connect("139.9.46.153", 30024);
            fc.connect(host, port);
            //  fc.connect("139.9.46.153", 30021);
            //设置用户和密码
            boolean login = fc.login(username, pass);
            //设置文件类型
            fc.setFileType(FTP.BINARY_FILE_TYPE);
            System.out.println("login=="+login);
            if (login){
                map.put("登录状态","成功");
            }else{
                map.put("登录状态","失败");
            }
            //上传
           // boolean flag = fc.storeFile("test3377.jpg", new FileInputStream(new File("E:/test.jpg")));
            boolean flag = fc.storeFile(file.getOriginalFilename(), file.getInputStream());
            //  fc.enterLocalPassiveMode();
            // boolean b = fc.deleteFile("test.jpg");
            FTPFile[] ftpFiles = fc.listFiles();
            System.out.println(ftpFiles.length);
            if (flag){
                System.out.println("上传成功...");
                map.put("上传状态","上传成功");
            }
            else{
                map.put("上传状态","上传失败");
                System.out.println("上传失败...");
            }
        }catch (Exception e){
            e.printStackTrace();
            if (e.getMessage().indexOf("Connection refused: connect") != -1){
                map.put("登录状态","失败");
                return Response.success(map);
            }
        }
        return Response.success(map);
    }



}
