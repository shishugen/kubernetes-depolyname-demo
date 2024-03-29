package com.c3stones.controller;

import com.c3stones.client.BaseConfig;
import com.c3stones.client.Kubes;
import com.c3stones.common.Response;
import com.c3stones.entity.HarborImage;
import com.c3stones.entity.Pages;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretList;
import io.fabric8.kubernetes.api.model.networking.v1beta1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1beta1.IngressRule;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
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
@RequestMapping(value = "ingress")
public class IngressController {


    @Autowired
    private Kubes kubes;

    @RequestMapping(value = "listData")
    @ResponseBody
    public Response<Pages> listData(String namespace) {
        KubernetesClient kubeclinet = kubes.getKubeclinet();
        List list = new ArrayList();
        List<Ingress> items = null;
        if (StringUtils.isNotBlank(namespace)){
            items = kubeclinet.network().ingresses().inNamespace(namespace).withLabel("app-ingress").list().getItems();
        }else{
            items = kubeclinet.network().ingresses().withLabel("app-ingress").list().getItems();
        }
      if (items != null){
          items.forEach(a->{
              List<IngressRule> rules = a.getSpec().getRules();
              IngressRule ingressRule = rules.get(0);
              list.add(ingressRule);
          });
      }
        Pages page = new Pages();
        page.setRecords(list);
        page.setTotal(list.size());
        return Response.success(page);
    }

    /**
     * 批量
     *
     * @return
     */
    @RequestMapping(value = "test/upload")
    public String addMulti() {
        return "pages/test/upload";
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
