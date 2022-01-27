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
public class VsFtpPod extends BaseConfig {

    /***
     * labels
     */
    private  static  String LABELS_KEY = "app";

    private  static  String MYSQL_ROOT_PASSWORD = "123456";

    @Value("${vsFtp.image}")
    private String image;

    @Autowired
    private  Kubes kubes;


    @Value("${pod.env.prefix}")
    private String podEnvPrefix;



    public  boolean createDeployment(String namespace, String podName, String labelsName, String image, String vsftpdNginxName, String portName,
                                    Integer vsftpdPort , Integer vsftpdNginxPort,boolean isAnew) {
        ResourceRequirements resource= new ResourceRequirements();
        Map<String,Quantity> map= new HashMap(1);
        map.put("memory",new Quantity("1000M"));
        resource.setLimits(map);
        Map<String,Quantity> stringQuantityMap= new HashMap(1);
        stringQuantityMap.put("memory",new Quantity(String.valueOf(500),"M"));
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
                .withHostNetwork(true)
                .addNewContainer().withName(podName).withImage(image)
              //  .withImagePullPolicy(policy)
                .withVolumeMounts(new VolumeMountBuilder().withName(pvcName).withMountPath("/home/vsftpd/").build())
                .addToPorts(new ContainerPortBuilder().withName(portName).withContainerPort(21).build())
              //  .addToPorts(new ContainerPortBuilder().withName(portName+"1").withContainerPort(21110).build())
                .addToPorts(new ContainerPortBuilder().withName(vsftpdNginxName).withContainerPort(20080).build())
                .addToEnv(new EnvVarBuilder().withName("LOCAL_UMASK").withValue("000").build())
              //  .addToEnv(new EnvVarBuilder().withName("FTP_USER").withValue("myuser").build())
               // .addToEnv(new EnvVarBuilder().withName("FTP_PASS").withValue("mypass").build())
               // .addToEnv(new EnvVarBuilder().withName("PASV_ADDRESS").withValue("139.9.41.14").build())
              //  .addToEnv(new EnvVarBuilder().withName("PASV_MAX_PORT").withValue("21110").build())
               // .addToEnv(new EnvVarBuilder().withName("PASV_MIN_PORT").withValue("21110").build())
                .addToEnv(new EnvVarBuilder().withName("PASV_ADDRESS")
                        .withValueFrom(new EnvVarSourceBuilder().withFieldRef(
                                new ObjectFieldSelectorBuilder().withFieldPath("status.podIP").build()).build())
                        .build())
                .addToEnv(new EnvVarBuilder().withName("FILE_OPEN_MODE").withValue("0777").build())
                .withResources(resource)
                .endContainer()
             //   .withDnsPolicy("ClusterFirst")
                .addNewVolume()
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
    public  Service createService(String namespace, String serviceName,  String labelsValue , Integer port,
                                  String portName,Integer nodePort,String portNginxName,Integer nodeNginxPort){
        if(nodePort > 30000 || nodePort < 32767){
            log.error("端口为 30000-32767   nodePort : {}",nodePort);
            new KubernetesException("端口为30000-32767  端口异常"+nodePort);
        }
        String type = "NodePort";
        Map<String,String> annotations = new HashMap<>();
        annotations.put("service.beta.kubernetes.io/alibaba-cloud-loadbalancer-spec","slb.s1.small");
        Service build = new ServiceBuilder()
                .withNewMetadata()
                //.withAnnotations(annotations)
                .withName(podEnvPrefix+serviceName)
                .withNamespace(namespace)
                .endMetadata()
                .withNewSpec()
               // .withExternalTrafficPolicy("Local")
                .addNewPort()
                //内网端口
                .withPort(21)
                .withProtocol("TCP")
                .withName(portName)
                .withNodePort(nodePort)
                .endPort()

                .addNewPort()
                //内网端口
                .withPort(20080)
                .withName(portNginxName)
                .withProtocol("TCP")
                 .withNodePort(nodeNginxPort)
                .endPort()
                .withType(type)
                .addToSelector(LABELS_KEY, labelsValue).endSpec()
                .build();
        return kubes.getKubeclinet().services().create(build);
    }

    public static void main(String[] args) {

    }
    public void  create(String namespace,Integer vsftpdPort, Integer vsftpdNginxPort,Integer vsftpdSize, boolean isAnew){
        String podName="vsftpd";
        String labelsName="vsftpd";
        String portName="vsftpd";
        String portNginxName="vsftpdnginx";
        try {

            kubes.createNamespace(namespace);
            String pvcName =namespace + podName;
            kubes.createPVC(pvcName,namespace,nfsStorageClassName,vsftpdSize);
            createDeployment(namespace,podName,labelsName,harborImageEnvPrefix+image,portNginxName,portName,vsftpdPort,vsftpdNginxPort,isAnew);
            createService(namespace,podName,labelsName,21,podName,vsftpdPort,portNginxName,vsftpdNginxPort);
        }catch (Exception e){
            //harbor.org/tool/vsftp:3.0
            e.printStackTrace();
            kubes.getKubeclinet().apps().deployments().inNamespace(namespace).withName(podEnvPrefix+podName).delete();
            kubes.getKubeclinet().services().inNamespace(namespace).withName(podEnvPrefix+podName).delete();
        }

    }

}
