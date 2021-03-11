package com.c3stones.socket.service.impl;


import com.c3stones.client.Kubes;
import com.c3stones.socket.constant.ConstantPool;
import com.c3stones.socket.pojo.Pods;
import com.c3stones.socket.pojo.SSHConnectInfo;
import com.c3stones.socket.service.WebSSHService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jcraft.jsch.*;
import io.fabric8.kubernetes.client.dsl.LogWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.*;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
* @Description: WebSSH业务逻辑实现
* @Author: NoCortY
* @Date: 2020/3/8
*/
@Service
public class WebSSHServiceImpl implements WebSSHService {


    @Autowired
    private Kubes kubes;
    //存放ssh连接信息的map
    private static Map<String, Object> sshMap = new ConcurrentHashMap<>();

    private Logger logger = LoggerFactory.getLogger(WebSSHServiceImpl.class);
    //线程池
    private ExecutorService executorService = Executors.newCachedThreadPool();

    /**
     * @Description: 初始化连接
     * @Param: [session]
     * @return: void
     * @Author: NoCortY
     * @Date: 2020/3/7
     */
    @Override
    public void initConnection(WebSocketSession session) {
        JSch jSch = new JSch();
        SSHConnectInfo sshConnectInfo = new SSHConnectInfo();
        sshConnectInfo.setjSch(jSch);
        sshConnectInfo.setWebSocketSession(session);
        String uuid = String.valueOf(session.getAttributes().get(ConstantPool.USER_UUID_KEY));
        //将这个ssh连接信息放入map中
        sshMap.put(uuid, sshConnectInfo);
    }

    /**
     * @Description: 处理客户端发送的数据
     * @Param: [buffer, session]
     * @return: void
     * @Author: NoCortY
     * @Date: 2020/3/7
     */
    @Override
    public void recvHandle(String buffer, WebSocketSession session) {
        ObjectMapper objectMapper = new ObjectMapper();
        Pods pods = new Pods();
        try {
            pods= objectMapper.readValue(buffer, Pods.class);
            if(pods.getRowNumber() == null  || pods.getRowNumber() < 1){
                pods.setRowNumber(200);
            }
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }

        //String userId = String.valueOf(session.getAttributes().get(ConstantPool.USER_UUID_KEY));

        Pods finalPods = pods;
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    connectToSSH(null, finalPods, session);
                } catch (JSchException | IOException e) {
                    logger.error("webssh连接异常");
                    logger.error("异常信息:{}", e.getMessage());
                    close(session);
                }
            }
        });


    }

    @Override
    public void sendMessage(WebSocketSession session, byte[] buffer)  {
        try {
            session.sendMessage(new TextMessage(buffer));
        } catch (IOException e) {
           // e.printStackTrace();
        }
    }

    @Override
    public void close(WebSocketSession session) {
        String userId = String.valueOf(session.getAttributes().get(ConstantPool.USER_UUID_KEY));
        SSHConnectInfo sshConnectInfo = (SSHConnectInfo) sshMap.get(userId);
        if (sshConnectInfo != null) {
            //断开连接
            if (sshConnectInfo.getChannel() != null) sshConnectInfo.getChannel().disconnect();
            //map中移除
            sshMap.remove(userId);
        }
    }

    /**
     * @Description: 使用jsch连接终端
     * @Param: [cloudSSH, webSSHData, webSocketSession]
     * @return: void
     * @Author: NoCortY
     * @Date: 2020/3/7
     */
    private void connectToSSH(SSHConnectInfo sshConnectInfo, Pods pods, WebSocketSession webSocketSession) throws JSchException, IOException {

        LogWatch mysql = kubes.getKubeclinet().pods().inNamespace(pods.getNamespace())
                .withName(pods.getPodName()).usingTimestamps().tailingLines(pods.getRowNumber()).watchLog();
        InputStream inputStream = mysql.getOutput();
        try {


            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream), 1);
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                String s ="";
                if(line.contains("INFO")){
                     s = "<p style=\"color: #0000FF\">" + line + "</p>";
                }else if(line.contains("ERROR")){
                     s = "<p style=\"color: crimson\">" + line + "</p>";
                }
                System.out.println(s);
             //   System.out.println(line);
                sendMessage(webSocketSession,s.getBytes());
            }

        /*    //循环读取
            byte[] buffer = new byte[1024];
            int i = 0;
            //如果没有数据来，线程会一直阻塞在这个地方等待数据。
            while ((i = inputStream.read(buffer)) != -1) {
                System.out.println(inputStream);
                System.out.println(Arrays.copyOfRange(buffer, 0, i));
                sendMessage(webSocketSession, Arrays.copyOfRange(buffer, 0, i));
            }*/

        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }

    }

    /**
     * @Description: 将消息转发到终端
     * @Param: [channel, data]
     * @return: void
     * @Author: NoCortY
     * @Date: 2020/3/7
     */
    private void transToSSH(Channel channel, String command) throws IOException {
        if (channel != null) {
            OutputStream outputStream = channel.getOutputStream();
            outputStream.write(command.getBytes());
            outputStream.flush();
        }
    }


    /**
     * 对将要执行的linux的命令进行遍历
     *
     * @param in
     * @return
     * @throws Exception
     */
    public String processDataStream(InputStream in) throws Exception {
        StringBuffer sb = new StringBuffer();
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        String result = "";
        try {
            while ((result = br.readLine()) != null) {
                sb.append(result).append(",");
                // System.out.println(sb.toString());
            }
        } catch (Exception e) {
            throw new Exception("获取数据流失败: " + e);
        } finally {
            br.close();
        }
        return sb.toString();
    }
}
