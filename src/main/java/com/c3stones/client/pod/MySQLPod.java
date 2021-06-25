package com.c3stones.client.pod;

import com.c3stones.client.BaseConfig;
import com.c3stones.client.Kubes;
import com.c3stones.exception.KubernetesException;
import io.fabric8.kubernetes.api.model.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @ClassName: MySQLPod
 * @Description: TODO
 * @Author: stone
 * @Date: 2021/1/26 10:57
 */
@Slf4j
@Component
public class MySQLPod extends BaseConfig {

    /***
     * labels
     */
    private  static  String LABELS_KEY = "app";

    private  static  String MYSQL_ROOT_PASSWORD = "123456";

    @Value("${MySQL.image}")
    private String image;

    @Autowired
    private  Kubes kubes;


    @Value("${pod.env.prefix}")
    private String podEnvPrefix;

    /***
     * 创建 pod
     * @param namespace
     * @param podName
     * @param image
     * @return
     */
    public  boolean create(String namespace, String podName, String labelsName , String image , Integer port,String portName
        ,String password ){
        if(StringUtils.isNotBlank(password)){
            MYSQL_ROOT_PASSWORD = password;
        }
            String pvcName =namespace + podName;
            kubes.createPVC(pvcName,namespace,nfsStorageClassName,nfsMySqlStorageSize);
            Pod pod = new PodBuilder().withNewMetadata().withName(podEnvPrefix+podName).withNamespace(namespace).addToLabels(LABELS_KEY, labelsName).endMetadata()
                    .withNewSpec().withContainers(new ContainerBuilder()
                            .withName(labelsName)
                            .withImage(image)
                           // .withImagePullPolicy("Always")
                            .withImagePullPolicy("IfNotPresent")
                            .withVolumeMounts(new VolumeMountBuilder().withName(pvcName).withMountPath("/var/lib/mysql/").build())
                            .addToPorts(new ContainerPortBuilder().withName(portName).withContainerPort(port).build())
                            .addToEnv(new EnvVarBuilder().withName("MYSQL_ROOT_PASSWORD").withValue(MYSQL_ROOT_PASSWORD).build())
                            .addToEnv(new EnvVarBuilder().withName("lower_case_table_names").withValue("1").build())
                            .build())
                       .withVolumes(new VolumeBuilder().withName(pvcName)
                            .withPersistentVolumeClaim(new PersistentVolumeClaimVolumeSourceBuilder().withClaimName(pvcName).build()).build())
                    .endSpec().build();
            Pod newPod = kubes.getKubeclinet().pods().create(pod);
            System.out.println(newPod);
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
    public  Service createService(String namespace, String serviceName,  String labelsValue , Integer port,String portName,Integer nodePort){
        if(nodePort > 30000 || nodePort < 32767){
            log.error("端口为 30000-32767   nodePort : {}",nodePort);
            new KubernetesException("端口为30000-32767  端口异常"+nodePort);
        }
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
        Integer nodePort = 23234;
        if(nodePort > 30000 || nodePort < 32767){
            log.error("端口为 30000-32767   nodePort : {}",nodePort);
        }
        System.out.println(1111);
    }
    public void  createMySQL(String namespace, Integer nodePort){
        String podName="mysql";
        String labelsName="mysql";
        String portName="mysql";
        try {
            kubes.createNamespace(namespace);
            create(namespace,podName,labelsName,harborImageEnvPrefix+image,3306,portName,"");
            createService(namespace,podName,labelsName,3306,portName,nodePort);
        }catch (Exception e){
            e.printStackTrace();
            kubes.getKubeclinet().pods().inNamespace(namespace).withName(podEnvPrefix+podName).delete();
            kubes.getKubeclinet().services().inNamespace(namespace).withName(podEnvPrefix+podName).delete();
        }

    }

}
