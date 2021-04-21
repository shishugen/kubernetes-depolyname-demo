package com.c3stones.client.pod;

import com.c3stones.client.BaseConfig;
import com.c3stones.client.Kubes;
import io.fabric8.kubernetes.api.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName: NginxPod
 * @Description: TODO
 * @Author: stone
 * @Date: 2021/1/26 10:57
 */
@Slf4j
@Component
public class NginxPod extends BaseConfig {

    /***
     * labels
     */
    private  static  String LABELS_KEY = "app";

    private  static  String MODE = "standalone";

    private  static  String MOUNT_PATH = "/home/";

    @Autowired
    private  Kubes kubes;
    ///etc/nginx/nginx.conf

    @Value("${redis.image}")
    private String image;

    @Value("${pod.nginx.prefix}")
    private String podNginxPrefix;

    /***
     * 创建 pod
     * @param namespace
     * @param podName
     * @param image
     * @return
     */
    public  boolean create(String namespace, String podName, String labelsName , String image , Integer port,String configName ){
            Pod pod = new PodBuilder().withNewMetadata().withName(podNginxPrefix+podName).withNamespace(namespace).addToLabels(LABELS_KEY, labelsName).endMetadata()
                    .withNewSpec().withContainers(new ContainerBuilder()
                            .withName(labelsName)
                            .withImage(image)
                            .withImagePullPolicy("Always")
                            .withSecurityContext(new SecurityContextBuilder().withPrivileged(true).build())
                            .addToPorts(new ContainerPortBuilder().withName(podName).withContainerPort(port).build())
                            .addToVolumeMounts(new VolumeMountBuilder().withName(podNginxPrefix+configName).withMountPath(MOUNT_PATH)
                                    .build())
                            .build())
                    .addToVolumes(
                            new VolumeBuilder()
                                    .withName(podNginxPrefix+configName)
                                    .withConfigMap(new
                                            ConfigMapVolumeSourceBuilder()
                                            .withName(podNginxPrefix+configName).build()
                                    ).build())
                    .endSpec().build();
            Pod newPod = kubes.getKubeclinet().pods().createOrReplace(pod);
            System.out.println(newPod);
        return true;
    }

    //

    /**
     *
     * @param namespace
     * @param serviceName
     * @param port
     * @param nodePort 30000-32767
     */
    public  Service createService(String namespace, String serviceName, Integer port ,Integer nodePort){

        String type = "NodePort";
        Service build = new ServiceBuilder()
                .withNewMetadata()
                .withName(podNginxPrefix+serviceName)
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
                .addToSelector(LABELS_KEY, serviceName).endSpec()
                .build();
        return kubes.getKubeclinet().services().create(build);
    }

    public static void main(String[] args) {
        String namespace="test2";
       // NginxPod.createNginx("app-app-test");
    }


    public  void createNginx(String namespace){
        kubes.createNamespace(namespace);
        String podName="nginx";
        String configName="nginx";
        String labelsName="nginx";
        String portName="nginx";
        String image ="nginx";
        // configMap(namespace,configName,configName);
      //  create(namespace,podName,labelsName,image,81,podName);
        //  createService(namespace,podName,labelsName,81,portName);
    }

    public  void delete(String namesapce,String podName){
        Boolean aBoolean = kubes.deletePod(namesapce, podName);
        kubes.getKubeclinet().apps().deployments().inNamespace(namesapce).withName(podNginxPrefix+podName).delete();
        if (aBoolean){
            kubes.deleteService(namesapce,podNginxPrefix+podName);
            kubes.deleteConf(namesapce,podNginxPrefix+podName);
        }
    }


    public  void configMap(String namespace ,String configName,String labelsName,String data){
        ConfigMap configMap = new ConfigMapBuilder()
                .withNewMetadata()
                .withName(podNginxPrefix+configName)
                .withNamespace(namespace)
                .addToLabels(LABELS_KEY,labelsName).endMetadata()
                .addToData("nginx.conf", data
                ).build();
        kubes.getKubeclinet().configMaps().createOrReplace(configMap);
    }

}
