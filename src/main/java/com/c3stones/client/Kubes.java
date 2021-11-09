package com.c3stones.client;

import com.c3stones.common.Response;
import com.c3stones.entity.Namespaces;
import com.c3stones.util.KubeUtils;
import com.c3stones.util.OpenFileUtils;
import com.sun.org.apache.xml.internal.utils.NameSpace;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.*;
import io.fabric8.kubernetes.api.model.metrics.v1beta1.NodeMetricsList;
import io.fabric8.kubernetes.api.model.metrics.v1beta1.PodMetrics;
import io.fabric8.kubernetes.api.model.metrics.v1beta1.PodMetricsList;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.*;
import io.fabric8.kubernetes.client.dsl.internal.PodMetricOperationsImpl;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.omg.CORBA.portable.ValueBase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

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

   /* @Value("${kubernetes.config.file:/root/.kube/config}")
    private String kubernetesConfigFile;*/
    @Value("${pod.app.prefix}")
    private String podAppPrefix;


    public  KubernetesClient getKubeclinet() {
        String file = null;
        try {
            file = OpenFileUtils.readFileByChars(getHomeConfigFile().getPath());
            if(StringUtils.isNotBlank(file)) {
                return new DefaultKubernetesClient(Config.fromKubeconfig(file));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 获取 k8s IP
     * @return
     */
    public  String getK8SNodeIp() {
        List<Pod> items = getKubeclinet().pods().inNamespace("kube-system").list().getItems();
        for (Pod pod : items){
            String hostIP = pod.getStatus().getHostIP();
            System.out.println("hostIP"+hostIP);
            return hostIP;
        }
        return null;
    }



    /**
     *
     * @param nodePort  true存在，false不存在
     * @return
     */
    public boolean checkSvc(Integer nodePort){
        AtomicBoolean falg = new AtomicBoolean(false);
        List<Service> items = getKubeclinet().services().list().getItems();
        items.forEach(a->{
            List<ServicePort> ports = a.getSpec().getPorts();
            ports.forEach(b->{
                Integer nodePort1 = b.getNodePort();
                if(nodePort1 != null && nodePort1.equals(nodePort)){
                    falg.set(true);
                    return;
                }
            });
        });
        return falg.get();
    }


    /**
     * 检查 命名空间
     * */
    public  boolean checkdeployname(String namespace ,String name){
        Deployment deployment = getKubeclinet().apps().deployments().inNamespace(namespace).withName(name).get();
        return deployment == null ? false : true;
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



    public  Service createService(String namespace, String serviceName, Integer port,Integer nodePort,boolean isNginxEnv){
        Map<String,String> labels = new HashMap<>();
        labels.put(LABELS_KEY,serviceName);
        if(port.equals(8888) || isNginxEnv){
            labels.put(BaseConfig.GATEWAY_API_KEY,BaseConfig.GATEWAY_API_VALUE);
        }
        String type = "NodePort";
        Service build = new ServiceBuilder()
                .withNewMetadata()
                .withName(serviceName)
                .withNamespace(namespace)
                .withLabels(labels)
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
        return getKubeclinet().services().createOrReplace(build);
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
    public  ServiceList findService(String namesapce){
        NonNamespaceOperation<Service, ServiceList, DoneableService, ServiceResource<Service, DoneableService>> serviceServiceList = getKubeclinet().services().inNamespace(namesapce);
        ServiceList list = serviceServiceList.list();
        if(serviceServiceList != null &&
                list != null){
            return list;
        }
        return null;
    }

    public  Boolean deletePod(String namesapce,String podName){
        return getKubeclinet().pods().inNamespace(namesapce).withName(podName).delete();
    }

    public  Boolean deleteService(String namesapce,String podName){
        return getKubeclinet().services().inNamespace(namesapce).withName(podName).delete();
    }
    public  Boolean deletePvc(String namesapce,String name){
        return getKubeclinet().persistentVolumeClaims().inNamespace(namesapce).withName(name).delete();
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
        return getKubeclinet().persistentVolumeClaims().createOrReplace(build);
    }
    /**
     *
     * @param name
     * @param namespace
     * @param storageClassName
     * @param storageSize
     * @return
     */
    public  PersistentVolume createPV(String name, String namespace, String storageClassName ,Integer storageSize) {
        Map<String,Quantity> map = new HashMap(1);
        map.put("storage",new Quantity(String.valueOf(storageSize),"G"));
        PersistentVolume build = new PersistentVolumeBuilder()
                .withNewMetadata()
                .withName(name)
                .withNamespace(namespace)
                .endMetadata()
                .withNewSpec()
                .withCapacity(map)
                .withAccessModes("ReadOnlyMany")
                .withStorageClassName(storageClassName)
                .withNewNfs().withServer("192.168.0.218").withPath("/xuanyuan/nfs/data/test2/").endNfs()
                .endSpec()
                .build();
        return getKubeclinet().persistentVolumes().createOrReplace(build);
    }

    public  boolean check(String name, String namespace) {
        return  get(name,namespace) != null ? true : false;
    }

    public  PersistentVolumeClaim get(String name, String namespace) {
        return getKubeclinet().persistentVolumeClaims().inNamespace(namespace).withName(name).get();
    }



    public  boolean createDeployment(String namespace, String deploymentName, String appName, Integer replicas, String image, Integer port,String randomPortName ,
                                     String nacos , String nacosNamespace,Integer memoryXmx,Integer memoryXms,String pvcLogs,boolean isHealth) {
        Container container =
                createContainer(appName, image,port,randomPortName,nacos , nacosNamespace,null, memoryXmx, memoryXms,pvcLogs,isHealth);
        Map<String,String> labels = new HashMap<>();
        labels.put(LABELS_KEY,randomPortName);
        if(port != null && port.equals(8888)){
            labels.put(BaseConfig.GATEWAY_API_KEY,BaseConfig.GATEWAY_API_VALUE);
        }
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
                //.addToLabels(LABELS_KEY, randomPortName)
                .addToLabels(labels)
                .endMetadata()
                .withNewSpec()
                .withContainers(container)
                .addNewVolume()
                .withName(pvcLogs)
                .withPersistentVolumeClaim(new PersistentVolumeClaimVolumeSourceBuilder().withClaimName(pvcLogs).build())
                .endVolume()
                .addNewVolume()
                .withName("date-config").withNewHostPath().withNewPath("/etc/localtime").endHostPath()
                .endVolume()
                // .addNewVolume()
               // .endVolume()
                .endSpec().endTemplate().endSpec().build();
        getKubeclinet().apps().deployments().createOrReplace(newDeployment);
        return true;
    }

    public  boolean createDeployment(String namespace, String deploymentName, String appName, Integer replicas, String image, Integer port,String randomPortName ,String nacos ,
                                     String nacosNamespace,String pvcName,Integer memoryXmx,Integer memoryXms,String pvcLogs,boolean isHealth) {
        Container container = createContainer(appName, image,port,randomPortName,nacos , nacosNamespace,pvcName, memoryXmx, memoryXms,pvcLogs,isHealth);

        Map<String,String> labels = new HashMap<>();
        labels.put(LABELS_KEY,randomPortName);
        if(port.equals(8888)){
            labels.put(BaseConfig.GATEWAY_API_KEY,BaseConfig.GATEWAY_API_VALUE);
        }
        Deployment newDeployment = new DeploymentBuilder()
                .withNewMetadata()
                .withName(podAppPrefix+appName)
                .withNamespace(namespace)
                .endMetadata()
                .withNewSpec()
                .withStrategy(new DeploymentStrategyBuilder()
                        .withRollingUpdate(new RollingUpdateDeploymentBuilder()
                                .withMaxSurge(new IntOrStringBuilder().withStrVal("25%").build())
                                .withMaxUnavailable(new IntOrStringBuilder().withStrVal("25%").build())
                                .build())
                        .withType("RollingUpdate")
                        .build())
                .withNewSelector()
                .addToMatchLabels(LABELS_KEY, randomPortName)
                .endSelector()
                .withReplicas(replicas)
                .withNewTemplate()
                .withNewMetadata()
                //.addToLabels(LABELS_KEY, randomPortName)
                .addToLabels(labels)
                .endMetadata()
                .withNewSpec()
                .withTerminationGracePeriodSeconds(60L)
                .withContainers(container)
                .addNewVolume()
                .withName(pvcName)
                .withPersistentVolumeClaim(new PersistentVolumeClaimVolumeSourceBuilder().withClaimName(pvcName).build())
                .endVolume()

                .addNewVolume()
                .withName(pvcLogs)
                .withPersistentVolumeClaim(new PersistentVolumeClaimVolumeSourceBuilder().withClaimName(pvcLogs).build())
                .endVolume()

                .endSpec().endTemplate().endSpec().build();
        getKubeclinet().apps().deployments().createOrReplace(newDeployment);
        return true;
    }


    /**
     *
     * @param appName
     * @param image
     * @param ports
     * @param serviceName
     * @param nacos
     * @param nacosNamespace
     * @param pvcName
     * @param memoryXmx 最大
     * @param memoryXms 最小
     * @param isHealth 是否健康
     * @return
     */
    private  Container createContainer(String appName,String image,Integer ports,String serviceName,String nacos ,String nacosNamespace,String pvcName
    ,Integer memoryXmx,Integer memoryXms,String pvcLogs, boolean isHealth){
        log.info("ports :  {},serviceName  : {}",ports,serviceName);

        Container container = new  Container();
        Map<String,Quantity> requests= new HashMap(1);
        requests.put("memory",new Quantity(String.valueOf(memoryXms),"M"));

        ResourceRequirements resource= new ResourceRequirements();
        Map<String,Quantity> map= new HashMap(1);
        map.put("memory",new Quantity(String.valueOf(memoryXmx),"M"));
        resource.setLimits(map);

        List<String> cmdList = new ArrayList<>();
        cmdList.add("java");
        cmdList.add("-jar");
        container.setCommand(cmdList);
        List<String> arrayList = new ArrayList<>();
        arrayList.add("-Xms"+memoryXms+"m");
        arrayList.add("-Xmx"+memoryXmx+"m");
        arrayList.add("/app.jar");
        container.setArgs(arrayList);

        resource.setRequests(requests);
        container.setResources(resource);

        Probe probe = new Probe();
        HTTPGetAction httpGetAction = new HTTPGetAction();
        httpGetAction.setPath("/health");
        httpGetAction.setPort(new IntOrStringBuilder().withIntVal(9000).build());
        httpGetAction.setScheme("HTTP");
        probe.setHttpGet(httpGetAction);
        probe.setInitialDelaySeconds(30);
        probe.setTimeoutSeconds(5);
        probe.setSuccessThreshold(1);
        probe.setFailureThreshold(3);
        Probe probe2 = new Probe();
        HTTPGetAction httpGetAction2 = new HTTPGetAction();
        httpGetAction2.setPath("/health");
        httpGetAction2.setPort(new IntOrStringBuilder().withIntVal(9000).build());
        httpGetAction2.setScheme("HTTP");
        probe2.setHttpGet(httpGetAction2);
        //容器启动后多久开始探测
        probe2.setInitialDelaySeconds(30);
        //表示容器必须在2s内做出相应反馈给probe，否则视为探测失败
        probe2.setTimeoutSeconds(5);
        // 探测周期，每30s探测一次
        probe2.setPeriodSeconds(20);
        // 连续探测1次成功表示成功
        probe2.setSuccessThreshold(1);
        //连续探测3次失败表示失败
        probe2.setFailureThreshold(3);
        if(isHealth){
            container.setReadinessProbe(probe2);
            container.setLivenessProbe(probe);
        }
 /*       Lifecycle lifecycle = new Lifecycle();
        Handler handler = new Handler();
        ExecAction execAction = new ExecAction();
        List<String> list =new ArrayList<>();
        list.add("sleep");
        list.add("30");
        execAction.setCommand(list);
        handler.setExec(execAction);
        lifecycle.setPreStop(handler);
        container.setLifecycle(lifecycle);*/


        container.setName(appName);
        container.setImage(image);



        List<EnvVar> env = new ArrayList<>(4);
        EnvVar envVar = new EnvVar();
        envVar.setName("NAMESPACE");
        envVar.setValue(nacosNamespace);
        env.add(envVar);


        EnvVar envVar2 = new EnvVar();
        envVar2.setName("NACOS_PORT");
        envVar2.setValue("8848");
       // envVar2.setValue("30848");
        env.add(envVar2);

        EnvVar envVar3 = new EnvVar();
        envVar3.setName("NACOS_IP");
       // envVar3.setValue("139.9.50.40");
        envVar3.setValue(nacos);
        env.add(envVar3);
        container.setEnv(env);
        container.setImagePullPolicy("Always");


       // String cmd = "java -jar -Xms512m  -Xmx1624m  app.jar";
        List<VolumeMount> volumeMounts = new ArrayList();
        if(pvcName != null){
            VolumeMount volumeMount = new VolumeMount();
            volumeMount.setName(pvcName);
            volumeMount.setMountPath("/tmp/");
            volumeMounts.add(volumeMount);
        }

        VolumeMount volumeLogs = new VolumeMount();
        volumeLogs.setName(pvcLogs);
        volumeLogs.setMountPath("/logs/");
        volumeMounts.add(volumeLogs);

        VolumeMount volumeDate = new VolumeMount();
        volumeDate.setName("date-config");
        volumeDate.setMountPath("/etc/localtime");
        volumeMounts.add(volumeDate);

        container.setVolumeMounts(volumeMounts);


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


    /**
     *
     * @param appName
     * @param image
     * @param ports
     * @param serviceName
     * @param userName
     * @param password
     * @return
     */
    private  Container createContainerPython(String appName,String image,Integer ports,String serviceName,
                                             String envNeo4j,String userName , String password){
        log.info("ports :  {},serviceName  : {}",ports,serviceName);

        Container container = new  Container();
        container.setName(appName);
        container.setImage(image);

        List<EnvVar> env = new ArrayList<>(4);
        EnvVar envVar = new EnvVar();
        envVar.setName("NEO4J_BOLT_IP");
        envVar.setValue("bolt://"+envNeo4j+":7687");
        env.add(envVar);

        EnvVar envVar2 = new EnvVar();
        envVar2.setName("NEO4J_USER_NAME");
        envVar2.setValue(userName);
        env.add(envVar2);

        EnvVar envVar3 = new EnvVar();
        envVar3.setName("NEO4J_PASSWORD");
        envVar3.setValue(password);
        env.add(envVar3);

        container.setEnv(env);
        container.setImagePullPolicy("Always");
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


    /**
     *
     * @param namespace
     * @param deploymentName
     * @param appName
     * @param replicas
     * @param image
     * @param port
     * @param randomPortName
     * @param userName
     * @param password
     * @return
     */
    public  boolean createDeploymentPython(String namespace, String deploymentName, String appName, Integer replicas, String image, Integer port,String randomPortName ,
                                           String env,String userName , String password) {
        Container container = createContainerPython(appName, image,port,randomPortName,env , userName,password);
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
                .withTerminationGracePeriodSeconds(60L)
                .withContainers(container)
                .endSpec().endTemplate().endSpec().build();
        getKubeclinet().apps().deployments().createOrReplace(newDeployment);
        return true;
    }

    /**
     * 单位换算
     * @param format
     * @return
     */
    private static Integer memoryUnitFormat(String format){
        Integer num = 1;
        switch (format){
            case "G":
                num  = num * 1000;
                break;
            case "Mi" :
                num  = num * 1;
                break;
            case "m":
                num  = num * 1;
                break;
            case "mi":
                num  = num * 1;
                break;
            default:
        }
        return num;
    }

    public static KubernetesClient getKubeclinet2(){
        System.setProperty(Config.KUBERNETES_KUBECONFIG_FILE,"E:\\dockerfile\\kube-test-config-10");
        Config config = new ConfigBuilder()
                .build();
        return new DefaultKubernetesClient(config);
    }

    @SneakyThrows
    public static void main(String[] args) {

        KubernetesClient kubeclinet = getKubeclinet2();

        ResourceRequirements resource= new ResourceRequirements();
        Map<String,Quantity> map= new HashMap(2);
        map.put("cpu",new Quantity("600","m"));
        map.put("memory",new Quantity("500","M"));
        resource.setLimits(map);

        Map<String,Quantity> stringQuantityMap= new HashMap(2);
        stringQuantityMap.put("cpu",new Quantity(String.valueOf(500),"m"));
        stringQuantityMap.put("memory",new Quantity(String.valueOf(500),"M"));
        resource.setRequests(stringQuantityMap);

  /*      Deployment done = kubeclinet.apps()
                .deployments()
                .inNamespace("1455814591134339073")
                .withName("1455814591134339073")
                .edit()
                .editSpec()
                .editTemplate()
                .editSpec()
                .editContainer(0)
                .withResources(resource)
                .endContainer()
                .endSpec()
                .endTemplate()
                .endSpec()
                .done();

        System.out.println(done);
        System.out.println(done);*/
        List<Pod> items = kubeclinet.pods().inNamespace("1455814591134339073").list().getItems();
        System.out.println(items);


/*        Deployment deployment = new Deployment();
        Map<String,Quantity> stringQuantityMap= new HashMap(2);
        stringQuantityMap.put("cpu",new Quantity(String.valueOf(2000),"m"));
        stringQuantityMap.put("memory",new Quantity(String.valueOf(8000),"M"));
        kubernetesClient.apps().deployments().inNamespace("1422357431756730369").withName("1422357431756730369")
                .edit().editSpec().editTemplate().editSpec().editContainer(0)
                .editResources().withRequests(stringQuantityMap).endResources()
                .endContainer().endSpec()
                .endTemplate().endSpec().done();*/


       // PodMetricOperationsImpl pods = kubernetesClient.top().pods();


   /*     List<PodMetrics> items = pods.metrics("1410113812702375937").getItems();
        items.forEach(pod->{        // 120665107 n
          //  System.out.println(a); 20515912 = 21m


            pod.getContainers().forEach(b->{
                b.setName(pod.getMetadata().getName());
                System.out.println(b);

            });
        });*/

       /*

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

        Pod newPod = kubernetesClient.pods().create(pod);*/


        //createDeployment(namespace, namespace, podName, 2, image, null);

    }

    /**
     * K8S配置文件夹
     * @return
     */
    public static String getHomeConfigDir() {
        String dir = System.getProperty("user.home")+ File.separator + ".kube-deployment"+ File.separator +".k8s"+ File.separator+"configs";
        if(!new File(dir).exists()){
            new File(dir).mkdirs();
        }
        return dir;
    }
    /**
     * nginx配置文件夹
     * @return
     */
    public static String getHomeNginxConfigDir() {
        String dir = System.getProperty("user.home")+ File.separator + ".kube-deployment"+ File.separator +".nginx"+ File.separator+"configs";
        if(!new File(dir).exists()){
            new File(dir).mkdirs();
        }
        return dir;
    }

    /**
     * K8S配置名
     * @return
     */
    public static File getHomeConfigFile() {
        File file = null;
        String dir = System.getProperty("user.home") +File.separator + ".kube-deployment"+ File.separator + ".k8s";
        try {
            if(!new File(dir).exists()){
                new File(dir).mkdirs();
            }
             file = new File(dir + File.separator + "config");
            if (!file.isFile()){
                file.createNewFile();
            }
            if(!System.getProperty("os.name").toLowerCase().contains("win") && !file.exists() && file.length() == 0){
                File file1 = new File("/root/.kube/config");
                return file1;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
    }




    /**
     * 根据master获取配置
     * @param masterUrl
     * @return
     * @throws Exception
     */
    public static Config getMasterConfig(String masterUrl) throws Exception {
        File files = new File(Kubes.getHomeConfigDir());
        File[] files1 = files.listFiles();
        for (File f :files1){
            FileInputStream  stream = new FileInputStream(f);
            String file = OpenFileUtils.readFile(stream);
            stream.close();
            if(StringUtils.isNotBlank(file)){
                Config config = Config.fromKubeconfig(file);
                if(config.getMasterUrl().equals(masterUrl)){
                    return  config;
                }
            }
        }
        return  null;
    }

    /**
     * 根据master获取配置
     * @param masterUrl
     * @return
     * @throws Exception
     */
    public static void setMasterConfig(String masterUrl) throws Exception {
        File files = new File(Kubes.getHomeConfigDir());
        File[] files1 = files.listFiles();
        for (File f :files1){
            FileInputStream  stream = new FileInputStream(f);
            String file = OpenFileUtils.readFile(stream);
            if(StringUtils.isNotBlank(file)){
                Config config = Config.fromKubeconfig(file);
                if(config.getMasterUrl().equals(masterUrl)){
                    FileOutputStream fileOutputStream = null;
                    try {
                     fileOutputStream = new FileOutputStream(getHomeConfigFile());
                    fileOutputStream.write(file.getBytes());
                    }finally {
                        fileOutputStream.flush();
                        fileOutputStream.close();
                    }

                }
            }
        }
    }




}
