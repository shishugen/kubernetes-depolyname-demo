package com.c3stones.controller;

import com.c3stones.client.Kubes;
import com.c3stones.common.Response;
import com.c3stones.entity.Namespaces;
import com.c3stones.entity.Pages;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

/**
 * @ClassName: NamespaceController
 * @Description: TODO
 * @Author: stone
 * @Date: 2021/1/27 10:00
 */
@Controller
@RequestMapping(value = "namespace")
public class NamespaceController {



    @Value("${pod.namespace.prefix}")
    private String podNamespacePrefix;

    @Autowired
    private Kubes kubes;


    /**
     * 查询列表
     *
     * @return
     */
    @RequestMapping(value = "list")
    public String list() {
        return "pages/namespace/list";
    }

    /**
     * 新增
     *
     * @return
     */
    @RequestMapping(value = "add")
    public String add() {
        return "pages/namespace/add";
    }



    @RequestMapping(value = "listData")
    @ResponseBody
    public Response<Pages<String>> listData() {
        List<Namespaces> harborList = kubes.getNamespace();
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
    public Response<Boolean> create(String name) {
        System.out.println("创建 名称为 : "+name);
        String namespace = podNamespacePrefix +name;
        Assert.notNull(name, "name不能为空");
        kubes.createNamespace(namespace);
        return Response.success(true);
    }

    /**
     * 删除
     *
     * @return
     */
    @RequestMapping(value = "delete")
    @ResponseBody
    public Response<Boolean> delete(String namespace  ) {
        Assert.notNull(namespace, "namespace不能为空");
        Boolean aBoolean = kubes.deleteNamespace(namespace);
        if (aBoolean){
            return Response.success(true);
        }
        return Response.success(false);
    }


}
