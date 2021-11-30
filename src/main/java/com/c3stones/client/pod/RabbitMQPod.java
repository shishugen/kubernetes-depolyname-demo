package com.c3stones.client.pod;

import com.c3stones.client.BaseConfig;
import com.c3stones.client.Kubes;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.rbac.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @ClassName: RabbitMQPod
 * @Description: TODO
 * @Author: stone
 * @Date: 2021/1/26 10:57
 */
@Slf4j
@Component
public class RabbitMQPod extends BaseConfig {


    @Value("${rabbitmq.image}")
    private String image;

    @Autowired
    private  Kubes kubes;
    /***
     * labels
     */
    private  static  String LABELS_KEY = "app";



    @Value("${pod.env.prefix}")
    private String podEnvPrefix;



    /***
     * 创建 pod
     * @param namespace
     * @param podName
     * @param image
     * @return
     */
    public  boolean create(String namespace, String podName, String labelsName , String image , Integer port,String portName ,Integer port2,String portName2 ){
        try{
            //
            String pvcName =namespace + podName;
            List<KeyToPath> paths = new ArrayList<>();
            KeyToPath keyToPath = new KeyToPath();
            keyToPath.setKey("enabled_plugins");
            keyToPath.setPath("enabled_plugins");
            paths.add(keyToPath);

            KeyToPath keyToPath2 = new KeyToPath();
            keyToPath2.setKey("rabbitmq.conf");
            keyToPath2.setPath("rabbitmq.conf");
            paths.add(keyToPath2);

            KeyToPath keyToPath3 = new KeyToPath();
            keyToPath3.setKey("rabbitmq-env.conf");
            keyToPath3.setPath("rabbitmq-env.conf");
            paths.add(keyToPath3);
            ResourceRequirements resource= new ResourceRequirements();
            Map<String,Quantity> map= new HashMap(2);
            //map.put("cpu",new Quantity("m"));
            map.put("memory",new Quantity("2000M"));
            resource.setLimits(map);
            Map<String,Quantity> stringQuantityMap= new HashMap(2);
            //stringQuantityMap.put("cpu",new Quantity(String.valueOf(500),"m"));
            stringQuantityMap.put("memory",new Quantity(String.valueOf(1000),"M"));
            resource.setRequests(stringQuantityMap);

            Pod pod = new PodBuilder().withNewMetadata().withName(podEnvPrefix+podName).withNamespace(namespace).addToLabels(LABELS_KEY, labelsName)
                    .endMetadata()
                    .withNewSpec()
                         .withContainers(new ContainerBuilder()
                            .withName(labelsName)
                                 .withResources(resource)
                            .withImage(image)
                               //  .withImagePullPolicy("Always")
                                // .withImagePullPolicy("IfNotPresent")
                                 .withVolumeMounts(new VolumeMountBuilder().withName(pvcName).withMountPath("/bitnami").build())
                               //  .withVolumeMounts(new VolumeMountBuilder().withName("rabbitmq-config").withMountPath("/opt/bitnami/rabbitmq/etc/rabbitmq/").build())
                                 .addToPorts(new ContainerPortBuilder().withName(portName).withContainerPort(port).build())
                            .addToPorts(new ContainerPortBuilder().withName(portName2).withContainerPort(port2).build())
                                 .addToEnv(new EnvVarBuilder().withName("RABBITMQ_USERNAME").withValue("admin").build())
                                 .addToEnv(new EnvVarBuilder().withName("RABBITMQ_PASSWORD").withValue("123456").build())
                                 .withVolumeMounts(new VolumeMountBuilder().withName("date-config").withMountPath("/etc/localtime").build())
                                 .build())
                    .withVolumes(new VolumeBuilder().withName(pvcName)
                           .withPersistentVolumeClaim(new PersistentVolumeClaimVolumeSourceBuilder().withClaimName(pvcName).build()).build())
                    .withVolumes(new VolumeBuilder().withName("date-config").withHostPath(new HostPathVolumeSourceBuilder().withNewPath("/etc/localtime").build()).build())



                  /* .mounts(new VolumeMountBuilder().withName("date-config")
                           .withMountPath("/etc/localtime").build()
                   )*/
                    .endSpec()
                    .build();
            Pod newPod = kubes.getKubeclinet().pods().create(pod);
            kubes.createPVC(pvcName,namespace,nfsStorageClassName,nfsMqStorageSize);
            System.out.println(newPod);
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
        return true;
    }


    public  boolean createDeployment(String namespace, String podName, String labelsName, String image, Integer port, String portName, Integer port2, String portName2, boolean isAnew) {
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
        kubes.createPVC(pvcName,namespace,nfsStorageClassName,nfsMqStorageSize);
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
                .withName(podName)
                .withImage(image)
                .withImagePullPolicy(policy)
                .withVolumeMounts(new VolumeMountBuilder().withName(pvcName).withMountPath("/bitnami/").build())
                .addToPorts(new ContainerPortBuilder().withName(portName).withContainerPort(port).build())
                .addToPorts(new ContainerPortBuilder().withName(portName2).withContainerPort(port2).build())
                .addToEnv(new EnvVarBuilder().withName("RABBITMQ_USERNAME").withValue("admin").build())
                .addToEnv(new EnvVarBuilder().withName("RABBITMQ_PASSWORD").withValue("123456").build())
                .withResources(resource)
                .endContainer()
                .withVolumes(new VolumeBuilder().withName(pvcName).withPersistentVolumeClaim(new PersistentVolumeClaimVolumeSourceBuilder().withClaimName(pvcName).build()).build())

                //  .withVolumes(new VolumeBuilder().withName("date-config").withHostPath(new HostPathVolumeSourceBuilder().withNewPath("/etc/localtime").build()).build())
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
    public  Service createService(String namespace, String serviceName,  String labelsValue , Integer port,String portName,
    Integer port2,String portName2 ){
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
                .withName(portName)
             //   .withNodePort(nodePort)
                .endPort()

                .addNewPort()
                //内网端口
                .withPort(port2)
                .withName(portName2)
                .withProtocol("TCP")
             //   .withNodePort(nodePort2)
                .endPort()

                .withType(type)
                .addToSelector(LABELS_KEY, labelsValue).endSpec()
                .build();
        return kubes.getKubeclinet().services().create(build);
    }

    //user  bitnami
    public static void main(String[] args) {
        String namespace="test2";
        String podName="rabbitmq";
        String configName="rabbitmq";
       // configMap(namespace,configName,configName);
        String labelsName="rabbitmq";
        String image="10.49.0.9/app-tool/bitnami-rabbitmq:3.8.11";
        String portName="rabbitmq";
     //   kubes.createNamespace(namespace);
          //30000-32767
        Integer nodePort = 30229;
        Integer nodePort2 = 30228;
       // create(namespace,podName,labelsName,image,15672,portName+1,5672,portName+2);
       // createService(namespace,podName,labelsName,15672,portName+1,nodePort,5672,portName+2,nodePort2);

    }

    public  void createRabbitmq(String namespace, boolean isAnew){
        String podName="rabbitmq";
        String labelsName="rabbitmq";
        String portName="rabbitmq";
        kubes.createNamespace(namespace);
     //   configMap(namespace);
        try {
            createDeployment(namespace,podName,labelsName,harborImageEnvPrefix+image,15672,portName+1,5672,portName+2,isAnew);
            createService(namespace,podName,labelsName,15672,portName+1,5672,portName+2);
        }catch (Exception e){
            e.printStackTrace();
            kubes.getKubeclinet().apps().deployments().inNamespace(namespace).withName(podEnvPrefix+podName).delete();
            kubes.getKubeclinet().services().inNamespace(namespace).withName(podEnvPrefix+podName).delete();
        }
    }



    public  void configMap(String namespace){
        ConfigMap configMap = new ConfigMapBuilder()
                .withNewMetadata()
                .withName("rabbitmq-config")
                .withNamespace(namespace)
                .endMetadata()
                .addToData("enabled_plugins","[rabbitmq_management,rabbitmq_peer_discovery_k8s]")
                .addToData("rabbitmq-env.conf","HOME=/opt/bitnami/rabbitmq/.rabbitmq\n" +
                        "NODE_PORT=5672\n" +
                        "NODENAME=rabbit@localhost")
                .addToData("rabbitmq.conf",
                        " HOME=/opt/bitnami/rabbitmq/.rabbitmq\n" +
                                "NODE_PORT=5672\n" +
                                "NODENAME=rabbit@localhost\n" +
                                "I have no name!@rabbitmq:/opt/bitnami/rabbitmq/etc/rabbitmq$ cat rabbitmq.conf \n" +
                                "## Clustering\n" +
                                "cluster_partition_handling = ignore\n" +
                                "\n" +
                                "## Defaults\n" +
                                "# During the first start, RabbitMQ will create a vhost and a user\n" +
                                "# These config items control what gets created\n" +
                                "default_permissions.configure = .*\n" +
                                "default_permissions.read = .*\n" +
                                "default_permissions.write = .*\n" +
                                "default_vhost = /\n" +
                                "default_user = admin\n" +
                                "\n" +
                                "## Networking\n" +
                                "listeners.tcp.default = 5672\n" +
                                "\n" +
                                "## Management\n" +
                                "management.tcp.ip = 0.0.0.0\n" +
                                "management.tcp.port = 15672\n" +
                                "\n" +
                                "## Resource limits\n" +
                                "# Set a free disk space limit relative to total available RAM\n" +
                                "disk_free_limit.relative = 1.0\n"
                ).build();
        kubes.getKubeclinet().configMaps().createOrReplace(configMap);
    }






    public  void serviceAccount(String namespace){
        ServiceAccount serviceAccount = new ServiceAccountBuilder()
                .withNewMetadata()
                .withName("rabbitmq")
                .withNamespace(namespace).endMetadata()
                .build();
        kubes.getKubeclinet().serviceAccounts().create(serviceAccount);
    }
    public  void role(String namespace){
        Role role = new RoleBuilder()
                .withApiVersion("rbac.authorization.k8s.io/v1")
                .withNewMetadata()
                .withNamespace(namespace)
                .withName("endpoint-reader")
                .endMetadata()
                .addToRules( new PolicyRuleBuilder()
                        .addNewApiGroup("")
                        .addToResources("endpoints")
                        .addToVerbs("get").build()).build();
        kubes.getKubeclinet().rbac().roles().create(role);
    }

    public  void roleBinding(String namespace){
        RoleBinding role = new RoleBindingBuilder()
                .withApiVersion("rbac.authorization.k8s.io/v1")
                .withNewMetadata()
                .withName("endpoint-reader")
                .withNamespace(namespace)
                .endMetadata()
                .addToSubjects(new SubjectBuilder().withKind("ServiceAccount")
                        .withName("rabbitmq").build())
                .withNewRoleRef().withApiGroup("rbac.authorization.k8s.io")
                .withKind("Role")
                .withName("endpoint-reader").endRoleRef().build();
        kubes.getKubeclinet().rbac().roleBindings().create(role);
    }





}
