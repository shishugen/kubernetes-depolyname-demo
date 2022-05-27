package com.c3stones.controller;

import cn.hutool.core.net.NetUtil;
import com.c3stones.client.BaseConfig;
import com.c3stones.client.Kubes;
import com.c3stones.common.Response;
import com.c3stones.entity.Config;
import com.c3stones.entity.K8sNode;
import com.c3stones.entity.Namespaces;
import com.c3stones.entity.Pages;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.*;

/**
 * @ClassName: NodeController
 * @Description: TODO
 * @Author: stone
 * @Date: 2022/5/20 14:00
 */
@Controller
@Slf4j
@RequestMapping(value = "node")
public class NodeController {


    @Autowired
    private Kubes kubes ;


    /**
     *
     * @return
     */
    @RequestMapping(value = "list")
    public String list() {
        return "pages/node/list";
    }

    public void test(){
        System.out.println("进入…………");
        List<K8sNode> node = kubes.findNode();
        node.forEach(a->{
            System.out.println(a);
        });
    }

    @RequestMapping(value = "listData")
    @ResponseBody
    public Response<List<K8sNode>> listData(){
        return Response.success(kubes.findNode());
    }

    /**
     * 查询网络
     * @param host  检测的IP地址
     * @param isMaster 是不为主节点
     * @return
     */
    @RequestMapping(value = "checkNet")
    @ResponseBody
    public Response<Map<String,List<K8sNode>>> checkNet(String host,String isMaster){
        Config config = BaseConfig.initConfig();
        String ports = "";
        //true : 为master
        if("true".equals(isMaster)){
            //主节点
             ports= config.getK8sNetPort();
        }else{
            //子节点端口
             ports = config.getK8sNetNodePort();
        }
        Map<String,List<K8sNode>> map = new HashMap<>(2);
        List<K8sNode> k8sNodeSuccess = new ArrayList<>();
        List<K8sNode> k8sNodeError = new ArrayList<>();
        String[] portArr = ports.trim().split(",");
        List<String> list = Arrays.asList(portArr);
        for (String p:list){
            Integer p1 = Integer.valueOf(p.trim());
            if (isHostConnectable(host, p1)){
                k8sNodeSuccess.add(new K8sNode(host,p1));
            }else{
                k8sNodeError.add(new K8sNode(host,p1));
            }
        }
        map.put("success",k8sNodeSuccess);
        map.put("error",k8sNodeError);
        return Response.success(map);
    }

    /**
     * 查询网络
     * @param host  检测的IP地址
     * @param port 端口
     * @return
     */
    @RequestMapping(value = "checkHost")
    @ResponseBody
    public Response<Boolean> checkNet(String host,Integer port){
        return Response.success(isHostConnectable(host,port));
    }

    public static boolean isHostConnectable(String host, Integer port) {
        return NetUtil.isOpen(NetUtil.buildInetSocketAddress(host, port), 200);
    }

}
