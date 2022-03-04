package com.c3stones.file;

import com.alibaba.fastjson.JSONObject;
import com.c3stones.client.Kubes;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.ExecListener;
import io.fabric8.kubernetes.client.dsl.internal.core.v1.PodOperationsImpl;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @ClassName: PodFile
 * @Description: TODO
 * @Author: stone
 * @Date: 2022/3/4 14:01
 */
@Component
public class PodFile {

    @Autowired
    private Kubes kubes;



    public List<String> findPodFile(String namespace,String podName,String path) throws IOException {
        PipedOutputStream out = new PipedOutputStream();
        PipedInputStream in = new PipedInputStream(out);
        String[] comArr = {"ls",path};
        FileExecListener fileExecListener = new FileExecListener(out);
        kubes.getKubeclinet().pods()
                .inNamespace(namespace)
                .withName(podName)
                .redirectingInput()
                .writingOutput(out)
                .usingListener(fileExecListener)
                .exec(comArr);
        int length;
        byte[] buffer = new byte[1024];
        StringBuilder sb = new StringBuilder();
        while ((length = in.read(buffer)) != -1) {
            byte[] actual = new byte[length];
            System.arraycopy(buffer, 0, actual, 0, length);
            sb.append(new String(actual));
        }
        String[] split = sb.toString().split("\n");
        System.out.println(split);
        List<String> list = Arrays.asList(split);
        out.flush();
        out.close();
        in.close();
        return list;
    }

    /**
     * 下载文件
     * @param namespace
     * @param podName
     * @param path
     * @param fileName
     * @param response
     * @param request
     */
    public void downloadFile(String namespace, String podName,String path,String fileName ,HttpServletResponse response , HttpServletRequest request ) {
        InputStream read = kubes.getKubeclinet().pods().inNamespace(namespace)
                .withName(podName).file(path+"/"+fileName).read();
        response.setContentType("application/octet-stream");
        String filename = request.getParameter("filename");
        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;
        try {
            response.setHeader("Content-disposition", "attachment; filename=" +new String((fileName).getBytes("gb2312"),"ISO8859-1"));
            bos = new BufferedOutputStream(response.getOutputStream());
            byte[] buff = new byte[1024];
            int len = 0;
            while((len = read.read(buff)) != -1) {
                bos.write(buff, 0, len);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if(read != null){
                try {
                    read.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (bos != null) {
                try {
                    bos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
    }


        public static KubernetesClient getKubeclinet2(){
        System.setProperty(Config.KUBERNETES_KUBECONFIG_FILE,"E:\\dockerfile\\kube-test-config-10");
        Config config = new ConfigBuilder()
                .build();
        return new DefaultKubernetesClient(config);
    }

    public static void main(String[] args) throws Exception {
        PipedOutputStream out = new PipedOutputStream();
        KubernetesClient kubernetesClient = getKubeclinet2();
        PipedInputStream in = new PipedInputStream(out);
        String[] comArr = {"ls","/db"};
        FileExecListener fileExecListener = new FileExecListener(out);
        kubernetesClient.pods()
                .inNamespace("app-ssg")
                .withName("env-mysqlback-74955f6dd6-52px2")
                .redirectingInput()
                .writingOutput(out)
                .usingListener(fileExecListener)
                .exec(comArr);
        int length;
        byte[] buffer = new byte[1024];
        StringBuilder sb = new StringBuilder();
        while ((length = in.read(buffer)) != -1) {
            byte[] actual = new byte[length];
            System.arraycopy(buffer, 0, actual, 0, length);
            sb.append(new String(actual));
        }
        String[] split = sb.toString().split("\n");
        System.out.println(split);
        List<String> list = Arrays.asList(split);
        for (int i = 0; i < list.size(); i++) {
            if (i == 0) {
                continue;
            }


        }
        out.flush();
        out.close();
        in.close();
    }

        /**
         *  获取文件
         */
       static class FileExecListener implements ExecListener {
            private OutputStream outputStream;

            public FileExecListener(OutputStream outputStream) {
                this.outputStream = outputStream;
            }

            @Override
            public void onOpen(Response response) {

            }

            @Override
            public void onFailure(Throwable t, Response response) {
                // log.error("容器获取文件异常!",t);
            }

            @Override
            public void onClose(int code, String reason) {
                if (outputStream != null) {
                    try {
                        outputStream.flush();
                        outputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }



}
