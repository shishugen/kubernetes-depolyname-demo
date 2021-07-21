package com.c3stones.client.pod;

import com.c3stones.client.Kubes;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
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
        Kubes kubes = new Kubes();
        Map<String,String> map = new HashMap<>();
        map.put("","-----BEGIN CERTIFICATE-----\n" +
                "MIIGATCCBOmgAwIBAgIQBs66T7g8aBxC9zBQFAXJdTANBgkqhkiG9w0BAQsFADBu\n" +
                "MQswCQYDVQQGEwJVUzEVMBMGA1UEChMMRGlnaUNlcnQgSW5jMRkwFwYDVQQLExB3\n" +
                "d3cuZGlnaWNlcnQuY29tMS0wKwYDVQQDEyRFbmNyeXB0aW9uIEV2ZXJ5d2hlcmUg\n" +
                "RFYgVExTIENBIC0gRzEwHhcNMjEwNzE0MDAwMDAwWhcNMjIwNzE0MjM1OTU5WjAh\n" +
                "MR8wHQYDVQQDExZwb3J0YWwueHVhbnl1YW4uY29tLmNuMIIBIjANBgkqhkiG9w0B\n" +
                "AQEFAAOCAQ8AMIIBCgKCAQEAsvs1DdAFNCxOnFoq6a8Jph/2ZHsnp5pikqinAkfp\n" +
                "B5NKCbqcGXzozA+WJop7jUwi5yNLgCuVq7iy4woAue4bdrjCyF7LSTiddzVqmMkE\n" +
                "0dTRh3nkPn10vXTNr/3jYc+MY6CYM1uUZnoKh4Cew5tj/xRrXiTjWloSKW4mKE5I\n" +
                "4GGmeWyjcW+zIU0OGoVnBmwZcmSWckFO5gLaTjFUVnnquacsVn5ETPjPy5GxyNGR\n" +
                "ciJ1zQTiM+DG1BiId2jw424gZAWqoe/fKAOLyD4oPEpbafPDlPyod/BK1AXRspkI\n" +
                "8d+oMcaxk+wJLcSvSPjwpFgMCcXfKkHZZRNVeSqYyXK9ZwIDAQABo4IC5jCCAuIw\n" +
                "HwYDVR0jBBgwFoAUVXRPsnJP9WC6UNHX5lFcmgGHGtcwHQYDVR0OBBYEFAevAIG9\n" +
                "paMzZbuH/mwZR9+6bX74MCEGA1UdEQQaMBiCFnBvcnRhbC54dWFueXVhbi5jb20u\n" +
                "Y24wDgYDVR0PAQH/BAQDAgWgMB0GA1UdJQQWMBQGCCsGAQUFBwMBBggrBgEFBQcD\n" +
                "AjA+BgNVHSAENzA1MDMGBmeBDAECATApMCcGCCsGAQUFBwIBFhtodHRwOi8vd3d3\n" +
                "LmRpZ2ljZXJ0LmNvbS9DUFMwgYAGCCsGAQUFBwEBBHQwcjAkBggrBgEFBQcwAYYY\n" +
                "aHR0cDovL29jc3AuZGlnaWNlcnQuY29tMEoGCCsGAQUFBzAChj5odHRwOi8vY2Fj\n" +
                "ZXJ0cy5kaWdpY2VydC5jb20vRW5jcnlwdGlvbkV2ZXJ5d2hlcmVEVlRMU0NBLUcx\n" +
                "LmNydDAJBgNVHRMEAjAAMIIBfgYKKwYBBAHWeQIEAgSCAW4EggFqAWgAdQBGpVXr\n" +
                "dfqRIDC1oolp9PN9ESxBdL79SbiFq/L8cP5tRwAAAXqj0MZEAAAEAwBGMEQCIHba\n" +
                "VNslpk3b1LHJzyVmZBGVNwqo8+2CLu4nYKT4sNWpAiB1yN/xE+PrqtUAX0VWCzqb\n" +
                "J2cdjoXAc1xBVhj67j4bHQB3AFGjsPX9AXmcVm24N3iPDKR6zBsny/eeiEKaDf7U\n" +
                "iwXlAAABeqPQxj0AAAQDAEgwRgIhAKyA8wBfWgKjj7XEbYJ8tsSB7wnp9fc6MVkh\n" +
                "0F7+Zx/YAiEAkv5faPEHpoBWESBujK1zn2neU+Mv2Z+oExQy0QJVCu8AdgBByMqx\n" +
                "3yJGShDGoToJQodeTjGLGwPr60vHaPCQYpYG9gAAAXqj0MXKAAAEAwBHMEUCIQC6\n" +
                "FHvtuRNqX0z30VTEiv7r4hoDF3CLcOJB1ylrjeCpAgIgQwFZXG1e17irXaUY3atE\n" +
                "x61wBFNcwBVPs5yHNmXthbQwDQYJKoZIhvcNAQELBQADggEBAD6iqDexNWYNEY6n\n" +
                "HBWbCnzCncMIL55In1HmTSvBUeKudnt3ikeDkmQMi/7OCZ1uZgYLQVXkaW1LGuj9\n" +
                "XpxwQZPN2DOmTZqDsmf7a6un7OrzBnTPjzTrPhKgYg85sv4Jav9w6MSlWigMh9VG\n" +
                "iUkJBGbzHk9wQf7F2smoO92gb7pwW9CPRCEmYcbDEpMoOU47lb5ZYkLFBfd44/Pz\n" +
                "36IELhsxYCbYPUtTW12yamk9+amphlOL2jflMuQBLwH5UiWD7cSqRB+MqmWzBX7T\n" +
                "hdKKssG0bk4Q1svV9c+I38I7bZL6PVBKWiU3cKkeVQ3jlbN+GMov9YhDg7LabBNF\n" +
                "GNU2oHw=\n" +
                "-----END CERTIFICATE-----\n");
        KubernetesClient kubeclinet = kubes.getKubeclinet();
        Secret secretBuilder = new SecretBuilder()
                .withNewMetadata().withName("test").endMetadata()
                .withType("Opaque")
                .build();

        kubeclinet.secrets().create(secretBuilder);

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


