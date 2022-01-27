package com.c3stones.controller;

import com.c3stones.client.BaseConfig;
import com.c3stones.client.Kubes;
import com.c3stones.client.pod.*;
import com.c3stones.common.Response;
import com.c3stones.entity.HarborImage;
import com.c3stones.entity.Pages;
import com.c3stones.entity.Pvc;
import com.c3stones.http.HttpHarbor;
import com.c3stones.util.KubeUtils;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimList;
import io.fabric8.kubernetes.api.model.Quantity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName: EnvController
 * @Description: TODO
 * @Author: stone
 * @Date: 2021/1/27 10:00
 */
@Controller
@Slf4j
@RequestMapping(value = "pvc")
public class PvcController extends BaseConfig {

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
        return "pages/pvc/list";
    }



    @RequestMapping(value = "listData")
    @ResponseBody
    public Response<Pages<HarborImage>> listData() {
        List<Pvc> list = new ArrayList<>();
        List<PersistentVolumeClaim> persistentVolumeClaimList = kubes.getKubeclinet().persistentVolumeClaims().list().getItems();
        for (PersistentVolumeClaim pvc:persistentVolumeClaimList ) {
            ObjectMeta metadata = pvc.getMetadata();
            String name = metadata.getName();
            if (name.startsWith(podNamespacePrefix)){
                String namespace = metadata.getNamespace();
                Quantity storage = pvc.getSpec().getResources().getRequests().get("storage");
                String sto = storage.getAmount() + storage.getFormat();
                String phase = pvc.getStatus().getPhase();
                Pvc pvc1 = new Pvc(sto,name,namespace,KubeUtils.StringFormatDate(metadata.getCreationTimestamp()),phase);
                list.add(pvc1);
           }
        }
        Pages page = new Pages();
        page.setRecords(list);
        page.setTotal(list.size());
        return Response.success(page);
    }


    /**
     * 删除
     *
     * @return
     */
    @RequestMapping(value = "delete")
    @ResponseBody
    public Response<Boolean> delete(String namespace,String pvcName) {
        Assert.notNull(namespace, "namespace不能为空");
        Assert.notNull(pvcName, "pvcName不能为空");
        log.info("namespace : {}  , pvcName : {}",namespace,pvcName);
        kubes.deletePvc(namespace,pvcName);
        return Response.success(true);
    }




}
