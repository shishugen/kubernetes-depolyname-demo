package com.c3stones.client.pod;

import com.sun.scenario.effect.impl.sw.sse.SSEBlend_SRC_OUTPeer;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.client.Callback;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.dsl.ExecListener;
import io.fabric8.kubernetes.client.dsl.ExecWatch;
import io.fabric8.kubernetes.client.utils.NonBlockingInputStreamPumper;
import org.springframework.web.socket.WebSocketSession;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * @ClassName: Test
 * @Description: TODO
 * @Author: stone
 * @Date: 2021/8/3 16:54
 */
public class Test {
    //线程池
    private static  ExecutorService executorService = Executors.newCachedThreadPool();
    private static Map<String, Object> connectMap = new ConcurrentHashMap<>();

     static{
         System.setProperty(Config.KUBERNETES_KUBECONFIG_FILE, "E:\\dockerfile\\10.49.0.11.config");
     }

    public static void main(String[] args) throws Exception {

        Config config = new ConfigBuilder()
                .build();
        DefaultKubernetesClient kubernetesClient = new DefaultKubernetesClient(config);




    }





}
