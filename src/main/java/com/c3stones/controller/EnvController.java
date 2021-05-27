package com.c3stones.controller;

import cn.hutool.db.Page;
import com.c3stones.client.BaseConfig;
import com.c3stones.client.Kubes;
import com.c3stones.client.pod.*;
import com.c3stones.common.Response;
import com.c3stones.entity.HarborImage;
import com.c3stones.entity.Pages;
import com.c3stones.http.HttpHarbor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

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
    private RabbitMQPod rabbitMQPod ;

    @Autowired
    private FastdfsPod fastdfsPod ;

    @Autowired
    private RedisPod redisPod ;
    @Autowired
    private LibreofficePod libreofficePod ;

    @Autowired
    private GuacdPod guacdPod ;

    @Value("${pod.namespace.prefix}")
    private String podNamespacePrefix;
    @Value("${pod.env.prefix}")
    private String podEnvPrefix;


    /**
     * 查询列表
     *
     * @return
     */
    @RequestMapping(value = "list")
    public String list() {
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



    @RequestMapping(value = "listData")
    @ResponseBody
    public Response<Pages<HarborImage>> listData() {
        List<HarborImage> harborList = httpHarbor.harborList(harborImageEnvProjectName);
        Pages page = new Pages();
        page.setRecords(harborList);
        page.setTotal(harborList.size());
        return Response.success(page);
    }


    /**
     *
     * @param name
     * @return
     */
    @RequestMapping(value = "create")
    @ResponseBody
    public Response<Boolean> create(String name,String namespace,Integer nodePort,String guacamoleName,Integer MySQLnodePort) {
        System.out.println("创建 名称为 : "+name+namespace);
        Assert.notNull(namespace, "name不能为空");
        Assert.notNull(name, "name不能为空");
        if(kubes.checkSvc(nodePort)){
            return Response.error("端口已存在");
        }
        if(kubes.checkSvc(MySQLnodePort)){
            return Response.error("端口已存在");
        }
        try {
            switch (name){
                case "mysql":
                    mySQLPod.createMySQL(namespace,MySQLnodePort);
                    break;
                case "nacos":
                    nacosPod.createNacos(namespace);
                    break;
                case "redis":
                    redisPod.createRedis(namespace);
                    break;
                case "rabbit":
                    rabbitMQPod.createRabbitmq(namespace);
                    break;
                case "fdfs":
                    fastdfsPod.createT(namespace,nodePort);
                    break;
                case "libreoffice":
                    libreofficePod.createlibreoffice(namespace);
                    break;
                case "guacamole":
                    guacdPod.createGuacamole(namespace,guacamoleName);
                    break;
                case "neo4j":
                    neo4JPod.create(namespace);
                    break;
            }
        }catch (Exception e){
            e.printStackTrace();
            kubes.getKubeclinet().pods().inNamespace(namespace).withName(podEnvPrefix+name).delete();
            kubes.getKubeclinet().services().inNamespace(namespace).withName(podEnvPrefix+name).delete();
        }

        return Response.success(true);
    }



}
