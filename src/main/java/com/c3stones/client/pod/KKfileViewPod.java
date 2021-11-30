package com.c3stones.client.pod;

import com.c3stones.client.BaseConfig;
import com.c3stones.client.Kubes;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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
public class KKfileViewPod extends BaseConfig {

    /***
     * labels
     */
    private  static  String LABELS_KEY = "app";

    @Autowired
    private  Kubes kubes;


    @Value("${pod.namespace.prefix}")
    private String podNamespacePrefix;


    @Value("${kkfileview.image}")
    private String image;

    @Value("${pod.env.prefix}")
    private String podEnvPrefix;




    public  boolean createDeployment(String namespace, String podName, String labelsName, String image, Integer port, String portName, boolean isAnew, String kkfileviewHttps) {
        ResourceRequirements resource= new ResourceRequirements();
        Map<String,Quantity> map= new HashMap(1);
        map.put("memory",new Quantity("500M"));
        resource.setLimits(map);
        String policy ="IfNotPresent";
        if (isAnew){
            policy ="Always";
        }
        List<EnvVar> envVars = new ArrayList<>();
        if (StringUtils.isNotBlank(kkfileviewHttps)){
            EnvVar envVar = new EnvVar();
            envVar.setName("KK_BASE_URL");
            envVar.setValue(kkfileviewHttps);
            EnvVar envVar2 = new EnvVar();
            envVar2.setName("KK_CONTEXT_PATH");
            envVar2.setValue("/preview");
            envVars.add(envVar);
            envVars.add(envVar2);
        }
        Map<String,Quantity> stringQuantityMap= new HashMap(1);
        stringQuantityMap.put("memory",new Quantity(String.valueOf(200),"M"));
        resource.setRequests(stringQuantityMap);
        String pvcName =namespace + podName;
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
                .addNewContainer().withName(podName).withImage(image).withImagePullPolicy(policy)
                .addToPorts(new ContainerPortBuilder().withName(portName).withContainerPort(port).build())
                .withEnv(envVars)
                .withResources(resource)
                .endContainer()
                .addNewVolume()
                .withName("date-config").withNewHostPath().withNewPath("/etc/localtime").endHostPath()
                .endVolume()
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
        String image="kkfileview:latest";
        String portName="nacos";
       // Kubes.createNamespace(namespace);
        Integer nodePort = 30867;
     //  create(namespace,podName,labelsName,image,8848,portName);
       // createService(namespace,podName,labelsName,8848,portName,nodePort);



    }



    public void create(String namespace, Integer nodePort, boolean isAnew, String kkfileviewHttps){
        String podName="kkfileview";
        String labelsName="kkfileview";
        String portName="kkfileview";
        try {
            kubes.createNamespace(namespace);
            createDeployment(namespace,podName,labelsName,harborImageEnvPrefix+image,8012,portName,isAnew,kkfileviewHttps);
            createService(namespace,podName,labelsName,8012,nodePort,portName);
        }catch (Exception e){
            e.printStackTrace();
            kubes.getKubeclinet().apps().deployments().inNamespace(namespace).withName(podEnvPrefix+podName).delete();
            kubes.getKubeclinet().services().inNamespace(namespace).withName(podEnvPrefix+podName).delete();
        }
    }
}
