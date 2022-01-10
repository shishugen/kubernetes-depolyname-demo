package com.c3stones.client.pod;

import com.c3stones.client.BaseConfig;
import com.c3stones.client.Kubes;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.networking.v1beta1.HTTPIngressPath;
import io.fabric8.kubernetes.api.model.networking.v1beta1.HTTPIngressPathBuilder;
import io.fabric8.kubernetes.api.model.networking.v1beta1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1beta1.IngressBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * @ClassName: NginxPod
 * @Description: TODO
 * @Author: stone
 * @Date: 2021/1/26 10:57
 */
@Slf4j
@Component
public class NginxSslPod extends BaseConfig {

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


    public  boolean createDeployment(String namespace, String appName, String labelsName , String image , Integer port,String configName ) {
        ResourceRequirements resource= new ResourceRequirements();
        Map<String,Quantity> map= new HashMap(2);
        //map.put("cpu",new Quantity("m"));
        map.put("memory",new Quantity("3000M"));
        resource.setLimits(map);
        Map<String,Quantity> stringQuantityMap= new HashMap(2);
        //stringQuantityMap.put("cpu",new Quantity(String.valueOf(500),"m"));
        stringQuantityMap.put("memory",new Quantity(String.valueOf(1000),"M"));
        resource.setRequests(stringQuantityMap);
        Deployment newDeployment = new DeploymentBuilder()
                .withNewMetadata()
                .withName(podNginxPrefix+appName)
                .withNamespace(namespace)
                .endMetadata()
                .withNewSpec()
                .withNewSelector()
                .addToMatchLabels(LABELS_KEY, podNginxPrefix+labelsName)
                .endSelector()
                .withReplicas(1)
                .withNewTemplate()
                .withNewMetadata()
                .addToLabels(LABELS_KEY, podNginxPrefix+labelsName)
                .endMetadata()
                .withNewSpec()
                .withContainers(new ContainerBuilder()
                        .withName(labelsName)
                       // .withResources(resource)
                        .withImage(image)
                        .withImagePullPolicy("Always")
                       // .withImagePullPolicy("IfNotPresent")
                        .withSecurityContext(new SecurityContextBuilder().withPrivileged(true).build())
                        .addToPorts(new ContainerPortBuilder().withName(appName).withContainerPort(port).build())
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
                .addToSelector(LABELS_KEY, podNginxPrefix+serviceName).endSpec()
                .build();
        return kubes.getKubeclinet().services().create(build);
    }

    private void ingress(String namespace,String name,String domain,String serviceName,Integer servicePort){
        KubernetesClient kubeclinet = kubes.getKubeclinet();
        Ingress ingress = new IngressBuilder()
                .withNewMetadata()
                .withNamespace(namespace)
                .withName(name)
                .endMetadata()
                .withNewSpec()
                .addNewRule()
                .withHost(domain)
                .withNewHttp()
                .withPaths(new HTTPIngressPathBuilder()
                        .withPath("/")
                        .withNewBackend()
                        .withServiceName(serviceName)
                        .withNewServicePort(servicePort)
                        .endBackend().build())
                .endHttp()
                .endRule()
                .endSpec()

                .build();
        kubeclinet.network().ingresses().create(ingress);
    }



    public static void main(String[] args) {
        String namespace="test2";
       // NginxPod.createNginx("app-app-test");
    }


    public  void createNginx(String namespace,String domain){
        kubes.createNamespace(namespace);
        String podName="nginxssl";
        String configName="nginxssl";
        String labelsName="nginxssl";
        String portName="nginxssl";
        String image ="nginx";
        //  ingress(String namespace,String name,String domain,String serviceName,Integer servicePort){
      //  ingress(namespace,podName,domain,);

      //  create(namespace,podName,labelsName,image,81,podName);
        //  createService(namespace,podName,labelsName,81,portName);
    }

    public  void delete(String namesapce,String podName){
        Boolean aBoolean = kubes.deletePod(namesapce, podName);
        if (aBoolean){
            kubes.deleteService(namesapce,podName);
            kubes.deleteConf(namesapce,podName);
        }
    }


    public  void configMap(String namespace ,String configName,String labelsName,String data){
        ConfigMap configMap = new ConfigMapBuilder()
                .withNewMetadata()
                .withName(podNginxPrefix+configName)
                .withNamespace(namespace)
                .addToLabels(LABELS_KEY,podNginxPrefix+labelsName).endMetadata()
                .addToData("nginx.conf", data
                ).build();
        kubes.getKubeclinet().configMaps().createOrReplace(configMap);
    }

}
