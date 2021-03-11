package com.c3stones.client;

import com.c3stones.entity.Namespaces;
import com.c3stones.util.KubeUtils;
import com.sun.org.apache.xml.internal.utils.NameSpace;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentList;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.ExecWatch;
import io.fabric8.kubernetes.client.dsl.LogWatch;
import io.fabric8.kubernetes.client.dsl.ServiceResource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.omg.CORBA.portable.ValueBase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @ClassName: Kubes
 * @Description: TODO
 * @Author: stone
 * @Date: 2021/1/26 10:09
 */
@Component
@Slf4j
public class Kubes {

    /***
     * labels
     */
    private  static  String LABELS_KEY = "app";

    @Value("${pod.namespace.prefix}")
    private String podNamespacePrefix;

    @Value("${kubernetes.config.file}")
    private String kubernetesConfigFile;
    @Value("${pod.app.prefix}")
    private String podAppPrefix;


    public  KubernetesClient getKubeclinet() {
        System.setProperty(Config.KUBERNETES_KUBECONFIG_FILE, kubernetesConfigFile);
        Config config = new ConfigBuilder()
                .build();
        return new DefaultKubernetesClient(config);
    }



    /**
     * 检查 命名空间
     * */
    public  boolean checkNamespace(String name){
        return getKubeclinet().namespaces().list().getItems().
                stream().anyMatch(a -> name.equals(a.getMetadata().getName()));
    }
    /**
     * 创建 命名空间
     * */
    public  boolean createNamespace(String name){
        System.out.println("createNamespace");
        try{
            if (checkNamespace(name)){
                return true;
            }
            Map<String,String> LabelsMap = new HashMap<>(1);
            LabelsMap.put("app",name);
            Namespace namespace = new NamespaceBuilder().withNewMetadata().withName(name).withLabels(LabelsMap).endMetadata().build();
            getKubeclinet().namespaces().create(namespace);
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
        return true;
    }



    /***
     * 创建 pod
     * @param namespace
     * @param podName
     * @param image
     * @return
     */
    public  boolean createPod(String namespace, String podName, String image ,String nacos,String nacosNamespace){
            Pod pod = new PodBuilder().withNewMetadata().withName(podName).withNamespace(namespace).addToLabels(LABELS_KEY, podName).endMetadata()
                    .withNewSpec().withContainers(new ContainerBuilder()
                            .withName(podName)
                            .withImage(image)
                            .withImagePullPolicy("Always")
                            .addToEnv(new EnvVarBuilder().withName("NAMESPACE").withValue(nacosNamespace).build())
                            .addToEnv(new EnvVarBuilder().withName("NACOS_PORT").withValue("8848").build())
                            .addToEnv(new EnvVarBuilder().withName("NACOS_IP").withValue(nacos).build())
                            .build())
                    .endSpec().build();
            Pod newPod = getKubeclinet().pods().create(pod);
        return true;
    }



        /***
         * 创建 pod
         * @param namespace
         * @param podName
         * @param image
         * @return
         */
        public  boolean createPod(String namespace, String podName, String image , Integer port,String portName,String nacos,String nacosNamespace){
               /* ContainerPort[] arr = new ContainerPort[ports.size()];
                for (int i = 0 ; i < ports.size(); i++ ) {
                    ContainerPort build = new ContainerPortBuilder().withName(podName).withContainerPort(ports.get(i)).build();
                    arr[i] = build;
                }*/
                Pod pod = new PodBuilder().withNewMetadata().withName(podName).withNamespace(namespace).addToLabels(LABELS_KEY, portName).endMetadata()
                        .withNewSpec().withContainers(new ContainerBuilder()
                                .withName(podName)
                                .withImage(image)
                                .withImagePullPolicy("Always")
                                .addToEnv(new EnvVarBuilder().withName("NAMESPACE").withValue(nacosNamespace).build())
                                .addToEnv(new EnvVarBuilder().withName("NACOS_PORT").withValue("8848").build())
                                .addToEnv(new EnvVarBuilder().withName("NACOS_IP").withValue(nacos).build())
                                .addToPorts(new ContainerPortBuilder().withName(portName).withContainerPort(port).build())
                                .build())
                        .endSpec().build();
                Pod newPod = getKubeclinet().pods().create(pod);
                System.out.println(newPod);
            return true;
        }



    public  Service createService(String namespace, String serviceName, Integer port,Integer nodePort){
        String type = "NodePort";
        Service build = new ServiceBuilder()
                .withNewMetadata()
                .withName(serviceName)
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
                .addToSelector(LABELS_KEY, serviceName).endSpec()
                .build();
        return getKubeclinet().services().create(build);
    }
    public  Service createService(String namespace, String serviceName, Integer port){
        String type = "NodePort";
        Service build = new ServiceBuilder()
                .withNewMetadata()
                .withName(serviceName)
                .withNamespace(namespace)
                .endMetadata()
                .withNewSpec()
                .addNewPort()
                //内网端口
                .withPort(port)
                .withProtocol("TCP")
                .endPort()
                .withType(type)
                .addToSelector(LABELS_KEY, serviceName).endSpec()
                .build();
        return getKubeclinet().services().create(build);
    }




    public   List<Pod> findPod(String namesapce){
       return getKubeclinet().pods().inNamespace(namesapce).list().getItems();

    }
    public   List<Pod> findPod(){
       return getKubeclinet().pods().list().getItems();

    }
    public   Service findService(String namesapce,String serviceName){
        ServiceResource<Service, DoneableService> serviceDoneableServiceServiceResource = getKubeclinet().services().inNamespace(namesapce).withName(serviceName);
      if(serviceDoneableServiceServiceResource != null &&
              serviceDoneableServiceServiceResource.get()!= null){
          return serviceDoneableServiceServiceResource.get();
      }
        return null;
    }
    public  Boolean deletePod(String namesapce,String podName){
        return getKubeclinet().pods().inNamespace(namesapce).withName(podName).delete();
    }

    public  Boolean deleteService(String namesapce,String podName){
        return getKubeclinet().services().inNamespace(namesapce).withName(podName).delete();
    }
    public  Boolean deleteConf(String namesapce,String podName){
        return getKubeclinet().configMaps().inNamespace(namesapce).withName(podName).delete();
    }
    public  Boolean deleteNamespace(String namesapce){
        return getKubeclinet().namespaces().withName(namesapce).delete();
    }
    public  Boolean deleteDeployments(String name,String namesapce){
        return getKubeclinet().apps().deployments().inNamespace(namesapce).withName(name).delete();
    }

    public  List<Namespaces> getNamespace(){
        List<Namespaces> list = new ArrayList<>();
        List<Namespace> namespaceList = getKubeclinet().namespaces().list().getItems();
        for (Namespace name : namespaceList) {
            String name1 = name.getMetadata().getName();
            if (name1.contains(podNamespacePrefix)) {
                list.add(new Namespaces(name1,KubeUtils.StringFormatDate(name.getMetadata().getCreationTimestamp())));
            }
        }
        return list;
    }



    public  Boolean checkPort(Integer cport){
         AtomicBoolean falg = new AtomicBoolean(false);
        List<Service> items = getKubeclinet().services().list().getItems();
        items.forEach(service -> {
            List<ServicePort> ports = service.getSpec().getPorts();
            ports.forEach(port->{
                Integer nodePort = port.getNodePort();
                if(cport.equals(nodePort)){
                    falg.set(true);
                }
            });
        });
        return falg.get();
    }

    /**
     *
     * @param name
     * @param namespace
     * @param storageClassName
     * @param storageSize
     * @return
     */
    public  PersistentVolumeClaim createPVC(String name, String namespace, String storageClassName, Integer storageSize ) {
        if(check(name,namespace)){
            return get(name,namespace);
        }
        Map<String,Quantity> map = new HashMap(1);
        map.put("storage",new Quantity(String.valueOf(storageSize),"G"));
        PersistentVolumeClaim build = new PersistentVolumeClaimBuilder()
                .withNewMetadata()
                .withName(name)
                .withNamespace(namespace)
                .endMetadata()
                .withNewSpec()
                .withAccessModes("ReadOnlyMany")
                .withNewResources()
                .withRequests(map)
                .endResources()
                .withStorageClassName(storageClassName)
                .endSpec()
                .build();
        return getKubeclinet().persistentVolumeClaims().create(build);
    }

    public  boolean check(String name, String namespace) {
        return  get(name,namespace) != null ? true : false;
    }

    public  PersistentVolumeClaim get(String name, String namespace) {
        return getKubeclinet().persistentVolumeClaims().inNamespace(namespace).withName(name).get();
    }



    public  boolean createDeployment(String namespace, String deploymentName, String appName, Integer replicas, String image, Integer port,String randomPortName ,String nacos , String nacosNamespace) {
        Container container = createContainer(appName, image,port,randomPortName,nacos , nacosNamespace,null);
        Deployment newDeployment = new DeploymentBuilder()
                .withNewMetadata()
                .withName(podAppPrefix+appName)
                .withNamespace(namespace)
                .endMetadata()
                .withNewSpec()
                .withNewSelector()
                .addToMatchLabels(LABELS_KEY, randomPortName)
                .endSelector()
                .withReplicas(replicas)
                .withNewTemplate()
                .withNewMetadata()
                .addToLabels(LABELS_KEY, randomPortName)
                .endMetadata()
                .withNewSpec()
                .withContainers(container)
               // .addNewVolume()
               // .endVolume()
                .endSpec().endTemplate().endSpec().build();
        getKubeclinet().apps().deployments().createOrReplace(newDeployment);
        return true;
    }

    public  boolean createDeployment(String namespace, String deploymentName, String appName, Integer replicas, String image, Integer port,String randomPortName ,String nacos , String nacosNamespace,String pvcName) {
        Container container = createContainer(appName, image,port,randomPortName,nacos , nacosNamespace,pvcName);
        Deployment newDeployment = new DeploymentBuilder()
                .withNewMetadata()
                .withName(podAppPrefix+appName)
                .withNamespace(namespace)
                .endMetadata()
                .withNewSpec()
                .withNewSelector()
                .addToMatchLabels(LABELS_KEY, randomPortName)
                .endSelector()
                .withReplicas(replicas)
                .withNewTemplate()
                .withNewMetadata()
                .addToLabels(LABELS_KEY, randomPortName)
                .endMetadata()
                .withNewSpec()
                .withContainers(container)
                .addNewVolume()
                .withName(pvcName)
                .withPersistentVolumeClaim(new PersistentVolumeClaimVolumeSourceBuilder().withClaimName(pvcName).build())
                .endVolume()
                .endSpec().endTemplate().endSpec().build();
        getKubeclinet().apps().deployments().createOrReplace(newDeployment);
        return true;
    }


    /**
     *
     * @param appName
     * @param image
     * @param cpu
     * @return
     */
    private  Container createContainer(String appName,String image,Integer ports,String serviceName,String nacos ,String nacosNamespace,String pvcName){
        log.info("ports :  {},serviceName  : {}",ports,serviceName);

        Container container = new  Container();
        container.setName(appName);
        container.setImage(image);

        List<EnvVar> env = new ArrayList<>();
        EnvVar envVar = new EnvVar();
        envVar.setName("NAMESPACE");
        envVar.setValue(nacosNamespace);
        env.add(envVar);

        EnvVar envVar2 = new EnvVar();
        envVar2.setName("NACOS_PORT");
        envVar2.setValue("8848");
        env.add(envVar2);

        EnvVar envVar3 = new EnvVar();
        envVar3.setName("NACOS_IP");
        envVar3.setValue(nacos);
        env.add(envVar3);
        container.setEnv(env);
        container.setImagePullPolicy("Always");

        if(pvcName != null){
            List<VolumeMount> volumeMounts = new ArrayList();
            VolumeMount volumeMount = new VolumeMount();
            volumeMount.setName(pvcName);
            volumeMount.setMountPath("/tmp/");
            volumeMounts.add(volumeMount);
            container.setVolumeMounts(volumeMounts);
        }


        SecurityContext securityContext = new SecurityContext();
        securityContext.setPrivileged(true);
        container.setSecurityContext(securityContext);
        if(ports != null ){
            List<ContainerPort> portslist = new ArrayList<>();
            ContainerPort containerPort = new ContainerPort();
            containerPort.setName(serviceName);
            containerPort.setContainerPort(ports);
            portslist.add(containerPort);
            container.setPorts(portslist);
        }
        return container;
    }

    @SneakyThrows
    public static void main(String[] args) {

        System.setProperty(Config.KUBERNETES_KUBECONFIG_FILE, "E:\\dockerfile\\kube-test-config");
        Config config = new ConfigBuilder()
                .build();
        DefaultKubernetesClient kubernetesClient = new DefaultKubernetesClient(config);

        String namespace = "app-sys";
        String pvcName = "app-sys";
        String podName = "test-pv3";
        String image = "10.49.0.9/base/system:1.0";
        boolean readOnly =false; //false 可写可读，true 只读

        Map<String,Quantity> map = new HashMap(1);
        map.put("storage",new Quantity(String.valueOf("2"),"G"));
        PersistentVolumeClaim build = new PersistentVolumeClaimBuilder()
                .withNewMetadata()
                .withName(pvcName)
                .withNamespace(namespace)
                .endMetadata()
                .withNewSpec()
                .withAccessModes("ReadOnlyMany")
                .withNewResources()
                .withRequests(map)
                .endResources()
                .withStorageClassName("xuanyuan-nfs")
                .endSpec()
                .build();
      //  kubernetesClient.persistentVolumeClaims().create(build);


        Pod pod = new PodBuilder().withNewMetadata().withName(podName).withNamespace(namespace).addToLabels(LABELS_KEY, podName).endMetadata()
                .withNewSpec().withContainers(new ContainerBuilder()
                        .withName(podName)
                        .withImage(image)
                        .withNewWorkingDir("/data")
                        .addNewVolumeMount()
                        .withName("test-pv")
                        .withMountPath("/home/")
                        .endVolumeMount()
                        .build())
                       .addNewVolume().withName("test-pv")
                       .withNewPersistentVolumeClaim(pvcName,readOnly).endVolume()
                .endSpec().build();

        Pod newPod = kubernetesClient.pods().create(pod);


        //createDeployment(namespace, namespace, podName, 2, image, null);

    }

}
