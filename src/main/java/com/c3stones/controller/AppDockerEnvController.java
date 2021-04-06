package com.c3stones.controller;

import com.c3stones.client.Dockers;
import com.c3stones.client.Kubes;
import com.c3stones.common.Response;
import com.c3stones.entity.DockerConfigs;
import com.c3stones.entity.K8sConfig;
import com.c3stones.entity.Pages;
import com.c3stones.util.OpenFileUtils;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.model.Version;
import com.sun.corba.se.impl.encoding.WrapperInputStream;
import io.fabric8.kubernetes.client.Config;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.PropertiesUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;

/**
 * @ClassName: AppDockerEnvController
 * @Description: TODO docker 环境
 * @Author: stone
 * @Date: 2021/4/1 17:28
 */

@Controller
@Slf4j
@RequestMapping(value = "app/env/docker/")
public class AppDockerEnvController {


    @Autowired
    private Dockers dockers;
    /**
     * 环境
     *
     * @return
     */
    @RequestMapping(value = "list")
    public String list() {
        return "pages/app/docker/list";
    }
    /**
     * 环境
     *
     * @return
     */
    @RequestMapping(value = "addServerView")
    public String addServerView() {
        return "pages/app/docker/addServer";
    }
    /**
     * 环境
     *
     * @return
     */
    @RequestMapping(value = "addConfigFileView")
    public String addConfigFileView(String serverIp , Model model) {
         model.addAttribute("serverIp",serverIp);
        return "pages/app/docker/addConfigFile";
    }




    /**
     * 环境
     *
     * @return
     */
    @RequestMapping(value = "data/list")
    @ResponseBody
    public Response<Pages<DockerConfigs>> List() {
        TestSetProperties( new Random().nextInt()+"");
        List<DockerConfigs> list = new ArrayList<>();
        try {
            String homeConfigDir = Dockers.getHomeConfigDir();
            File files = new File(homeConfigDir);
            File[] files1 = files.listFiles();
            for (File file:files1 ) {
                DockerConfigs dockerConfigs = new DockerConfigs();
                if(file.isDirectory()){
                     File currentServer=  new File(Dockers.getHomeConfigFile());
                    File[] files2 = currentServer.listFiles();
                    dockerConfigs.setStatus("未启用");
                    if(files2.length > 0){
                        for (File server:files2 ) {
                            if(server.isDirectory()
                                    && server.getName().equals(file.getName())){
                                dockerConfigs.setStatus("当前环境");
                            }
                        }
                    }
                    dockerConfigs.setServerIp(file.getName());
                    list.add(dockerConfigs);
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
    /**
     * configFile
     *
     * @return
     */
    @RequestMapping(value = "configFile/list")
    @ResponseBody
    public Response<Pages<DockerConfigs>> configFile(String serverIp) {
        List<DockerConfigs> list = new ArrayList<>();
        try {
            String homeConfigDir = Dockers.getHomeConfigDir();
            File files = new File(homeConfigDir);
            File[] files1 = files.listFiles();
            for (File file:files1 ) {
                if(file.isDirectory() && file.getName().equals(serverIp)){
                    File[] files2 = file.listFiles();
                    for (File f:files2 ) {
                        DockerConfigs dockerConfigs = new DockerConfigs();
                        dockerConfigs.setFileName(f.getName());
                        dockerConfigs.setServerIp(f.getParentFile().getName());
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
    /**
     * 添加服务
     *
     * @return
     */
    @RequestMapping(value = "addServer")
    @ResponseBody
    public Response<Boolean> addServer( String serverIp) {
        System.out.println("serverIP=="+serverIp);
        try {
            File filePa = new File(Dockers.getHomeConfigDir()+File.separator+serverIp);
            filePa.mkdirs();
        }catch (Exception e){
            e.printStackTrace();
            return Response.error("失败");
        }
        return Response.success("OK");
    }
    /**
     * 删除服务
     *
     * @return
     */
    @RequestMapping(value = "deleteServer")
    @ResponseBody
    public Response<Boolean> deleteServer( String serverIp) {
        System.out.println("serverIP=="+serverIp);
        try {
            File filePa = new File(Dockers.getHomeConfigDir()+File.separator+serverIp);
            OpenFileUtils.deleteFile(filePa);
        }catch (Exception e){
            e.printStackTrace();
            return Response.error("失败");
        }
        return Response.success("OK");
    }
    /**
     * 删除服务
     *
     * @return
     */
    @RequestMapping(value = "deleteServerConfigFile")
    @ResponseBody
    public Response<Boolean> deleteServerConfigFile(String serverIp,String fileName) {
        System.out.println("serverIP=="+serverIp);
        System.out.println("fileName=="+fileName);
        try {
            File filePa = new File(Dockers.getHomeConfigDir()+File.separator+serverIp+File.separator+fileName);
            OpenFileUtils.deleteFile(filePa);
        }catch (Exception e){
            e.printStackTrace();
            return Response.error("失败");
        }
        return Response.success("OK");
    }



    /**
     * 添加配置文件
     *
     * @return
     */
    @RequestMapping(value = "addConfigFile")
    @ResponseBody
    public Response<Boolean> addConfigFile( MultipartFile file,String serverIp) {
        System.out.println("file=="+file);
        System.out.println("serverIp=="+serverIp);
        try {
            File filePa = new File(Dockers.getHomeConfigDir()+File.separator+serverIp+File.separator+file.getOriginalFilename());
             filePa.createNewFile();
            OpenFileUtils.writeFile(file,filePa);
        }catch (Exception e){
            e.printStackTrace();
            return Response.error("失败");
        }
        return Response.success("OK");
    }

    /**
     * verify serverIp
     *
     * @return
     */
    @RequestMapping(value = "/verify")
    @ResponseBody
    public Response<Pages<Config>> k8sVerify(String serverIp) {
        try {
            DockerClient dockerClient = dockers.getDockerClient(Dockers.getHomeConfigDir(), serverIp);
            Version version = dockerClient.versionCmd().exec();
            System.out.println(version.getVersion());
        } catch (Exception e) {
            e.printStackTrace();
            return Response.error("连接失败");
        }
        return Response.success("连接成功");
    }

    /**
     * setEnv
     *
     * @return
     */
    @RequestMapping(value = "/setEnv")
    @ResponseBody
    public Response<Pages<Config>> setEnv(String serverIp) {
        try {
            File   file =  new File(Dockers.getHomeConfigDir()+File.separator+serverIp);
            File[] files = file.listFiles();
            if (files.length > 2){
                OpenFileUtils.deleteFile(new File(Dockers.getHomeConfigFile()));
                for (File f: files) {
                    File file1 = new File(Dockers.getHomeConfigFile() + File.separator + f.getName());
                    file1.createNewFile();
                    OpenFileUtils.writeFile(f,file1);
                }
                //为服务名
                File server = new File(Dockers.getHomeConfigFile() +File.separator+serverIp);
                server.mkdir();
            }else{
                return Response.error("设置失败 请检查配置文件");
            }
        } catch (Exception e) {
            OpenFileUtils.deleteFile(new File(Dockers.getHomeConfigFile()));
            e.printStackTrace();
            return Response.error("设置失败");
        }
        return Response.success("设置成功");
    }

    private void TestSetProperties(String value){
        System.out.println("value==="+value);
        Properties pro = new Properties();
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            ClassLoader classloader = Thread.currentThread().getContextClassLoader();
            inputStream = classloader.getResourceAsStream("application.properties");
            pro.load(inputStream);
            String property = pro.getProperty("docker.port");

            System.out.println("docker.port==="+property);

            pro.setProperty("docker.port", value);
            pro.store(outputStream, "");
            outputStream.flush();
            System.out.println("docker.port==="+property);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            try {
                if(inputStream != null){
                    inputStream.close();
                }
                   if(outputStream != null){
                       outputStream.close();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public void FileInputStreams(File file) throws FileNotFoundException {
        String name = (file != null ? file.getPath() : null);
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkRead(name);
        }
        if (name == null) {
            throw new NullPointerException();
        }
        FileDescriptor  fd = new FileDescriptor();
    }

}
