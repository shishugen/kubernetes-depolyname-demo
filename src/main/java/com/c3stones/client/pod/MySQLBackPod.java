package com.c3stones.client.pod;

import com.c3stones.client.BaseConfig;
import com.c3stones.client.Kubes;
import com.c3stones.constant.NfsTypeEnum;
import com.c3stones.entity.PodParameter;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @ClassName: MySQLPod
 * @Description: TODO
 * @Author: stone
 * @Date: 2021/1/26 10:57
 */
@Slf4j
@Component
public class MySQLBackPod extends BaseConfig {

    /***
     * labels
     */
    private  static  String LABELS_KEY = "app";

    private  static  String MYSQL_ROOT_PASSWORD = "123456";

    @Value("${MySQLBack.image}")
    private String image;

    @Autowired
    private  Kubes kubes;


    @Value("${pod.env.prefix}")
    private String podEnvPrefix;



    public  boolean createDeployment(PodParameter podParameter) {
        ResourceRequirements resource= new ResourceRequirements();
        Map<String,Quantity> map= new HashMap(1);
        map.put("memory",new Quantity("300M"));
        resource.setLimits(map);
        String policy ="IfNotPresent";
        Map<String,Quantity> stringQuantityMap= new HashMap(1);
        stringQuantityMap.put("memory",new Quantity(String.valueOf(300),"M"));
        resource.setRequests(stringQuantityMap);

        String namespace =  podParameter.getNamespace();
        String labelsName =  podParameter.getLabelsName();
        List<Volume> volumeList = new ArrayList<>();
        List<VolumeMount> volumeMountList = new ArrayList<>();
        Volume date = new VolumeBuilder().withName("date-config").withHostPath(new HostPathVolumeSourceBuilder().withNewPath("/etc/localtime").build()).build();
        volumeList.add(date);
        VolumeMount build1 = new VolumeMountBuilder().withName("date-config").withMountPath("/etc/localtime").build();
        String pvcName = "";
        String podName = "";
        volumeMountList.add(build1);
        if (NfsTypeEnum.K8S_DATA.getName().equals(podParameter.getNfsType())){
            podName = "mysqlbackk8s";
            pvcName =podParameter.getNamespace() + podName;
            kubes.createPVC(pvcName,namespace,nfsStorageClassName,podParameter.getNfsSize());
            Volume build = new VolumeBuilder().withName(pvcName)
                    .withPersistentVolumeClaim(new PersistentVolumeClaimVolumeSourceBuilder().withClaimName(pvcName).build()).build();
            volumeMountList.add(new VolumeMountBuilder().withName(pvcName).withMountPath("/db").build());
            volumeList.add(build);
        }else{
            pvcName =podParameter.getNamespace() + podParameter.getPodName();
            podName =  podParameter.getPodName();
            VolumeMount build2 = new VolumeMountBuilder().withName(pvcName).withMountPath("/db").build();
            volumeMountList.add(build2);
            Volume build = new VolumeBuilder()
                    .withName(pvcName)
                    .withNewNfs().withServer(podParameter.getNfsServer()).withPath(podParameter.getNfsPath()).endNfs()
                    .build();
            volumeList.add(build);
        }
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
                .addNewContainer().withName(podName).withImage(harborImageEnvPrefix+image)
                .withImagePullPolicy(policy)
                .addAllToVolumeMounts(volumeMountList)
                //.addToVolumeMounts(new VolumeMountBuilder().withName(pvcName).withMountPath("/db").build())
              //  .addToVolumeMounts(new VolumeMountBuilder().withName("date-config").withMountPath("/etc/localtime").build())
                .addToEnv(new EnvVarBuilder().withName("DB_DUMP_FREQ").withValue(podParameter.getBackDate().toString()).build())
                .addToEnv(new EnvVarBuilder().withName("DB_DUMP_TARGET").withValue("/db").build())
                .addToEnv(new EnvVarBuilder().withName("DB_SERVER").withValue(podParameter.getMysqlServer()).build())
                .addToEnv(new EnvVarBuilder().withName("DB_USER").withValue(podParameter.getMysqlUser()).build())
                .addToEnv(new EnvVarBuilder().withName("DB_PORT").withValue("3306").build())
                .addToEnv(new EnvVarBuilder().withName("DB_PASS").withValue(podParameter.getMysqlPwd().toString()).build())
                .addToEnv(new EnvVarBuilder().withName("BACK_DAY").withValue(podParameter.getBackDay().toString()).build())
                .withResources(resource)
                .endContainer()
                .addAllToVolumes(volumeList)
                //.addToVolumes(
                    //    new VolumeBuilder()
                    //            .withName("test-pv")
              //  .withNewNfs().withServer("10.49.0.10").withPath("/home/nfs/data/xuanyuan/ssg/").endNfs()
                            //    .build())
               // .addToVolumes(new VolumeBuilder().withName("date-config").withHostPath(new HostPathVolumeSourceBuilder().withNewPath("/etc/localtime").build()).build())
               // .addToVolumes(new VolumeBuilder().withName(pvcName)
                       // .withPersistentVolumeClaim(new PersistentVolumeClaimVolumeSourceBuilder().withClaimName(pvcName).build()).build())

                .endSpec().endTemplate().endSpec().build();
        kubes.getKubeclinet().apps().deployments().createOrReplace(newDeployment);
        return true;
    }

    //




    public void  create(PodParameter podParameter){
        String podName="mysqlback2asdfghassssssddfg";
        String labelsName="mysqlback2asdfghassssssddfg";
            podParameter.setImage("");
            podParameter.setLabelsName("mysqlback"+podParameter.getPodName());
            podParameter.setPodName("mysqlback"+podParameter.getPodName());
            createDeployment(podParameter);
    }




    public void reMySQLBack(PodParameter podParameter) {
        String podName="remysqlback";
        String labelsName="remysqlback";
        String portName="mysqlback";
        try {
            podParameter.setLabelsName(labelsName);
            podParameter.setPodName(podName);

            createDeploymentRe(podParameter);
        }catch (Exception e){
            e.printStackTrace();
            kubes.getKubeclinet().apps().deployments().inNamespace(podParameter.getNamespace()).withName(podEnvPrefix+podName).delete();
        }


    }
    public  boolean createDeploymentRe(PodParameter podParameter) {
        ResourceRequirements resource= new ResourceRequirements();
        Map<String,Quantity> map= new HashMap(1);
        map.put("memory",new Quantity("300M"));
        resource.setLimits(map);
        String policy ="IfNotPresent";
        Map<String,Quantity> stringQuantityMap= new HashMap(1);
        stringQuantityMap.put("memory",new Quantity(String.valueOf(300),"M"));
        resource.setRequests(stringQuantityMap);
        String pvcName =podParameter.getNamespace() + "mysqlback";
        String podName =  podParameter.getPodName();
        String namespace =  podParameter.getNamespace();
        String labelsName =  podParameter.getLabelsName();
        Map<String,String> nodeSelectorMap = isk8sArm();
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
                .addToNodeSelector(nodeSelectorMap)
                .addNewContainer().withName(podName).withImage(harborImageEnvPrefix+image)
                .withImagePullPolicy(policy)
                .addToVolumeMounts(new VolumeMountBuilder().withName(pvcName).withMountPath("/db").build())
                .addToVolumeMounts(new VolumeMountBuilder().withName("date-config").withMountPath("/etc/localtime").build())
              //  .addToEnv(new EnvVarBuilder().withName("DB_DUMP_FREQ").withValue(podParameter.getBackDate().toString()).build())
              //  .addToEnv(new EnvVarBuilder().withName("DB_DUMP_TARGET").withValue("/db").build())
                .addToEnv(new EnvVarBuilder().withName("DB_SERVER").withValue(podParameter.getMysqlServer()).build())
                .addToEnv(new EnvVarBuilder().withName("DB_USER").withValue(podParameter.getMysqlUser()).build())
                .addToEnv(new EnvVarBuilder().withName("DB_PORT").withValue("3306").build())
                .addToEnv(new EnvVarBuilder().withName("DB_PASS").withValue(podParameter.getMysqlPwd().toString()).build())
                .addToEnv(new EnvVarBuilder().withName("DB_RESTORE_TARGET").withValue("/db/"+podParameter.getBackFile()).build())
                .withResources(resource)
                .endContainer()
                .addToVolumes(new VolumeBuilder().withName("date-config").withHostPath(new HostPathVolumeSourceBuilder().withNewPath("/etc/localtime").build()).build())
                .addToVolumes(new VolumeBuilder().withName(pvcName)
                        .withPersistentVolumeClaim(new PersistentVolumeClaimVolumeSourceBuilder().withClaimName(pvcName).build()).build())
                .endSpec().endTemplate().endSpec().build();
        kubes.getKubeclinet().apps().deployments().createOrReplace(newDeployment);
        return true;
    }



    /**
     * 恢复本地数据库
     * @param podParameter
     */
    public void localReMySQLBack(PodParameter podParameter) {
        ResourceRequirements resource= new ResourceRequirements();
        Map<String,Quantity> map= new HashMap(1);
        map.put("memory",new Quantity("300M"));
        resource.setLimits(map);
        String policy ="IfNotPresent";
        Map<String,Quantity> stringQuantityMap= new HashMap(1);
        stringQuantityMap.put("memory",new Quantity(String.valueOf(300),"M"));
        resource.setRequests(stringQuantityMap);
        String podName =  podParameter.getPodName();
        String namespace =  podParameter.getNamespace();
        String labelsName =  podParameter.getPodName();
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
                .addNewContainer().withName(podName).withImage(harborImageEnvPrefix+image)
                .withImagePullPolicy(policy)
                .addToVolumeMounts(new VolumeMountBuilder().withName(podEnvPrefix+podParameter.getPodName()).withMountPath("/db/")
                        .build())
                .addToVolumeMounts(new VolumeMountBuilder().withName("date-config").withMountPath("/etc/localtime").build())
                .addToEnv(new EnvVarBuilder().withName("DB_SERVER").withValue(podParameter.getMysqlServer()).build())
                .addToEnv(new EnvVarBuilder().withName("DB_USER").withValue(podParameter.getMysqlUser()).build())
                .addToEnv(new EnvVarBuilder().withName("DB_PORT").withValue(podParameter.getMysqlPort().toString()).build())
                .addToEnv(new EnvVarBuilder().withName("DB_PASS").withValue(podParameter.getMysqlPwd().toString()).build())
                .addToEnv(new EnvVarBuilder().withName("DB_RESTORE_TARGET").withValue("/db/"+podParameter.getNfsFileName()).build())
                .withResources(resource)
                .endContainer()
                .addToVolumes(
                        new VolumeBuilder()
                                .withName(podEnvPrefix+podParameter.getPodName())
                                .withNewNfs().withServer(podParameter.getNfsServer()).withPath(podParameter.getNfsPath()).endNfs()
                                //.withNewNfs().withServer("10.49.0.10").withPath("/home/nfs/data/xuanyuan/app-ssg-app-ssgmysqlback-pvc-eb43f148-5bd0-423a-9ad5-3f42376889cc/").endNfs()
                                .build())
                .addToVolumes(new VolumeBuilder().withName("date-config").withHostPath(new HostPathVolumeSourceBuilder().withNewPath("/etc/localtime").build()).build())
                .endSpec().endTemplate().endSpec().build();
        kubes.getKubeclinet().apps().deployments().createOrReplace(newDeployment);
    }
}
