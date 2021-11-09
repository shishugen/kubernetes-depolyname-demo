package com.c3stones.client.pod;

import com.c3stones.client.BaseConfig;
import com.c3stones.client.Kubes;
import com.c3stones.exception.KubernetesException;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

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
        kubes.createPVC(pvcName,namespace,nfsStorageClassName,nfsMySqlStorageSize);
            Pod pod = new PodBuilder().withNewMetadata().withName(podEnvPrefix+podName).withNamespace(namespace).addToLabels(LABELS_KEY, labelsName).endMetadata()
                    .withNewSpec().withContainers(new ContainerBuilder()
                           // .withVolumeMounts(new VolumeMountBuilder().withMountPath("/home/").withName("mysql").build())
                            .withName(labelsName)
                            .withImage(image)
                            .withImagePullPolicy("Always")
                          //  .withImagePullPolicy("IfNotPresent")
                            .withResources(resource)
                            .withVolumeMounts(new VolumeMountBuilder().withName(pvcName).withMountPath("/var/lib/mysql/").build())
                            .addToPorts(new ContainerPortBuilder().withName(portName).withContainerPort(port).build())
                            .addToEnv(new EnvVarBuilder().withName("MYSQL_ROOT_PASSWORD").withValue(MYSQL_ROOT_PASSWORD).build())
                            .addToEnv(new EnvVarBuilder().withName("lower_case_table_names").withValue("1").build())
                            .build())//.withVolumes(new VolumeBuilder().withName("mysql").withNewNfs().withServer("192.168.0.218").withPath("/xuanyuan/nfs/data/test/").endNfs().build())
                       .withVolumes(new VolumeBuilder().withName(pvcName)
                            .withPersistentVolumeClaim(new PersistentVolumeClaimVolumeSourceBuilder().withClaimName(pvcName).build()).build())
                    .endSpec()
                    .
                            build();
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
        kubes.createPVC(pvcName,namespace,nfsStorageClassName,nfsMySqlStorageSize);
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
                .addNewContainer().withName(podName).withImage(image).withVolumeMounts(new VolumeMountBuilder().withName(pvcName).withMountPath("var/lib/mysql/").build())
                .addToPorts(new ContainerPortBuilder().withName(portName).withContainerPort(port).build())
                .addToEnv(new EnvVarBuilder().withName("MYSQL_ROOT_PASSWORD").withValue(MYSQL_ROOT_PASSWORD).build())
                .addToEnv(new EnvVarBuilder().withName("lower_case_table_names").withValue("1").build())
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
    protected final static  String[] POD_COMMAND_ARRY = {"/usr/sbin/sshd","-D"};

    public static void main(String[] args) {
        KubernetesClient kubeclinet = Kubes.getKubeclinet2();

        String labelsName = "test";
        Deployment newDeployment = new DeploymentBuilder()
                .withNewMetadata()
                .withName("test")
                .withNamespace("default")
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
                .addNewContainer().withName(labelsName).withImage("centos:centos7").addToCommand("/usr/sbin/init")
                .withVolumeMounts(new VolumeMountBuilder().withName("test01").withMountPath("/home/").withReadOnly(true).build())
                .endContainer()
                .addNewVolume()
                .withNewHostPath().withPath("/home2/").endHostPath()
                .withName("test01")
                .endVolume()
              //  .withVolumes(new VolumeBuilder().withName(pvcName)
                     //   .withPersistentVolumeClaim(new PersistentVolumeClaimVolumeSourceBuilder().withClaimName(pvcName).build()).build())
                .endSpec().endTemplate().endSpec().build();
        kubeclinet.apps().deployments().createOrReplace(newDeployment);
    }
    public void  createMySQL(String namespace, Integer nodePort){
        String podName="mysql";
        String labelsName="mysql";
        String portName="mysql";
        try {
            kubes.createNamespace(namespace);
            createDeployment(namespace,podName,labelsName,harborImageEnvPrefix+image,3306,portName);
            createService(namespace,podName,labelsName,3306,portName,nodePort);
        }catch (Exception e){
            e.printStackTrace();
            kubes.getKubeclinet().apps().deployments().inNamespace(namespace).withName(podEnvPrefix+podName).delete();
            kubes.getKubeclinet().services().inNamespace(namespace).withName(podEnvPrefix+podName).delete();
        }

    }

}
