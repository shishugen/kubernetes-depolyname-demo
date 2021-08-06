package com.c3stones.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.c3stones.client.BaseConfig;
import com.c3stones.client.Dockers;
import com.c3stones.client.Kubes;
import com.c3stones.common.Response;
import com.c3stones.entity.HarborImage;
import com.c3stones.entity.Namespaces;
import com.c3stones.entity.Pages;
import com.c3stones.http.HttpHarbor;
import com.c3stones.util.OpenFileUtils;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.ServiceSpec;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName: HarborController
 * @Description: TODO
 * @Author: stone
 * @Date: 2021/1/25 15:55
 */
@Controller
@Slf4j
@RequestMapping(value = "port")
public class PortController {


    @Autowired
    private Kubes kubes ;

    @Value("${pod.namespace.prefix}")
    private String podNamespacePrefix;

    /**
     * 查询列表
     *
     * @return
     */
    @RequestMapping(value = "list")
    public String list() {
        return "pages/port/list";
    }


    @RequestMapping(value = "listData")
    @ResponseBody
    public Response<Pages<Port>> listData(String namespace){
        Pages page = new Pages();
        List<Port> service = new ArrayList<>();
        if (!StringUtils.isEmpty(namespace)){
             service = getService(namespace);
        }else{
            List<Namespaces> harborList = kubes.getNamespace();
            for (Namespaces name:harborList) {
                service.addAll(getService(name.getName()));
            }
        }
        page.setRecords(service);
        page.setTotal(service.size());
        return Response.success(page);
    }

    private List<Port> getService(String namespace){
        List<Port> portList = new ArrayList<>();
        ServiceList serviceList = kubes.findService(namespace);
        if (serviceList != null )
            for (Service service:serviceList.getItems()) {
                if(service != null) {
                    String namespace1 = service.getMetadata().getNamespace();
                    String name = service.getMetadata().getName();
                    ServiceSpec spec = service.getSpec();
                    List<ServicePort> ports = spec.getPorts();
                    if (ports != null && ports.size() > 0) {
                        Port port2 = new Port(namespace1,name);
                        StringBuffer buffer = new StringBuffer(ports.size());
                        for (ServicePort port : ports) {
                            Integer port1 = port.getPort();
                            Integer nodePort = port.getNodePort();
                            if (buffer.toString().length() > 0) {
                                buffer.append(",");
                            }
                            buffer.append(port1 + ":" + nodePort);
                        }
                        port2.setPort(buffer.toString());
                        portList.add(port2);
                    }
                }
            }
            return portList;
    }

    @Data
    private class Port{
        public Port(String namespace,String name) {
            this.namespace = namespace;
            this.name = name;
        }

        private  String name;
        private  String namespace;
        private  String port;
    }



    /**
     * 删除
     *
     * @param projectName
     * @param tag
     * @return
     */
    @RequestMapping(value = "delete")
    @ResponseBody
    public Response<Boolean> delete(String namespace,String name) {
        log.info("删除  projectName : {}:{}",namespace,name);
        Assert.notNull(namespace, "namespace不能为空");
        Assert.notNull(name, "name不能为空");
        kubes.getKubeclinet().services().inNamespace(namespace).withName(name).delete();
        return Response.success(false);
    }




}
