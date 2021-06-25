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

import java.util.ArrayList;
import java.util.List;

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
        kubes.createPVC(pvcName,namespace,nfsStorageClassName,nfsFdfsStorageSize);
        Pod pod = new PodBuilder().withNewMetadata().withName(podEnvPrefix+podName).withNamespace(namespace).addToLabels(LABELS_KEY, podName).endMetadata()
                .withNewSpec().withContainers(new ContainerBuilder()
                        .withName(podName)
                        .withImage(image)
                       // .withImagePullPolicy("Always")
                        .withImagePullPolicy("IfNotPresent")
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
                        .build())
                .withVolumes(new VolumeBuilder().withName(pvcName)
                        .withPersistentVolumeClaim(new PersistentVolumeClaimVolumeSourceBuilder().withClaimName(pvcName).build()).build())
                .endSpec().build();
        Pod newPod = kubes.getKubeclinet().pods().create(pod);
        createService(namespace,nodePort);
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

    public  void  createT(String namespace,Integer nodePort){
        String podName = "fdfs";
        try {
            tracker(namespace,harborImageEnvPrefix+image,nodePort,podName);
        }catch (Exception e){
            e.printStackTrace();
            kubes.getKubeclinet().pods().inNamespace(namespace).withName(podEnvPrefix+podName).delete();
            kubes.getKubeclinet().services().inNamespace(namespace).withName(podEnvPrefix+podName).delete();
        }

    }

}
