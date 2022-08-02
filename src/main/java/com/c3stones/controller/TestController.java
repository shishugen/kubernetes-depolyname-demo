package com.c3stones.controller;

import com.c3stones.client.BaseConfig;
import com.c3stones.client.Kubes;
import com.c3stones.client.pod.TestPod;
import com.c3stones.common.Response;
import com.c3stones.entity.Pages;
import com.c3stones.entity.Pods;
import com.c3stones.util.KubeUtils;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @ClassName: PodController
 * @Description: TODO
 * @Author: stone
 * @Date: 2021/1/27 10:00
 */
@Controller
@Slf4j
@RequestMapping(value = "test")
public class TestController extends BaseConfig{


    @Autowired
    private  Kubes kubes;

    @Autowired
    private TestPod testPod;


    @RequestMapping(value = "list")
    @ResponseBody
    public List<Pods> list() {
        List<Pods> podsList = new ArrayList<>();
        List<Pod> items = kubes.getKubeclinet().pods().withoutLabel(LABELS_KEY_TEST, LABELS_KEY_TEST).list().getItems();
        for (Pod pod:items){
            Pods pods = new Pods();
            podsList.add(podConverEntity(pods,pod,pod.getMetadata().getName()));
        }
        return podsList;
    }

    /**
     * test
     *
     * @return
     */
    @RequestMapping(value = "list2")
    public String testList() {
        return "pages/podtest/list";
    }




    /**
     * api pod转换
     * @param pod
     * @param kubePod
     * @return
     */
    public  Pods podConverEntity(Pods pod , io.fabric8.kubernetes.api.model.Pod kubePod,String svcName){
        PodStatus status = kubePod.getStatus();
        ObjectMeta metadata = kubePod.getMetadata();
        PodSpec podSpec = kubePod.getSpec();
        String hostIp = status.getHostIP();
        pod.setPodIp(status.getPodIP());
        pod.setHostIp(hostIp);
        pod.setPodName(metadata.getName());
        pod.setPodStatus(getPodStatus(kubePod).getReason());
        pod.setConfigName(svcName);
        pod.setNamespace(metadata.getNamespace());
        pod.setDate(KubeUtils.StringFormatDate(status.getStartTime()));
        List<Volume> volumes = podSpec.getVolumes();
        if(volumes != null && volumes.size() > 0){
            volumes.forEach(v->{
                NFSVolumeSource nfs = v.getNfs();
                if (nfs != null){
                    String path = nfs.getPath();
                    String server = nfs.getServer();
                    pod.setNfsPath(path);
                    pod.setNfsServer(server);
                }
            });
        }
        ///metadata.getLabels().get("app");
        Container container = podSpec.getContainers().get(0);
        String image = container.getImage();

        String split[] =image.split("/");
        String images = split[split.length - 1];
        String[] split1 = images.split(":");
        List<ContainerPort> ports1 = container.getPorts();
        if(ports1 != null && ports1.size() > 0 ){

        }
        return pod;
    }


    /**
     * 获取pod的状态
     * @param pod
     */
    public TestController.KubePodStatus getPodStatus(io.fabric8.kubernetes.api.model.Pod pod){
        PodStatus status = pod.getStatus();
        List<ContainerStatus> containerStatuses = status.getContainerStatuses();
        if(containerStatuses.size()== 0){
            List<PodCondition> conditions = status.getConditions();
            if(conditions != null && conditions.size() > 0){
                PodCondition podCondition = conditions.get(0);
                return  new TestController.KubePodStatus(podCondition.getStatus(),podCondition.getReason(), podCondition.getMessage());
            }else{
                return  new TestController.KubePodStatus("fail","fail", "未知异常！");

            }
        }
        ContainerState state = containerStatuses.get(0).getState();
        TestController.KubePodStatus podStatus = new TestController.KubePodStatus("Running","running","运行中");
        if(state.getRunning() != null){
            //运行
        }else if(state.getWaiting() != null){
            //等待
            ContainerStateWaiting waiting = state.getWaiting();
            podStatus = new TestController.KubePodStatus("waiting",waiting.getReason(),waiting.getMessage());
        }else if(state.getTerminated() != null){
            //完成
            ContainerStateTerminated terminated = state.getTerminated();
            podStatus = new TestController.KubePodStatus("terminated",terminated.getReason(),terminated.getMessage());
        }
        return podStatus;
    }


    /**
     * 状态 类
     */
    @Data
    public  class  KubePodStatus {
        private String status; //状态
        private String reason; //原因
        private String message; //信息

        public KubePodStatus(String status, String reason, String message) {
            this.status = status;
            this.reason = reason;
            this.message = message;
        }

    }


}
