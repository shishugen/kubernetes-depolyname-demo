package com.c3stones.client.pod;

import com.c3stones.client.BaseConfig;
import com.c3stones.client.Kubes;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;

import java.util.HashMap;
import java.util.Map;

/**
 * @ClassName: RedisPod
 * @Description: TODO
 * @Author: stone
 * @Date: 2021/1/26 10:57
 */
@Slf4j
@Component
public class LibreofficePod extends BaseConfig {

    /***
     * labels
     */
    private  static  String LABELS_KEY = "app";

    private  static  String MODE = "standalone";

    private  static  String MOUNT_PATH = "/data/middleware-data/redis/conf/";
    @Autowired
    private  Kubes kubes;

    @Value("${pod.env.prefix}")
    private String podEnvPrefix;


    @Value("${libreoffice.image}")
    private String image;
    /***
     * 创建 pod
     * @param namespace
     * @param podName
     * @param image
     * @return
     */
    public  boolean create(String namespace, String podName, String labelsName , String image , Integer port,String portName ,String configName ){
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
                            .withImagePullPolicy("Always")
                           // .withImagePullPolicy("IfNotPresent")
                            .withCommand("/bin/sh","-c")
                            .withResources(resource)
                            .addToArgs("/usr/bin/soffice --headless --accept=\"socket,host=0,port=8100;urp;\" --nofirststartwizard --invisible")
                            .addToPorts(new ContainerPortBuilder().withName(portName).withContainerPort(port).build())
                          //  .addToVolumeMounts(new VolumeMountBuilder().withName(configName).withMountPath(MOUNT_PATH).build())
                            .build())
                         /*  .addToVolumes(
                                   new VolumeBuilder()
                                           .withName(configName)
                                           .withConfigMap(new
                                                   ConfigMapVolumeSourceBuilder()
                                                   .withName(configName).build()).build())*/
                    .endSpec().build();
            Pod newPod = kubes.getKubeclinet().pods().create(pod);
            System.out.println(newPod);
        return true;
    }

    public  boolean createDeployment(String namespace, String podName, String labelsName, String image, Integer port, String portName, String configName, boolean isAnew) {
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
        String pvcName =namespace + podName;
        Map<String,String> nodeSelectorMap = isk8sArm();
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
                .addToNodeSelector(nodeSelectorMap)
                .addNewContainer().withName(podName).withImagePullPolicy(policy)
                .withImage(image)
                     .withCommand("/bin/sh","-c")
                     .addToArgs("/usr/bin/soffice --headless --accept=\"socket,host=0,port=8100;urp;\" --nofirststartwizard --invisible")
                .addToPorts(new ContainerPortBuilder().withName(portName).withContainerPort(port).build())
                .withResources(resource)
                .endContainer()
               // .addNewVolume().withName("date-config").withNewHostPath().withNewPath("/etc/localtime").endHostPath().endVolume()
                .endSpec().endTemplate().endSpec().build();
        kubes.getKubeclinet().apps().deployments().createOrReplace(newDeployment);
        return true;
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
             //   .withNodePort(nodePort)
                .endPort()
                .withType(type)
                .addToSelector(LABELS_KEY, labelsValue).endSpec()
                .build();
        return kubes.getKubeclinet().services().create(build);
    }

    public static void main(String[] args) {
        String namespace="test2";
        String podName="redis";
        String configName="redis2";

       // configMap(namespace,configName,configName);
        String labelsName="redis";
        String image="redis:5.0.6";
        String portName="redis";
     //   Kubes.createNamespace(namespace);
        Integer nodePort = 30869;
       // create(namespace,podName,labelsName,image,6379,portName,configName);
      //  createService(namespace,podName,labelsName,6379,portName,nodePort);
       // configMap(namespace,podName,labelsName);

        Jedis jedis=new Jedis("10.49.0.12", 30869);
        //jedis.auth("123456");
        jedis.set("wxf", "我很强");
        String value=jedis.get("wxf");
        System.out.println(value);
        //释放资源
        jedis.close();
    }


    public void createlibreoffice(String namespace, boolean isAnew){
        kubes.createNamespace(namespace);
        String podName="libreoffice";
        String configName="libreoffice";
        String labelsName="libreoffice";
        String portName="libreoffice";
       // configMap(namespace,configName,configName);

        try {
            createDeployment(namespace,podName,labelsName,harborImageEnvPrefix+image,8100,portName,configName,isAnew);
            createService(namespace,podName,labelsName,8100,portName);
        }catch (Exception e){
            e.printStackTrace();
            kubes.getKubeclinet().apps().deployments().inNamespace(namespace).withName(podEnvPrefix+podName).delete();
            kubes.getKubeclinet().services().inNamespace(namespace).withName(podEnvPrefix+podName).delete();
        }
    }

    public  void delete(String namesapce,String podName){
        Boolean aBoolean = kubes.deletePod(namesapce, podName);
        if (aBoolean){
            kubes.deleteService(namesapce,podEnvPrefix+podName);
            kubes.deleteConf(namesapce,podEnvPrefix+podName);
        }
    }





}
