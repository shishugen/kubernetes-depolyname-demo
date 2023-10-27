package com.c3stones.client.pod;

import com.c3stones.client.BaseConfig;
import com.c3stones.client.Kubes;
import com.c3stones.exception.KubernetesException;
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
 * @ClassName: FastdfsPod
 * @Description: TODO
 * @Author: stone
 * @Date: 2021/1/26 10:57
 */
@Slf4j
@Component
public class FastdfsPod  extends BaseConfig {

    /***
     * labels
     */
    private  static  String LABELS_KEY = "app";

    private  static  String MYSQL_ROOT_PASSWORD = "123456";


    @Autowired
    private  Kubes kubes;

    @Value("${fdfs.image}")
    private String image;

    @Value("${pod.env.prefix}")
    private String podEnvPrefix;

    public  void tracker(String namespace , String image,Integer nodePort ,String podName){
      //  String image = "";
        // String image = "10.49.0.9/base/ssg-fastdfs:1.0";
        String pvcName =namespace + podName;
        ResourceRequirements resource= new ResourceRequirements();
        Map<String,Quantity> map= new HashMap(2);
        //map.put("cpu",new Quantity("m"));
        map.put("memory",new Quantity("2000M"));
        resource.setLimits(map);
        Map<String,Quantity> stringQuantityMap= new HashMap(2);
        //stringQuantityMap.put("cpu",new Quantity(String.valueOf(500),"m"));
        stringQuantityMap.put("memory",new Quantity(String.valueOf(1000),"M"));
        resource.setRequests(stringQuantityMap);


        kubes.createPVC(pvcName,namespace,nfsStorageClassName,nfsFdfsStorageSize);
        Pod pod = new PodBuilder().withNewMetadata().withName(podEnvPrefix+podName).withNamespace(namespace).addToLabels(LABELS_KEY, podName).endMetadata()
                .withNewSpec().withContainers(new ContainerBuilder()
                        .withName(podName)
                        .withImage(image)
                        .withResources(resource)
                       // .withImagePullPolicy("Always")
                       // .withImagePullPolicy("IfNotPresent")
                        .withVolumeMounts(new VolumeMountBuilder().withName(pvcName).withMountPath("/home/fdfs_storage/").build())
                        .withCommand("/home/start2.sh")
                        //    .withCommand("/usr/sbin/init")
                        .withPorts(new ContainerPortBuilder().withContainerPort(80).withName("nginx").build())
                        .withPorts(new ContainerPortBuilder().withContainerPort(22122).withName("fdfs2").build())
                        .addToEnv(new EnvVarBuilder().withName("TRACKER_SERVER")
                                .withValueFrom(new EnvVarSourceBuilder().withFieldRef(
                                        new ObjectFieldSelectorBuilder().withFieldPath("status.podIP").build()).build())
                                .build()
                        )
                       // .withVolumeMounts(new VolumeMountBuilder().withName("date-config").withMountPath("/etc/localtime").build())
                        .build())
               // .withVolumes(new VolumeBuilder().withName("date-config").withHostPath(new HostPathVolumeSourceBuilder().withNewPath("/etc/localtime").build()).build())
                .withVolumes(new VolumeBuilder().withName(pvcName)
                        .withPersistentVolumeClaim(new PersistentVolumeClaimVolumeSourceBuilder().withClaimName(pvcName).build()).build())
                .endSpec().build();

        Pod newPod = kubes.getKubeclinet().pods().create(pod);
        createService(namespace,nodePort);
    }

    public  boolean createDeployment(String namespace, String podName, String labelsName, String image, Integer port, String portName, boolean isAnew) {
        ResourceRequirements resource= new ResourceRequirements();
        Map<String,Quantity> map= new HashMap(1);
        map.put("memory",new Quantity("1000M"));
        resource.setLimits(map);
        String policy ="IfNotPresent";
        if (isAnew){
            policy ="Always";
        }
        Map<String,Quantity> stringQuantityMap= new HashMap(1);
        stringQuantityMap.put("memory",new Quantity(String.valueOf(500),"M"));
        resource.setRequests(stringQuantityMap);
        String pvcName =namespace + podName;
        kubes.createPVC(pvcName,namespace,nfsStorageClassName,nfsFdfsStorageSize);
        Map<String,String> isk8sArm = isk8sArm();
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
                .addToNodeSelector(isk8sArm)
                .addNewContainer().withName(podName).withImage(image).withImagePullPolicy(policy)
                .withCommand("/home/start2.sh")
                .withVolumeMounts(new VolumeMountBuilder().withName(pvcName).withMountPath("/home/fdfs_storage/").build())
                .withPorts(new ContainerPortBuilder().withContainerPort(80).withName("nginx").build())
                .withPorts(new ContainerPortBuilder().withContainerPort(22122).withName("fdfs2").build())
                .addToEnv(new EnvVarBuilder().withName("TRACKER_SERVER")
                        .withValueFrom(new EnvVarSourceBuilder().withFieldRef(
                                new ObjectFieldSelectorBuilder().withFieldPath("status.podIP").build()).build())
                        .build()
                )
                .withResources(resource)
                .endContainer()
                .addNewVolume()
                .withName("date-config").withNewHostPath().withNewPath("/etc/localtime").endHostPath()
                .endVolume()
                .withVolumes(new VolumeBuilder().withName(pvcName)
                        .withPersistentVolumeClaim(new PersistentVolumeClaimVolumeSourceBuilder().withClaimName(pvcName).build()).build())
                .endSpec().endTemplate().endSpec().build();
        kubes.getKubeclinet().apps().deployments().createOrReplace(newDeployment);
        createService(namespace,port);
        return true;
    }

    public  Service createService(String namespace ,Integer nodePort) {
        if(nodePort > 30000 || nodePort < 32767){
            log.error("端口为 30000-32767   nodePort : {}",nodePort);
            new KubernetesException("端口为30000-32767  端口异常"+nodePort);
        }
        List<ServicePort> servicePortList = new ArrayList<>();
        ServicePort servicePort =  new ServicePort();
        servicePort.setProtocol("TCP");
        servicePort.setPort(22122);
        servicePort.setName("fdfs2");
        servicePortList.add(servicePort);

        ServicePort servicePort2 =  new ServicePort();
        servicePort2.setProtocol("TCP");
        servicePort2.setPort(80);
        servicePort2.setNodePort(nodePort);
        servicePort2.setName("nginx");
        servicePortList.add(servicePort2);
        Service newService = new ServiceBuilder()
                .withNewMetadata()
                .withName(podEnvPrefix+"fdfs")
                .withNamespace(namespace)
                .endMetadata()
                .withNewSpec()
                .withPorts(servicePortList)
                .addToSelector(LABELS_KEY,"fdfs")
                .withType("NodePort")
                .endSpec()
                .build();
        return kubes.getKubeclinet().services().create(newService);
    }

    public static void main(String[] args) {
     //   tracker("fdfs","10.49.0.9/base/fdfs-nginx-single:5.5");
      //  createService("app-sys");
    }

    public  void  createT(String namespace, Integer nodePort, boolean isAnew){
        String podName = "fdfs";
        try {
            createDeployment(namespace,podName,podName,harborImageEnvPrefix+image,nodePort,podName,isAnew);
        }catch (Exception e){
            e.printStackTrace();
            kubes.getKubeclinet().apps().deployments().inNamespace(namespace).withName(podEnvPrefix+podName).delete();
            kubes.getKubeclinet().services().inNamespace(namespace).withName(podEnvPrefix+podName).delete();
        }

    }

}
