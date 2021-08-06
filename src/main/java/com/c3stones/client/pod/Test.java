package com.c3stones.client.pod;

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


        List<String> namespaceList = new ArrayList<>();
        namespaceList.add("1422398525072683009");
         namespaceList.add("1422398120372678657");
        List<String> podNameList = new ArrayList<>();
        podNameList.add("1422398525072683009-6846fbcfc7-tscc8");
        podNameList.add("1422398120372678657-574c4859f-nlcx2");


       for (int i =0 ; i < namespaceList.size(); i++){
           pod(null,namespaceList.get(i),podNameList.get(i),"ls");
       }
       Thread.sleep(6000L);
        for (int i =0 ; i < namespaceList.size(); i++){
            //获取容器终端对象，写入命令
          //  ExecWatch watch= (ExecWatch) connectMap.get(namespaceList.get(i));
          //  watch.getInput().write("ls".getBytes());
        }
    }
    private static void pod(DefaultKubernetesClient kubernetesClient2,String namespace,String podName,String com){
        Config config = new ConfigBuilder()
                .build();
        DefaultKubernetesClient kubernetesClient = new DefaultKubernetesClient(config);
        final CountDownLatch latch = new CountDownLatch(1);
        ExecWatch watch = kubernetesClient.pods().inNamespace(namespace)
                .withName(podName)
                .readingInput(System.in)
                .writingOutput(System.out)
                .writingError(System.err)
                .withTTY()
                .usingListener(new SimpleListener(latch))
               // .exec("env", "TERM=xterm","bash","");
                .exec("ls");
       // watch.close();
        //执行初始命令
        try {
            //watch.getInput().write("\f".getBytes());
            //watch.getInput().write(com.getBytes());
            //InputStream output = watch.getOutput();
           // connectMap.put(namespace,watch);


          //  NonBlockingInputStreamPumper pump = new NonBlockingInputStreamPumper(output, new OutCallback());
          //  executorService.submit(pump);
           // latch.await(5, TimeUnit.SECONDS);

          //  watch.close();
          //  pump.close();
            //watch.getInput().write("\f".getBytes());



        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /***
     * @Author Lee
     * @Description //TODO 输出回调
     * @Date 2020/11/10 14:55
     * @Param
     * @return
     **/
    private static class OutCallback implements Callback<byte[]> {

        @Override
        public void call(byte[] data) {
            System.out.println("0-----"+new String(data));
        }
    }

    private static class SimpleListener implements ExecListener {

        CountDownLatch latch;
        public SimpleListener(CountDownLatch latch) {
            this.latch = latch;
        }
        @Override
        public void onOpen(okhttp3.Response response) {

            System.out.println("The shell will remain open for 10 seconds.");
        }

        @Override
        public void onFailure(Throwable t, okhttp3.Response response) {
            System.err.println("shell barfed");
            latch.countDown();
        }

        @Override
        public void onClose(int code, String reason) {
            System.out.println("The shell will now close.");
            latch.countDown();
        }
    }

}
