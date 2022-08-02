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
public class TestPod extends BaseConfig {



    private  static  String MYSQL_ROOT_PASSWORD = "123456";

    @Value("${MySQL.image}")
    private String image;

    @Autowired
    private  Kubes kubes;


    @Value("${pod.env.prefix}")
    private String podEnvPrefix;



    public  boolean createDeployment(String namespace, String podName, String labelsName, String image, Integer port, String portName, Integer replicas) {
        ResourceRequirements resource= new ResourceRequirements();
        Map<String,Quantity> map= new HashMap(1);
        map.put("memory",new Quantity("400M"));
        resource.setLimits(map);
        Map<String,Quantity> stringQuantityMap= new HashMap(1);
        stringQuantityMap.put("memory",new Quantity(String.valueOf(200),"M"));
        resource.setRequests(stringQuantityMap);
        Deployment newDeployment = new DeploymentBuilder()
                .withNewMetadata()
                .withName(podEnvPrefix+podName)
                .addToLabels(LABELS_KEY_TEST,LABELS_KEY_TEST)
                .withNamespace(namespace)
                .endMetadata()
                .withNewSpec()
                .withNewSelector()
                .addToMatchLabels(LABELS_KEY_TEST, LABELS_KEY_TEST)
                .endSelector()
                .withReplicas(replicas)
                .withNewTemplate()
                .withNewMetadata()
                .addToLabels(LABELS_KEY_TEST,LABELS_KEY_TEST)
                .endMetadata()
                .withNewSpec()
                .addNewContainer().withName(podName).withImage(image)
                 .withSecurityContext(new SecurityContextBuilder().withNewPrivileged(true).build())
                .addToPorts(new ContainerPortBuilder().withName(portName).withContainerPort(port).build())
                .withResources(resource)
                .endContainer()
                .addNewVolume()
                .withName("date-config").withNewHostPath().withNewPath("/etc/localtime").endHostPath()
                .endVolume()
                .endSpec().endTemplate()
        .endSpec().build();
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
    public  Service createService(String namespace, String serviceName,  String labelsValue , Integer port,String portName){
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
                //.withNodePort(nodePort)
                .endPort()
                .withType(type)
                .addToSelector(LABELS_KEY_TEST, labelsValue).endSpec()
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
                .addToMatchLabels(LABELS_KEY_TEST, labelsName)
                .endSelector()
                .withReplicas(1)
                .withNewTemplate()
                .withNewMetadata()
                .addToLabels(LABELS_KEY_TEST,labelsName)
                .endMetadata()
                .withNewSpec()
                .addNewContainer().withSecurityContext(new SecurityContextBuilder().withNewPrivileged(true).build())
                .withName(labelsName).withImage("centos:centos7").addToCommand("/usr/sbin/init")
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
    public void  create(String namespace ,Integer replicas){
        String podName="test-vnc";
        String labelsName="test-vnc";
        String portName="test-vnc";
        try {
            kubes.createNamespace(namespace);
            createDeployment(namespace,podName,labelsName,"ssgssg/centos7_vnc:v1.0",5901,portName,replicas);
           // createService(namespace,podName,labelsName,5901,portName);
        }catch (Exception e){
            e.printStackTrace();
            kubes.getKubeclinet().apps().deployments().inNamespace(namespace).withName(podEnvPrefix+podName).delete();
            kubes.getKubeclinet().services().inNamespace(namespace).withName(podEnvPrefix+podName).delete();
        }

    }

}
