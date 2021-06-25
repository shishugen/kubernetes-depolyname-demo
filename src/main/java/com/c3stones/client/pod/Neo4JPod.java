package com.c3stones.client.pod;

import com.c3stones.client.BaseConfig;
import com.c3stones.client.Kubes;
import com.c3stones.entity.Pods;
import io.fabric8.kubernetes.api.model.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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
        try{
            String pvcName =namespace + podName;
            kubes.createPVC(pvcName,namespace,nfsStorageClassName,nfsNeo4jStorageSize);
            Pod pod = new PodBuilder().withNewMetadata().withName(podEnvPrefix+podName).withNamespace(namespace).addToLabels(LABELS_KEY, labelsName).endMetadata()
                    .withNewSpec().withContainers(new ContainerBuilder()
                            .withName(labelsName)
                            .withImage(image)
                           // .withImagePullPolicy("Always")
                            .withImagePullPolicy("IfNotPresent")
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
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
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


    public void create(String namespace){
        String labelsName="neo4j";
        String portName="neo4j";
        String podName="neo4j";
        Integer port=7687;
        try {
            create(namespace,podName,labelsName,harborImageEnvPrefix+image,port,portName);
            Service service = kubes.getKubeclinet().services().inNamespace(namespace).withName(podEnvPrefix+labelsName).get();
            if(service == null){
                createService(namespace,labelsName,labelsName,port);
            }
        }catch (Exception e){
            e.printStackTrace();
            kubes.getKubeclinet().pods().inNamespace(namespace).withName(podEnvPrefix+podName).delete();
            kubes.getKubeclinet().services().inNamespace(namespace).withName(podEnvPrefix+podName).delete();
        }
    }
}
