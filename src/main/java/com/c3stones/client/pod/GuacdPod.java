package com.c3stones.client.pod;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.c3stones.client.Kubes;
import com.c3stones.entity.NacosEntity;
import com.c3stones.entity.Pods;
import io.fabric8.kubernetes.api.model.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName: guacd
 * @Description: TODO
 * @Author: stone
 * @Date: 2021/1/26 10:57
 */
@Slf4j
@Component
public class GuacdPod {

    /***
     * labels
     */
    private  static  String LABELS_KEY = "app";


    @Autowired
    private  Kubes kubes;


    @Value("${pod.namespace.prefix}")
    private String podNamespacePrefix;


    @Value("${guacamole.image}")
    private String image;

    @Value("${pod.env.prefix}")
    private String podEnvPrefix;

    /***
     * 创建 pod
     * @param namespace
     * @param podName
     * @param image
     * @return
     */
    public  boolean create(String namespace, String podName, String labelsName , String image , Integer port,String portName ){
        try{
            String pvcName =namespace + podName;
            Pod pod = new PodBuilder().withNewMetadata().withName(podEnvPrefix+podName).withNamespace(namespace).addToLabels(LABELS_KEY, labelsName).endMetadata()
                    .withNewSpec().withContainers(new ContainerBuilder()
                            .withName(labelsName)
                            .withImage(image)
                            .withImagePullPolicy("Always")
                            .withCommand("/bin/sh","-c")
                            .addToArgs("/usr/local/guacamole/sbin/guacd -b 0.0.0.0 -L $GUACD_LOG_LEVEL -f")
                            .addToPorts(new ContainerPortBuilder().withName(portName).withContainerPort(port).build())
                            .build())
                    .endSpec().build();
            Pod newPod = kubes.getKubeclinet().pods().create(pod);
            System.out.println(newPod);
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public List<Pods>  getNacos(){
        List<String> list = new ArrayList<>();
        List<Pods> podsList = new ArrayList<>();
        List<Pod> items = kubes.getKubeclinet().pods().list().getItems();
        for (Pod pod : items){
            String namespace = pod.getMetadata().getNamespace();
            if(namespace.contains(podNamespacePrefix) && pod.getMetadata().getName().contains("nacos")){
                Pods pods = new Pods();
                podsList.add(podConverEntity(pods,pod));
              //  list.add(pod.getMetadata().getName()+"."+namespace);
            }
        }
        return podsList;
    }

    /**
     * api pod转换
     * @param pod
     * @param kubePod
     * @return
     */
    public  Pods podConverEntity(Pods pod , Pod kubePod){

        PodStatus status = kubePod.getStatus();
        ObjectMeta metadata = kubePod.getMetadata();
        PodSpec podSpec = kubePod.getSpec();
        String hostIp = status.getHostIP();
        pod.setPodIp(status.getPodIP());
        pod.setHostIp(hostIp);
        pod.setNacosName(metadata.getName()+"."+metadata.getNamespace());
        List<ContainerPort> ports1 = podSpec.getContainers().get(0).getPorts();
        if(ports1 != null && ports1.size() > 0 ){
            Service service = kubes.findService(metadata.getNamespace(), metadata.getName());
            if(service != null) {
                ServiceSpec spec = service.getSpec();
                List<ServicePort> ports = spec.getPorts();
                if (ports != null && ports.size() > 0) {
                    StringBuffer buffer = new StringBuffer(ports.size());
                    for (ServicePort port : ports) {
                        Integer nodePort = port.getNodePort();
                        if (buffer.toString().length() > 0) {
                            buffer.append(",");
                        }
                        buffer.append(nodePort);
                    }
                    pod.setPorts(buffer.toString());
                }
            }
        }
        return pod;
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
    //

    /**
     *
     * @param namespace
     * @param serviceName
     * @param labelsValue
     * @param port
     * @param portName
     * @param nodePort 30000-32767
     */
    public  Service createService(String namespace, String serviceName,  String labelsValue , Integer port,String portName ){
        String type = "NodePort";
        Service build = new ServiceBuilder()
                .withNewMetadata()
                .withName(podEnvPrefix+serviceName)
                .withNamespace(namespace)
                .endMetadata()
                .withNewSpec()
                .addNewPort()
                //内网端口
                .withPort(port)
                .withProtocol("TCP")
               // .withNodePort(nodePort)
                .endPort()
                .withType(type)
                .addToSelector(LABELS_KEY, labelsValue).endSpec()
                .build();
        return kubes.getKubeclinet().services().create(build);
    }

    public static void main(String[] args) {



    }



    public void createGuacamole(String namespace,String podName){
        String labelsName=podName;
        String portName=podName;
        create(namespace,podName,labelsName,image,4822,portName);
       // createService(namespace,podName,labelsName,4822,portName);
    }
}
