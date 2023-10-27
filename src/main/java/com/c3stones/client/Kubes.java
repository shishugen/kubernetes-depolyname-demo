package com.c3stones.client;

import com.c3stones.common.Response;
import com.c3stones.entity.K8sNode;
import com.c3stones.entity.Namespaces;
import com.c3stones.entity.PodParameter;
import com.c3stones.util.KubeUtils;
import com.c3stones.util.OpenFileUtils;
import com.sun.org.apache.xml.internal.utils.NameSpace;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.*;
import io.fabric8.kubernetes.api.model.extensions.Ingress;
import io.fabric8.kubernetes.api.model.metrics.v1beta1.ContainerMetrics;
import io.fabric8.kubernetes.api.model.metrics.v1beta1.NodeMetricsList;
import io.fabric8.kubernetes.api.model.metrics.v1beta1.PodMetrics;
import io.fabric8.kubernetes.api.model.metrics.v1beta1.PodMetricsList;
import io.fabric8.kubernetes.api.model.networking.v1beta1.*;
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
public class Kubes extends BaseConfig {

    /***
     * labels
     */
    private  static  String LABELS_KEY = "app";

    private static String nodeType ="Ready";

    private static String kubeletReady ="KubeletReady";

    @Value("${pod.namespace.prefix}")
    private String podNamespacePrefix;

   /* @Value("${kubernetes.config.file:/root/.kube/config}")
    private String kubernetesConfigFile;*/
    @Value("${pod.app.prefix}")
    private String podAppPrefix;


    public  KubernetesClient getKubeclinet() {
        String file = null;
        try {
            file = OpenFileUtils.readFileByChars(getK8sHomeConfigFile().getPath());
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
     * @param pvcName
     * @return
     */
    public  boolean createPod(String namespace, String pvcName){
        String podName = namespace+pvcName;
        Volume jar = new VolumeBuilder() .withName(pvcName)
                .withPersistentVolumeClaim(new PersistentVolumeClaimVolumeSourceBuilder().withClaimName(pvcName).build()).build();
        VolumeMount volumeLogs = new VolumeMount();
        volumeLogs.setName(pvcName);
        volumeLogs.setMountPath("/home/");
        Map<String,String> nodeSelectorMap = isk8sArm();
        Pod pod = new PodBuilder().withNewMetadata().withName(podName).withNamespace(namespace)
                    .addToLabels(LABELS_KEY, podName).endMetadata()
                    .withNewSpec()
                    .addToNodeSelector(nodeSelectorMap)
                    .withVolumes(jar)
                    .withContainers(new ContainerBuilder()
                            .withVolumeMounts(volumeLogs)
                           .withCommand("sleep","3600")
                            .withName(podName)
                            .withImage("ssgssg/base-mini:jdk8")
                            .build())
                    .endSpec().build();
            Pod newPod = getKubeclinet().pods().createOrReplace(pod);
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
    public  Boolean deletePv(String name){
        return getKubeclinet().persistentVolumes().withName(name).delete();
    }
    public  Boolean deleteConf(String namesapce,String podName){
        return getKubeclinet().configMaps().inNamespace(namesapce).withName(podName).delete();
    }
    public  Boolean deleteIngress(String namesapce,String name){
        return getKubeclinet().network().ingresses().inNamespace(namesapce).withName(name).delete();
    }
    public  Boolean deleteSecret(String namesapce,String name){
        return getKubeclinet().secrets().inNamespace(namesapce).withName(name).delete();
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

    public  PersistentVolumeClaim createPVC(String name, String namespace,String label, String storageClassName, Integer storageSize ) {
        if(check(name,namespace)){
            return get(name,namespace);
        }
        Map<String,Quantity> map = new HashMap(1);
        map.put("storage",new Quantity(String.valueOf(storageSize),"G"));
        Map<String,String> labels = new HashMap(1);
        if (StringUtils.isNotBlank(label)){
            labels.put(label,label);
        }
        PersistentVolumeClaim build = new PersistentVolumeClaimBuilder()
                .withNewMetadata()
                .withName(name)
                .withLabels(labels)
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
    public  PersistentVolumeClaim createPVC(String name, String namespace, String storageClassName, Integer storageSize ) {
     return this.createPVC(name,namespace,null,storageClassName,storageSize);
    }

    /**
     * 手动创建PVC
     * @param name
     * @param namespace
     * @param storageSize
     * @return
     */
    public  PersistentVolumeClaim createPVC(String name, String namespace, Integer storageSize ) {
        PersistentVolumeClaim build = new PersistentVolumeClaimBuilder()
                .withNewMetadata()
                .addToLabels("nfs-pvc","nfs-pvc")
                .withName(name)
                .withNamespace(namespace)
                .endMetadata()
                .withNewSpec()
                .withAccessModes("ReadWriteMany")
                .withNewResources().addToRequests("storage",new Quantity(storageSize.toString(),"Gi")).endResources().endSpec()
                .build();
        return getKubeclinet().persistentVolumeClaims().createOrReplace(build);
    }


        /**
         *
         * @param name
         * @param namespace
         * @param nfsAddr
         * @param nfsPath
         * @param storageSize
         * @return
         */
    public  PersistentVolume createPV(String name, String namespace,String nfsAddr, String nfsPath ,Integer storageSize) {
        Map<String,Quantity> map = new HashMap(1);
        map.put("storage",new Quantity(String.valueOf(storageSize),"Gi"));
        PersistentVolume build = new PersistentVolumeBuilder()
                .withNewMetadata()
                .addToLabels("nfs-pv","nfs-pv")
                .withName(name)
                .withNamespace(namespace)
                .endMetadata()
                .withNewSpec()
                .withCapacity(map)
                .withAccessModes("ReadWriteMany")
                .withPersistentVolumeReclaimPolicy("Delete")
                .withNewNfs().withServer(nfsAddr).withPath(nfsPath).endNfs()
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
                                     String nacos , String nacosNamespace,Integer memoryXmx,Integer memoryXms,String pvcLogs,boolean isHealth,String seataNacosNamespace
    ,PodParameter podParameter) {
        Container container =
                createContainer(appName, image,port,randomPortName,nacos , nacosNamespace,null, memoryXmx, memoryXms,pvcLogs,isHealth,seataNacosNamespace,podParameter);
        Map<String,String> labels = new HashMap<>();
        String jarName = image.substring(image.lastIndexOf("/")+1, image.lastIndexOf(":"));
        labels.put(LABELS_JAR_NAME,jarName);
        labels.put(LABELS_KEY,randomPortName);
        if(port != null && port.equals(8888)){
            labels.put(BaseConfig.GATEWAY_API_KEY,BaseConfig.GATEWAY_API_VALUE);
        }
        List<Volume> lists = new ArrayList<>();
        Volume log = new VolumeBuilder() .withName(pvcLogs)
                .withPersistentVolumeClaim(new PersistentVolumeClaimVolumeSourceBuilder().withClaimName(pvcLogs).build()).build();
        lists.add(log);
        if (StringUtils.isNotBlank(podParameter.getPvcName())){
            Volume jar = new VolumeBuilder() .withName(podParameter.getPvcName())
                    .withPersistentVolumeClaim(new PersistentVolumeClaimVolumeSourceBuilder().withClaimName(podParameter.getPvcName()).build()).build();
            lists.add(jar);
        }
        Map<String,String> nodeSelectorMap = isk8sArm();
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
                .addToNodeSelector(nodeSelectorMap)
                .withContainers(container)
                .addAllToVolumes(lists)
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
                                     String nacosNamespace,String pvcName,Integer memoryXmx,Integer memoryXms,String pvcLogs,boolean isHealth,String seataNacosNamespace,PodParameter podParameter) {
        Container container = createContainer(appName, image,port,randomPortName,nacos , nacosNamespace,pvcName, memoryXmx, memoryXms,pvcLogs,isHealth,seataNacosNamespace,podParameter);

        Map<String,String> labels = new HashMap<>();
        String jarName = image.substring(image.lastIndexOf("/")+1, image.lastIndexOf(":"));
        labels.put(LABELS_KEY,randomPortName);
        labels.put(LABELS_JAR_NAME,jarName);
        if(port != null &&port.equals(8888)){
            labels.put(BaseConfig.GATEWAY_API_KEY,BaseConfig.GATEWAY_API_VALUE);
        }
        List<Volume> lists = new ArrayList<>();
        Volume tmp = new VolumeBuilder() .withName(pvcName)
                .withPersistentVolumeClaim(new PersistentVolumeClaimVolumeSourceBuilder().withClaimName(pvcName).build()).build();
        Volume log = new VolumeBuilder() .withName(pvcLogs)
                .withPersistentVolumeClaim(new PersistentVolumeClaimVolumeSourceBuilder().withClaimName(pvcLogs).build()).build();
        lists.add(tmp);
        lists.add(log);
        if (StringUtils.isNotBlank(podParameter.getPvcName())){
            Volume jar = new VolumeBuilder() .withName(podParameter.getPvcName())
                    .withPersistentVolumeClaim(new PersistentVolumeClaimVolumeSourceBuilder().withClaimName(podParameter.getPvcName()).build()).build();
            lists.add(jar);
        }
        Map<String,String> nodeSelectorMap = isk8sArm();
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
                .addToNodeSelector(nodeSelectorMap)
                .withTerminationGracePeriodSeconds(60L)
                .withContainers(container)
                .addAllToVolumes(lists)
                .addNewVolume()
                .withName("date-config").withNewHostPath().withNewPath("/etc/localtime").endHostPath()
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
    ,Integer memoryXmx,Integer memoryXms,String pvcLogs, boolean isHealth,String seataNacosNamespace,PodParameter podParameter){
        log.info("ports :  {},serviceName  : {}",ports,serviceName);

        Container container = new  Container();
        Map<String,Quantity> requests= new HashMap(1);
        requests.put("memory",new Quantity(String.valueOf(memoryXms),"M"));

        ResourceRequirements resource= new ResourceRequirements();
        Map<String,Quantity> map= new HashMap(1);
        map.put("memory",new Quantity(String.valueOf(memoryXmx),"M"));
        resource.setLimits(map);

        List<String> cmdList = new ArrayList<>();

        if (StringUtils.isNotBlank(podParameter.getPvcName())){
            cmdList.add("/libs/jdk/bin/java");
        }else{
            cmdList.add("java");
        }
        if (StringUtils.isNotBlank(podParameter.getPvcName())){
            cmdList.add("-Dloader.path=/libs/");
        }
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
        probe2.setPeriodSeconds(30);
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
        env.add(envVar2);

        EnvVar envVar3 = new EnvVar();
        envVar3.setName("NACOS_IP");
        envVar3.setValue(nacos);
        env.add(envVar3);

        EnvVar envVar4 = new EnvVar();
        envVar4.setName("SEATA_NAMESPACE");
        envVar4.setValue(seataNacosNamespace);
        env.add(envVar4);


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

      if (StringUtils.isNotBlank(podParameter.getPvcName())){
          VolumeMount volumejar = new VolumeMount();
          volumejar.setName(podParameter.getPvcName());
          volumejar.setMountPath("/libs/");
          volumeMounts.add(volumejar);
      }

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
        Map<String,String> nodeSelectorMap = isk8sArm();

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
                .addToNodeSelector(nodeSelectorMap)
                .withTerminationGracePeriodSeconds(60L)
                .withContainers(container)
                .endSpec().endTemplate().endSpec().build();
        getKubeclinet().apps().deployments().createOrReplace(newDeployment);
        return true;
    }

    public List<K8sNode> findNode(){
        KubernetesClient kubeclinet2 = getKubeclinet();
        List<Node> nodeList = kubeclinet2.nodes().list().getItems();
        String masterIp = kubeclinet2.getMasterUrl().getHost();
        List<K8sNode> k8sNodeList = new ArrayList<>();
        for(Node node : nodeList){
            K8sNode k8sNode = new K8sNode();
            NodeStatus status = node.getStatus();
            ObjectMeta metadata = node.getMetadata();
            //是否为主节点
            String isMaster = metadata.getLabels().get("node-role.kubernetes.io/master");
            if(isMaster != null){
                k8sNode.setMaster(true);
            }else{
                k8sNode.setMaster(false);
            }
            k8sNode.setNodeName(metadata.getName());
            NodeSystemInfo nodeInfo = status.getNodeInfo();
            k8sNode.setDockerVersion(nodeInfo.getContainerRuntimeVersion());
            k8sNode.setOsImage(nodeInfo.getOsImage());
            k8sNode.setK8sVersion(nodeInfo.getKubeletVersion());
            List<NodeAddress> addresses = status.getAddresses();
            for(NodeAddress address : addresses){
                String nodeIp = address.getAddress();
                if("InternalIP".equals(address.getType())){
                    k8sNode.setNodeIp(nodeIp);
                }
            }
            List<NodeCondition> conditions = status.getConditions();
            for(NodeCondition condition : conditions){
                String type = condition.getType();
                if (nodeType.equals(type)){
                    if(kubeletReady.equals(condition.getReason())){
                        //正常
                        k8sNode.setStatus(nodeType);
                    }else{
                        //不正常
                        k8sNode.setStatus(condition.getReason());
                    }
                }
            }
            k8sNodeList.add(k8sNode);
        }
        return k8sNodeList;
    }





    public static KubernetesClient getKubeclinet2(){
        System.setProperty(Config.KUBERNETES_KUBECONFIG_FILE,"E:\\dockerfile\\10.49.0.11.config");
        Config config = new ConfigBuilder()
                .build();

        return new DefaultKubernetesClient(config);
    }
    public static KubernetesClient getKubeclinet3(){
        System.setProperty(Config.KUBERNETES_KUBECONFIG_FILE,"E:\\dockerfile\\192.168.115.106/k8s.txt");
        Config config = new ConfigBuilder()
                .build();
        return new DefaultKubernetesClient(config);
    }

    public static KubernetesClient getKubeclinetHauwei(){
        System.setProperty(Config.KUBERNETES_KUBECONFIG_FILE,"E:\\dockerfile\\luzik8s");
        Config config = new ConfigBuilder()
                .build();
        return new DefaultKubernetesClient(config);
    }


    public static void deleteingr(){
        KubernetesClient kubeclinet = getKubeclinetHauwei();
        kubeclinet.network().ingress().inNamespace("app-sys").withName("nginxdist-1").delete();
    }

    public void testpv(){
        KubernetesClient kubeclinet2 = getKubeclinet2();
        List<PersistentVolume> items = kubeclinet2.persistentVolumes().list().getItems();
        PersistentVolume pv = new PersistentVolumeBuilder().withNewMetadata().withName("test").endMetadata()
                .withNewSpec().addToCapacity("storage",new Quantity("2","Gi")).endSpec()

                .build();


    }

    @SneakyThrows
    public static void main(String[] args) {
        KubernetesClient kubeclinet2 = getKubeclinetHauwei();
        String deploymentName = "1627854984015216642";
        String namespace = "1627854984015216642";

        kubeclinet2.apps().deployments()
                .inNamespace(namespace)
                .withName(deploymentName)
                .rolling()
                .pause();













/*        Map<String,Integer> map = new HashMap(1);


        PodList list = kubeclinet.pods().list();
        List<Pod> items1 = list.getItems();
        items1.stream().forEach(pod ->{
            String name2 = pod.getMetadata().getName();
            Container container = pod.getSpec().getContainers().get(0);

            ResourceRequirements resources = container.getResources();

            Map<String, Quantity> capacity = resources.getLimits();
            if (capacity != null){
                capacity.forEach((k, v)->{
                    Quantity quantity = capacity.get(k);
                    switch (k){
                        case "memory":
                            String format = quantity.getFormat();
                            int memory2 = Integer.valueOf(quantity.getAmount()) * memoryUnitFormat(format);
                            map.put(name2,memory2);
                            break;
                        case "cpu":
                            int c = 1 * Integer.valueOf(quantity.getAmount());
                            map.put("cpu-"+name2,c);
                            break;
                        default:
                    }
                });
            }
        });
        System.out.println("map====="+map);
        List<PodMetrics> items = kubeclinet.top().pods().metrics().getItems();

        items.stream().forEach(a->{
            String name3 = a.getMetadata().getName();
            System.out.println("name3=="+name3);
            List<ContainerMetrics> containers = a.getContainers();
            containers.stream().forEach(b->{
                Map<String, Quantity> usage = b.getUsage();
                Quantity quantity2 = usage.get("memory");
                Quantity cpuQuantity = usage.get("cpu");

                String format2 = quantity2.getFormat();
                if ( format2.endsWith("Ki")){
                    System.out.println(Integer.valueOf(quantity2.getAmount()) / 1024);
                    Integer integer = map.get(name3);
                    if (integer != null && integer > 0){
                        System.out.println("最大"+integer);
                        System.out.println("当前"+Integer.valueOf(quantity2.getAmount()) / 1024);
                    }

                }
                String format = cpuQuantity.getFormat();
                Integer integer = map.get("cpu-"+name3);
                if (integer != null && integer > 0){
                    System.out.println("CPU"+integer);
                    System.out.println("CPU=="+Integer.valueOf(cpuQuantity.getAmount())/ 1024);
                }
                System.out.println("CPU单位："+format);
            });

        });*/



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
     * ssl配置文件夹
     * @return
     */
    public static String getHomeSSLDir() {
        String dir = System.getProperty("user.home")+ File.separator + ".kube-deployment"+ File.separator +".ssl"+ File.separator+"configs";
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
    public static File getK8sHomeConfigFile() {
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
                     fileOutputStream = new FileOutputStream(Kubes.getK8sHomeConfigFile());
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
