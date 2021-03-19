package com.c3stones.client.pod;

import com.c3stones.client.Kubes;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * @ClassName: Test
 * @Description: TODO
 * @Author: stone
 * @Date: 2021/2/4 16:57
 */
public class Test2 {

    /**
     * 用于网络策略 pod  Service
     */
    protected static final String POD_LABELS_KEY = "app";

    public static Deployment createDeployment() {
        Container container = createContainer();
        Deployment newDeployment = new DeploymentBuilder()
                .withNewMetadata()
                .withName("storage0")
                .withNamespace("fdfs")
                .endMetadata()
                .withNewSpec()
                .withNewSelector()
                .addToMatchLabels(POD_LABELS_KEY, "storage0")
                .endSelector()
                .withReplicas(1)
                .withNewTemplate()
                .withNewMetadata()
                .addToLabels(POD_LABELS_KEY, "storage0")
                .endMetadata()
                .withNewSpec()
                .withContainers(container)
                .withVolumes(new VolumeBuilder().withName("storage0").withHostPath(new HostPathVolumeSourceBuilder().withNewPath("/home/data/fdfs/storage1").build()).build())
                .endSpec().endTemplate().endSpec().build();
        return null; //Kubes.getKubeclinet().apps().deployments().create(newDeployment);
    }

    /**
     *
     * @param appName
     * @param image
     * @param cpu
     * @param memory
     * @return
     */
    private static Container createContainer() {
      return   new ContainerBuilder()
                .withName("storage0")
                .withImage("ygqygq2/fastdfs-nginx")
                // systemctl
                .withSecurityContext(new SecurityContextBuilder().withPrivileged(true).build())
                .withCommand("/usr/bin/start.sh")
                .withArgs("storage")
            //  .addToEnv(new EnvVarBuilder().withName("TRACKER_SERVER").withValue("tracker.fastdfs:22122").build())
              .addToEnv(new EnvVarBuilder().withName("TRACKER_SERVER").withValue("10.49.0.12:30446").build())
             // .addToEnv(new EnvVarBuilder().withName("TRACKER_SERVER_2").withValue("10.98.78.172:22122").build())
              .addToPorts(new ContainerPortBuilder().withName("storage0").withContainerPort(23000).build())
                .addNewVolumeMount()
                .withName("storage0")
                .withMountPath("/var/fdfs/")
                .endVolumeMount()
                .build();
    }



    public static Service createService() {
        ServicePort servicePort =  new ServicePort();
        servicePort.setName("storage0");
        servicePort.setProtocol("TCP");
        servicePort.setPort(23000);
        Service newService = new ServiceBuilder()
                .withNewMetadata()
                .withName("storage0")
                .withNamespace("fdfs")
                .endMetadata()
                .withNewSpec()
                .withPorts(servicePort)
                .addToSelector(POD_LABELS_KEY,"storage0")
                .withType("NodePort")
                .endSpec()
                .build();
        return null;//  Kubes.getKubeclinet().services().create(newService);
    }

    public static void main(String[] args) throws IOException {
        unzipJar("E:\\xuanyuan_project\\xuanyuan-base-spring-cloud\\training-docker-1.0\\training\\kubernetes\\zhsys-training-kubernetes-server\\target\\test",
                "E:\\xuanyuan_project\\xuanyuan-base-spring-cloud\\training-docker-1.0\\training\\kubernetes\\zhsys-training-kubernetes-server\\target\\zhsys-training-kubernetes-server.jar");

    }

    public static void unzipJar(String destinationDir, String jarPath) throws IOException {

        File file = new File(jarPath);

        JarFile jar = new JarFile(file);


        for (Enumeration<JarEntry> enums = jar.entries(); enums.hasMoreElements();) {

            JarEntry entry = (JarEntry) enums.nextElement();

            String fileName = destinationDir + File.separator + entry.getName();

            File f = new File(fileName);

            if (fileName.endsWith("/")) {

                f.mkdirs();

            }

        }


        for (Enumeration<JarEntry> enums = jar.entries(); enums.hasMoreElements();) {

            JarEntry entry = (JarEntry) enums.nextElement();

            String fileName = destinationDir + File.separator + entry.getName();

            File f = new File(fileName);

            if (!fileName.endsWith("/")) {

                InputStream is = jar.getInputStream(entry);


                FileOutputStream fos = new FileOutputStream(f);


                while (is.available() > 0) {
                    fos.write(is.read());
                }
                fos.flush();
                fos.close();
                is.close();

            }

        }

    }
}


