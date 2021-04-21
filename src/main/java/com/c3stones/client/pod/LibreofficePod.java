package com.c3stones.client.pod;

import com.c3stones.client.BaseConfig;
import com.c3stones.client.Kubes;
import io.fabric8.kubernetes.api.model.*;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jcajce.provider.symmetric.util.BaseWrapCipher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;

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
        try{
            Pod pod = new PodBuilder().withNewMetadata().withName(podEnvPrefix+podName).withNamespace(namespace).addToLabels(LABELS_KEY, labelsName).endMetadata()
                    .withNewSpec().withContainers(new ContainerBuilder()
                            .withName(labelsName)
                            .withImage(image)
                            .withImagePullPolicy("Always")
                            .withCommand("/bin/sh","-c")
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


    public void createlibreoffice(String namespace){
        kubes.createNamespace(namespace);
        String podName="libreoffice";
        String configName="libreoffice";
        String labelsName="libreoffice";
        String portName="libreoffice";
       // configMap(namespace,configName,configName);
        create(namespace,podName,labelsName,harborImageEnvPrefix+image,8100,portName,configName);
        createService(namespace,podName,labelsName,8100,portName);
    }

    public  void delete(String namesapce,String podName){
        Boolean aBoolean = kubes.deletePod(namesapce, podName);
        if (aBoolean){
            kubes.deleteService(namesapce,podEnvPrefix+podName);
            kubes.deleteConf(namesapce,podEnvPrefix+podName);
        }
    }





}
