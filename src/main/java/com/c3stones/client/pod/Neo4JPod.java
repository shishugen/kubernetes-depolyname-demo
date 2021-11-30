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
public class Neo4JPod extends BaseConfig {

    /***
     * labels
     */
    private  static  String LABELS_KEY = "app";


    @Autowired
    private  Kubes kubes;



    @Value("${neo4j.image}")
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
            kubes.createPVC(pvcName,namespace,nfsStorageClassName,nfsNeo4jStorageSize);
            Pod pod = new PodBuilder().withNewMetadata().withName(podEnvPrefix+podName).withNamespace(namespace).addToLabels(LABELS_KEY, labelsName).endMetadata()
                    .withNewSpec().withContainers(new ContainerBuilder()
                            .withName(labelsName)
                            .withImage(image)
                           // .withImagePullPolicy("Always")
                          //  .withImagePullPolicy("IfNotPresent")
                            .withVolumeMounts(new VolumeMountBuilder().withName(pvcName).withMountPath("/data/").build())
                            .withCommand("/sbin/tini","-g")
                            .addToArgs("/docker-entrypoint.sh","neo4j")
                            .addToPorts(new ContainerPortBuilder().withName(portName).withContainerPort(port).build())
                            .addToPorts(new ContainerPortBuilder().withName(portName+"1").withContainerPort(7474).build())
                            .build())
                    .withVolumes(new VolumeBuilder().withName(pvcName)
                            .withPersistentVolumeClaim(new PersistentVolumeClaimVolumeSourceBuilder().withClaimName(pvcName).build()).build())
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
        String pvcName =namespace + podName;
        kubes.createPVC(pvcName,namespace,nfsStorageClassName,nfsNeo4jStorageSize);
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
                .addNewContainer()
                .withImage(image)
                .withName(podName)
                .withImagePullPolicy(policy)
                .withVolumeMounts(new VolumeMountBuilder().withName(pvcName).withMountPath("/data/").build())
                .withCommand("/sbin/tini","-g")
                .addToArgs("/docker-entrypoint.sh","neo4j")
                .addToPorts(new ContainerPortBuilder().withName(portName).withContainerPort(port).build())
                .addToPorts(new ContainerPortBuilder().withName(portName+"1").withContainerPort(7474).build())
                .withResources(resource)
                .endContainer()
                .withVolumes(new VolumeBuilder().withName(pvcName)
                        .withPersistentVolumeClaim(new PersistentVolumeClaimVolumeSourceBuilder().withClaimName(pvcName).build()).build())
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
     */
    public  Service createService(String namespace, String serviceName,  String labelsValue , Integer port){
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
                 .withName(serviceName)
                .withProtocol("TCP")
                .endPort()
                .withType(type)

                .addNewPort()
                //内网端口
                .withName(serviceName+"1")
                .withPort(7474)
                .withProtocol("TCP")
                .endPort()
                .withType(type)

                .addToSelector(LABELS_KEY, labelsValue).endSpec()
                .build();
        return kubes.getKubeclinet().services().create(build);
    }


    public void create(String namespace, boolean isAnew){
        String labelsName="neo4j";
        String portName="neo4j";
        String podName="neo4j";
        Integer port=7687;
        try {
            createDeployment(namespace,podName,labelsName,harborImageEnvPrefix+image,port,portName,isAnew);
            Service service = kubes.getKubeclinet().services().inNamespace(namespace).withName(podEnvPrefix+labelsName).get();
            if(service == null){
                createService(namespace,labelsName,labelsName,port);
            }
        }catch (Exception e){
            e.printStackTrace();
            kubes.getKubeclinet().apps().deployments().inNamespace(namespace).withName(podEnvPrefix+podName).delete();
            kubes.getKubeclinet().services().inNamespace(namespace).withName(podEnvPrefix+podName).delete();
        }
    }
}
