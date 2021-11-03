package com.c3stones.client.pod;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.c3stones.client.BaseConfig;
import com.c3stones.client.Kubes;
import com.c3stones.controller.PodController;
import com.c3stones.entity.NacosEntity;
import com.c3stones.entity.Pods;
import com.c3stones.util.KubeUtils;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @ClassName: nacos
 * @Description: TODO
 * @Author: stone
 * @Date: 2021/1/26 10:57
 */
@Slf4j
@Component
public class NacosPod extends BaseConfig {

    /***
     * labels
     */
    private  static  String LABELS_KEY = "app";

    private  static  String MODE = "standalone";

    private  static  String EMBEDDED_STORAGE = "embedded";
    @Autowired
    private  Kubes kubes;


    @Value("${pod.namespace.prefix}")
    private String podNamespacePrefix;


    @Value("${nacos.image}")
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
        ResourceRequirements resource= new ResourceRequirements();
        Map<String,Quantity> map= new HashMap(2);
        //map.put("cpu",new Quantity("m"));
        map.put("memory",new Quantity("2000M"));
        resource.setLimits(map);

        Map<String,Quantity> stringQuantityMap= new HashMap(2);
        //stringQuantityMap.put("cpu",new Quantity(String.valueOf(500),"m"));
        stringQuantityMap.put("memory",new Quantity(String.valueOf(1000),"M"));
        resource.setRequests(stringQuantityMap);
            String pvcName =namespace + podName;
            kubes.createPVC(pvcName,namespace,nfsStorageClassName,nfsNacosStorageSize);
            Pod pod = new PodBuilder().withNewMetadata().withName(podEnvPrefix+podName).withNamespace(namespace).addToLabels(LABELS_KEY, labelsName).endMetadata()
                    .withNewSpec().withContainers(new ContainerBuilder()
                            .withName(labelsName)
                            .withImage(image)
                            .withImagePullPolicy("IfNotPresent")
                            .withVolumeMounts(new VolumeMountBuilder().withName(pvcName).withMountPath("/home/nacos/data/").build())
                            .addToPorts(new ContainerPortBuilder().withName(portName).withContainerPort(port).build())
                            .addToEnv(new EnvVarBuilder().withName("MODE").withValue(MODE).build())
                            .addToEnv(new EnvVarBuilder().withName("EMBEDDED_STORAGE").withValue(EMBEDDED_STORAGE).build())
                            .withResources(resource)
                          //  .withVolumeMounts(new VolumeMountBuilder().withName("date").withMountPath("/etc/localtime").build())
                            .build())
                    //.withVolumes(new VolumeBuilder().withName("date").withHostPath(new HostPathVolumeSourceBuilder().withNewPath("/etc/localtime").build()).build())
                    .withVolumes(new VolumeBuilder().withName(pvcName)
                            .withPersistentVolumeClaim(new PersistentVolumeClaimVolumeSourceBuilder().withClaimName(pvcName).build()).build())
                    .endSpec().build();
            Pod newPod = kubes.getKubeclinet().pods().create(pod);
            System.out.println(newPod);
        return true;
    }


    public  boolean createDeployment(String namespace, String podName, String labelsName , String image , Integer port,String portName) {
        ResourceRequirements resource= new ResourceRequirements();
        Map<String,Quantity> map= new HashMap(1);
        map.put("memory",new Quantity("1000M"));
        resource.setLimits(map);

        Map<String,Quantity> stringQuantityMap= new HashMap(1);
        stringQuantityMap.put("memory",new Quantity(String.valueOf(500),"M"));
        resource.setRequests(stringQuantityMap);
        String pvcName =namespace + podName;
        kubes.createPVC(pvcName,namespace,nfsStorageClassName,nfsNacosStorageSize);
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
                .addNewContainer().withName(podName).withImage(image)
                .withVolumeMounts(new VolumeMountBuilder().withName(pvcName).withMountPath("/home/nacos/data/").build())
                .addToPorts(new ContainerPortBuilder().withName(portName).withContainerPort(port).build())
                .addToEnv(new EnvVarBuilder().withName("MODE").withValue(MODE).build())
                .addToEnv(new EnvVarBuilder().withName("EMBEDDED_STORAGE").withValue(EMBEDDED_STORAGE).build())
                .withResources(resource)
                .endContainer()
                .addNewVolume()
                .withName("date-config").withNewHostPath().withNewPath("/etc/localtime").endHostPath()
                .endVolume()
                .withVolumes(new VolumeBuilder().withName(pvcName)
                        .withPersistentVolumeClaim(new PersistentVolumeClaimVolumeSourceBuilder().withClaimName(pvcName).build()).build())
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
    public  Pods podConverEntity(Pods pod , io.fabric8.kubernetes.api.model.Pod kubePod){

        PodStatus status = kubePod.getStatus();
        ObjectMeta metadata = kubePod.getMetadata();
        PodSpec podSpec = kubePod.getSpec();
        String hostIp = status.getHostIP();
        pod.setPodIp(status.getPodIP());
        pod.setHostIp(hostIp);
        pod.setNacosName(metadata.getName()+"."+metadata.getNamespace());
        List<ContainerPort> ports1 = podSpec.getContainers().get(0).getPorts();
        if(ports1 != null && ports1.size() > 0 ){
            Service service = kubes.findService(metadata.getNamespace(),podEnvPrefix+kubePod.getMetadata().getLabels().get(LABELS_KEY));

          //  Service service = kubes.findService(metadata.getNamespace(), metadata.getName());
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
                    pod.setServiceName(service.getMetadata().getName());
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
    public  Service createService(String namespace, String serviceName,  String labelsValue , Integer port,Integer nodePort,String portName){
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
                .withNodePort(nodePort)
                .endPort()
                .withType(type)
                .addToSelector(LABELS_KEY, labelsValue).endSpec()
                .build();
        return kubes.getKubeclinet().services().create(build);
    }

    public static void main(String[] args) {
        String namespace="test2";
        String podName="nacos";
        String labelsName="nacos";
       // String image="nacos/nacos-server:1.2.1";
        String image="nacos/nacos-server:1.2.1";
        String portName="nacos";
       // Kubes.createNamespace(namespace);
        Integer nodePort = 30867;
     //  create(namespace,podName,labelsName,image,8848,portName);
       // createService(namespace,podName,labelsName,8848,portName,nodePort);



    }

    public List<NacosEntity> getNamespace(String ip,Integer port){
        RestTemplate restTemplate = new RestTemplate();
        JSONObject forObject = restTemplate.getForObject("http://"+ip+":"+port+"/nacos/v1/console/namespaces", JSONObject.class);
        JSONArray data = forObject.getJSONArray("data");
        List<NacosEntity> list = new ArrayList<>();
        for (int i = 0 ; i < data.size(); i ++){
            NacosEntity object1 = data.getObject(i, NacosEntity.class);
            list.add(object1);
        }
        return list;
    }


    public void createNacos(String namespace,Integer nodePort){
        String podName="nacos";
        String labelsName="nacos";
        String portName="nacos";
        try {
            kubes.createNamespace(namespace);
            createDeployment(namespace,podName,labelsName,harborImageEnvPrefix+image,8848,portName);
            createService(namespace,podName,labelsName,8848,nodePort,portName);
        }catch (Exception e){
            e.printStackTrace();
            kubes.getKubeclinet().apps().deployments().inNamespace(namespace).withName(podEnvPrefix+podName).delete();
            kubes.getKubeclinet().services().inNamespace(namespace).withName(podEnvPrefix+podName).delete();
        }
    }
}
