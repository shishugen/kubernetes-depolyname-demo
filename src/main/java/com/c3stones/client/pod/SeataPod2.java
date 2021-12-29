package com.c3stones.client.pod;

import com.c3stones.client.BaseConfig;
import com.c3stones.client.Kubes;
import com.c3stones.entity.PodParameter;
import com.c3stones.entity.Pods;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import lombok.extern.slf4j.Slf4j;
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
public class SeataPod2 extends BaseConfig {

    /***
     * labels
     */
    private  static  String LABELS_KEY = "app";


    @Autowired
    private  Kubes kubes;


    @Value("${pod.namespace.prefix}")
    private String podNamespacePrefix;


    @Value("${seata.image}")
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
                            .withCommand("export JAVA_HOME=/usr/lib/jvm/jre-1.8.0-openjdk-1.8.0.312.b07-1.el7_9.x86_64","/home/seata/bin/seata-server.sh")
                            .withName(labelsName)
                            .withImage("harbor.org/tool/seata:1.4.0")
                            .withImagePullPolicy("IfNotPresent")
                            .withResources(resource)
                            .build())

                    .endSpec().build();
            Pod newPod = kubes.getKubeclinet().pods().create(pod);
            System.out.println(newPod);
        return true;
    }


    public  boolean createDeployment(String namespace, String podName, String labelsName, String image, PodParameter podParameter,String configName) {
        ResourceRequirements resource= new ResourceRequirements();
        Map<String,Quantity> map= new HashMap(1);
        map.put("memory",new Quantity("1000M"));
        resource.setLimits(map);
        List<EnvVar> env = new ArrayList<>();
        EnvVar envVar = new EnvVar();
        envVar.setName("NACOS_SERVER");
        envVar.setValue(podParameter.getNacosServer());
        env.add(envVar);

        EnvVar envVar2 = new EnvVar();
        envVar2.setName("NACOS_PORT");
        envVar2.setValue("8848");
        env.add(envVar2);

        EnvVar envVar3 = new EnvVar();
        envVar3.setName("NACOS_NAMESPACE");
        envVar3.setValue(podParameter.getNacosNamespace());
        env.add(envVar3);

        EnvVar envVar30 = new EnvVar();
        envVar30.setName("NACOS_CONFIG_NAMESPACE");
        envVar30.setValue(podParameter.getNacosConfigNamespace());
        env.add(envVar30);

        EnvVar envVar4 = new EnvVar();
        envVar4.setName("SEATA_GROUP");
        envVar4.setValue("SEATA_GROUP");
        env.add(envVar4);

        EnvVar envVar5 = new EnvVar();
        envVar5.setName("NACOS_USER");
        envVar5.setValue(podParameter.getNacosUser());
        env.add(envVar5);

        EnvVar envVar6 = new EnvVar();
        envVar6.setName("NACOS_PWD");
        envVar6.setValue(podParameter.getNacosPwd());
        env.add(envVar6);

        EnvVar envVar7 = new EnvVar();
        envVar7.setName("MYSQL_SERVER");
        envVar7.setValue(podParameter.getMysqlServer());
        env.add(envVar7);

        EnvVar envVar8 = new EnvVar();
        envVar8.setName("MYSQL_PORT");
        envVar8.setValue("3306");
        env.add(envVar8);

        EnvVar envVar9 = new EnvVar();
        envVar9.setName("MYSQL_DATABASE");
        envVar9.setValue(podParameter.getMysqlDatabase());
        env.add(envVar9);

        EnvVar envVar10 = new EnvVar();
        envVar10.setName("MYSQL_USER");
        envVar10.setValue(podParameter.getMysqlUser());
        env.add(envVar10);

        EnvVar envVar11 = new EnvVar();
        envVar11.setName("MYSQL_PWD");
        envVar11.setValue(podParameter.getMysqlPwd());
        env.add(envVar11);

        Map<String,Quantity> stringQuantityMap= new HashMap(1);
        stringQuantityMap.put("memory",new Quantity(String.valueOf(500),"M"));
        resource.setRequests(stringQuantityMap);
        String pvcName =namespace + podName;
       // kubes.createPVC(pvcName,namespace,nfsStorageClassName,nfsNacosStorageSize);
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
                .withImagePullPolicy("Always")
                .withCommand("/home/start.sh")
                .withName(podName).withImage(image)
                .withEnv(env)
                .addToVolumeMounts(new VolumeMountBuilder().withName(configName).withMountPath("/home/seata/registry/").build())
                .withResources(resource)
                .endContainer()
                .addToVolumes(
                        new VolumeBuilder()
                                .withName(configName)
                                .withConfigMap(new
                                        ConfigMapVolumeSourceBuilder()
                                        .withName(configName).build()).build())
                .endSpec().endTemplate().endSpec().build();
      new  Kubes().getKubeclinet().apps().deployments().createOrReplace(newDeployment);
        return true;
    }








    public static void main(String[] args) {
        String namespace="app-ssg";
        String podName="state";
        String labelsName="state";
        String configName="state";
       // String image="nacos/nacos-server:1.2.1";
        String image="nacos/nacos-server:1.2.1";
        String portName="state";
       // Kubes.createNamespace(namespace);
        Integer nodePort = 30867;
     //  create(namespace,podName,labelsName,image,8848,portName);
       // createService(namespace,podName,labelsName,8848,portName,nodePort);
      //  createDeployment(namespace,podName,labelsName,harborImageEnvPrefix+image,8091,portName,false,configName);



    }

    public  void configMap(String namespace ,String configName,String labelsName,PodParameter podParameter){
        ConfigMap configMap = new ConfigMapBuilder()
                .withNewMetadata()
                .withName(configName)
                .withNamespace(namespace)
                .addToLabels(LABELS_KEY,labelsName).endMetadata()
                .addToData("registry.conf",
                        "registry {\n" +
                                "        type = \"nacos\"\n" +
                                "        nacos {\n" +
                                "          application = \"seata-server\"\n" +
                                "          serverAddr = \""+podParameter.getNacosServer()+":8848\"\n" +
                                "          namespace = \""+podParameter.getNacosNamespace()+"\"\n" +
                                "        }\n" +
                                "    }\n" +
                                "    config {\n" +
                                "      type = \"nacos\"\n" +
                                "      nacos {\n" +
                                "        serverAddr = \""+podParameter.getNacosServer()+":8848\"\n" +
                                "        group = \"SEATA_GROUP\"\n" +
                                "          namespace = \""+podParameter.getNacosConfigNamespace()+"\"\n" +
                                "      }\n" +
                                "    }"
                ).build();
        kubes.getKubeclinet().configMaps().createOrReplace(configMap);
    }


    public void createSeata(PodParameter podParameter){
        String podName="seata";
        String labelsName="seata";
        String portName="seata";
        String configName ="seata";
        String namespace =podParameter.getNamespace();
        try {
            configMap(namespace,configName,configName,podParameter);
            kubes.createNamespace(namespace);
            createDeployment(namespace,podName,labelsName,harborImageEnvPrefix+image,podParameter,configName);
           // createService(namespace,podName,labelsName,8091,nodePort,portName);
        }catch (Exception e){
            e.printStackTrace();
            kubes.getKubeclinet().apps().deployments().inNamespace(namespace).withName(podEnvPrefix+podName).delete();
            kubes.getKubeclinet().services().inNamespace(namespace).withName(podEnvPrefix+podName).delete();
        }
    }
}
