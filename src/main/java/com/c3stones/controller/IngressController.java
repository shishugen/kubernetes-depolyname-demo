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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

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
    public Response<Pages> listData() {
        KubernetesClient kubeclinet = kubes.getKubeclinet();
        List list = new ArrayList();
        List<Ingress> items = kubeclinet.network().ingresses().withLabel("app-ingress").list().getItems();
        items.forEach(a->{
            List<IngressRule> rules = a.getSpec().getRules();
            IngressRule ingressRule = rules.get(0);
            list.add(ingressRule);
        });
        Pages page = new Pages();
        page.setRecords(list);
        page.setTotal(list.size());
        return Response.success(page);
    }

}
