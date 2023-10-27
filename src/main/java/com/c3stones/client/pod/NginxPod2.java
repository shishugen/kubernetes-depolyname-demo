package com.c3stones.client.pod;

import com.c3stones.client.BaseConfig;
import com.c3stones.client.Kubes;
import com.c3stones.util.OpenFileUtils;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.networking.v1beta1.*;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.*;

/**
 * @ClassName: NginxPod
 * @Description: TODO
 * @Author: stone
 * @Date: 2021/1/26 10:57
 */
@Slf4j
@Component
public class NginxPod2 extends BaseConfig {

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
        Map<String,String> nodeSelectorMap = isk8sArm();
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
                .addToNodeSelector(nodeSelectorMap)
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


    public void ingress(String namespace,String name,String domain,String serviceName,Integer servicePort) throws FileNotFoundException {
        KubernetesClient kubeclinet = kubes.getKubeclinet();
        File file = new File(Kubes.getHomeSSLDir()+File.separator+domain);
        String key = "";
        String crt = "";
        if (file.isDirectory()){
            File[] files = file.listFiles();
            for (int i = 0;i <files.length;i++){
                File file1 = files[i];
                String fileExtension = OpenFileUtils.getFileExtension(file1);
                if ("key".equals(fileExtension)){
                     key = OpenFileUtils.readFile(new FileInputStream(file1));
                }else if("crt".equals(fileExtension)){
                    crt = OpenFileUtils.readFile(new FileInputStream(file1));
                }
            }
            tls(namespace,name,key,crt);
        }
        Map<String,String> map =new HashMap<>();
        map.put("nginx.ingress.kubernetes.io/proxy-body-size","500m");
        Ingress ingress = new IngressBuilder()
                .withNewMetadata()
                .withNamespace(namespace)
                .withAnnotations(map)
                .addToLabels("app-ingress",podNginxPrefix+name)
                .withName(podNginxPrefix+name)
                .endMetadata()
                .withNewSpec()
                .addToTls(new IngressTLSBuilder().addToHosts(domain)
                        .withNewSecretName(podNginxPrefix+name).build())
                .addNewRule()
                .withHost(domain)
                .withNewHttp()
                .withPaths(new HTTPIngressPathBuilder()
                        .withPath("/")
                        .withNewBackend()
                        .withServiceName(podNginxPrefix+serviceName)
                        .withNewServicePort(servicePort)
                        .endBackend().build())
                .endHttp()
                .endRule()
                .endSpec()
                .build();
        kubeclinet.network().ingresses().create(ingress);
    }


    public void tls(String namespace,String name,String key,String crt){
       // System.out.println(crt);
      // System.out.println(key);
        KubernetesClient kubeclinet = kubes.getKubeclinet();
        Secret secretBuilder = new SecretBuilder()
                .withNewMetadata()
                .withName(podNginxPrefix+name)
                .addToLabels("app-secret","0")
                .withNamespace(namespace)
                .endMetadata()
                .withType("kubernetes.io/tls")
                .addToData("tls.key",Base64.getEncoder().encodeToString(key.getBytes()))
                .addToData("tls.crt",Base64.getEncoder().encodeToString(crt.getBytes()))
                .build();
        kubeclinet.secrets().create(secretBuilder);
    }





}
