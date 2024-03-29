package com.c3stones.client.pod;

import com.c3stones.client.BaseConfig;
import com.c3stones.client.Kubes;
import com.c3stones.entity.Pods;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @ClassName: guacd
 * @Description: TODO
 * @Author: stone
 * @Date: 2021/1/26 10:57
 */
@Slf4j
@Component
public class GuacdPod extends BaseConfig {

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
            String pvcName =namespace + podName;
        ResourceRequirements resource= new ResourceRequirements();
        Map<String,Quantity> map= new HashMap(2);
        //map.put("cpu",new Quantity("m"));
        map.put("memory",new Quantity("2000M"));
        resource.setLimits(map);
        Map<String,Quantity> stringQuantityMap= new HashMap(2);
        //stringQuantityMap.put("cpu",new Quantity(String.valueOf(500),"m"));
        stringQuantityMap.put("memory",new Quantity(String.valueOf(1000),"M"));
        resource.setRequests(stringQuantityMap);
            Pod pod = new PodBuilder().withNewMetadata().withName(podEnvPrefix+podName).withNamespace(namespace).addToLabels(LABELS_KEY, labelsName).endMetadata()
                    .withNewSpec().withContainers(new ContainerBuilder()
                            .withName(labelsName)
                            .withImage(image)
                            .withResources(resource)
                           // .withImagePullPolicy("Always")
                           // .withImagePullPolicy("IfNotPresent")
                            .withCommand("/bin/sh","-c")
                            .addToArgs("/usr/local/guacamole/sbin/guacd -b 0.0.0.0 -L $GUACD_LOG_LEVEL -f")
                            .addToPorts(new ContainerPortBuilder().withName(portName).withContainerPort(port).build())
                            .build())
                    .endSpec().build();
            Pod newPod = kubes.getKubeclinet().pods().create(pod);
            System.out.println(newPod);
        return true;
    }

    public  boolean createDeployment(String namespace, String podName, String labelsName, String image, Integer port, String portName, boolean isAnew) {
        ResourceRequirements resource= new ResourceRequirements();
        Map<String,Quantity> map= new HashMap(1);
        map.put("memory",new Quantity("1000M"));
        resource.setLimits(map);
        String policy ="IfNotPresent";
        if (isAnew){
            policy ="Always";
        }
        Map<String,Quantity> stringQuantityMap= new HashMap(1);
        stringQuantityMap.put("memory",new Quantity(String.valueOf(500),"M"));
        resource.setRequests(stringQuantityMap);
        Map<String,String> isk8sArm = isk8sArm();

        Deployment newDeployment = new DeploymentBuilder()
                .withNewMetadata()
                .withName(podEnvPrefix+podName)
                .withNamespace(namespace)
                .endMetadata()
                .withNewSpec()
                .withNewSelector()
                .addToMatchLabels(LABELS_KEY, labelsName)
                .endSelector()
                .withReplicas(1)
                .withNewTemplate()
                .withNewMetadata()
                .addToLabels(LABELS_KEY,labelsName)
                .endMetadata()
                .withNewSpec()
                .addToNodeSelector(isk8sArm)
                .addNewContainer().withName(podName).withImagePullPolicy(policy)
                .withImage(image)
                .withCommand("/bin/sh","-c")
                .addToArgs("/usr/local/guacamole/sbin/guacd -b 0.0.0.0 -L $GUACD_LOG_LEVEL -f")
                .addToPorts(new ContainerPortBuilder().withName(portName).withContainerPort(port).build())
                .withResources(resource)
                .endContainer()
                .endSpec().endTemplate().endSpec().build();
        kubes.getKubeclinet().apps().deployments().createOrReplace(newDeployment);
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



    public void createGuacamole(String namespace, String podName, boolean isAnew){
        String labelsName="guacamole";
        String portName=podName;

        try {
            createDeployment(namespace,podName,labelsName,harborImageEnvPrefix+image,4822,portName,isAnew);
            Service service = kubes.getKubeclinet().services().inNamespace(namespace).withName(podEnvPrefix+labelsName).get();
            if(service == null){
                createService(namespace,labelsName,labelsName,4822,portName);
            }
        }catch (Exception e){
            e.printStackTrace();
            kubes.getKubeclinet().apps().deployments().inNamespace(namespace).withName(podEnvPrefix+podName).delete();
            kubes.getKubeclinet().services().inNamespace(namespace).withName(podEnvPrefix+podName).delete();
        }
    }
}
