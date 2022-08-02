package com.c3stones.task;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Console;
import cn.hutool.core.net.NetUtil;
import cn.hutool.cron.CronUtil;
import cn.hutool.cron.task.Task;
import cn.hutool.extra.mail.MailAccount;
import cn.hutool.extra.mail.MailUtil;
import com.c3stones.client.BaseConfig;
import com.c3stones.client.Kubes;
import com.c3stones.entity.Config;
import com.c3stones.entity.K8sNode;
import com.c3stones.util.SendMailUtil;
import com.c3stones.util.SpringContextHolder;
import io.fabric8.kubernetes.client.KubernetesClientException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.web.context.ContextLoader;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * @ClassName: K8sMain
 * @Description: TODO
 * @Author: stone
 * @Date: 2022/5/23 15:59
 */
@Component
@EnableScheduling
@Slf4j
public class K8sMail {


    @Autowired
    private  Kubes kubes;





    @Scheduled(cron = "${mail.cron}")
    public void task() {
        Config config = BaseConfig.initConfig();
        if (StringUtils.isBlank(config.getMailHost())&&StringUtils.isBlank(config.getMailPerson())
                &&StringUtils.isBlank(config.getMailPass())
                &&StringUtils.isBlank(config.getMailUser())){
            log.warn("请配置群集检查，邮箱相关配置……");
           return;
        }

        log.info("进入…………集群健康检查……当前时间 :{}",LocalDateTime.now());
        List<K8sNode> node = null;
        try{
             node = kubes.findNode();
        }catch (KubernetesClientException e){
            this.send("集群无法访问，服务器是否开机");
            return;
        }
        List<K8sNode> notNode = new ArrayList<>();
        node.forEach(a->{
            log.info(a.toString());
            //节点不正常
            if (!"Ready".equals(a.getStatus())){
                notNode.add(a);
            }
        });
        if (notNode.size() > 0){
            this.ping(notNode);
        }else{
           log.info("集群节点正常 size : "+node.size());
        }

        //harbor

        String harborUrl = config.getCheckHarborIp();
        String checkNfsIp = config.getCheckNfsIp();
        check(harborUrl,80,"harbor"); //harbor
        check(checkNfsIp,20048,"nfs");//nfs


    }

    /**
     * 是否ping通
     * @param notNode
     */
    public void ping(List<K8sNode> notNode){
        List<K8sNode.Node> notPingList = new ArrayList<>();
        List<K8sNode> pingList = new ArrayList<>();
        if (notNode !=null && notNode.size() > 0){
            notNode.forEach(node->{
                //正常 可以ping通
                if (NetUtil.ping(node.getNodeIp())){
                    pingList.add(node);
                }else{
                    //不正常无法ping通
                    notPingList.add(new K8sNode().new Node(node.getNodeIp()));
                }
            });
        }
        //ping 通
        if (pingList.size() > 0){
            this.checkNet(pingList);
        }

        //无法ping
        if (notPingList.size() > 0){
            this.sendNotPing(notPingList);
        }

    }

    public void check(String ip,Integer port,String name){
        //正常 可以ping通
        if (NetUtil.ping(ip)){
            K8sNode.Node node = returnFail(ip, port.toString());
            if (StringUtils.isNotBlank(node.getPorts())){
                send(port+"， 端口无法访问，请检查服务"+name+"是否正常运行……",ip);
            }
        }else{
            //不正常无法ping通
            send("，无法 ping 与服务器通信，请检查"+name+"服务器是否开机……",ip);
        }

    }

    /**
     * 检查端口
     * @param notNode
     */
    public void checkNet(List<K8sNode> notNode){
        List<K8sNode.Node> failList = new ArrayList<>();
        if (notNode !=null && notNode.size() > 0){
            Config config = BaseConfig.initConfig();
                //主节点
            String  masterPorts= config.getK8sNetPort();
                //子节点端口
            String  nodePorts = config.getK8sNetNodePort();
            notNode.forEach(node->{
                //主节点
                if(node.isMaster()){
                    K8sNode.Node node1 = this.returnFail(node.getNodeIp(), masterPorts);
                    if(StringUtils.isNotBlank(node1.getPorts())){
                        failList.add(node1);
                    }
                }else{
                    K8sNode.Node node1 = this.returnFail(node.getNodeIp(), nodePorts);
                    if(StringUtils.isNotBlank(node1.getPorts())){
                        failList.add(node1);
                    }
                }
            });
        }
        this.send(failList);
    }


    /**
     *  反回异常端口
     * @param host
     * @param ports
     * @return
     */
    public K8sNode.Node returnFail(String host,String ports){
        String[] portsArr = ports.trim().split(",");
        K8sNode.Node node = new K8sNode().new Node();
        node.setNodeIp(host);
        String p = "";
        for (int i =0 ; i < portsArr.length;i++){
            String port = portsArr[i];
            boolean open = NetUtil.isOpen(NetUtil.buildInetSocketAddress(host, Integer.valueOf(port)), 200);
            if (!open){
              p+=","+ port;
              log.error("异常端口：{}:{}",host,port);
            }else{
                //正常端口
                log.info("正常……host:{}:{} ",host,port);
            }
        }
        node.setPorts(p);
      return node;
    }


    private void send(List<K8sNode.Node> failList){
        String host = "";
        if (failList != null && failList.size() > 0){
            for (K8sNode.Node node : failList){
                host+=node.toString();
            }
            this.send("，docker 是否正常运行……，",host);
        }
    }
    private void sendNotPing(List<K8sNode.Node> failList){
        String host = "";
        if (failList != null && failList.size() > 0){
            for (K8sNode.Node node : failList){
                host+=node.getNodeIp()+",";
            }
            this.send("无法与此节点通信,请检查服务器是否开机",host);
        }
    }

    private void send(String content,String host){
        content ="K8S群集故障，请及时处理……以下节点无法正常运行请检查："+host+":"+content;
        send(content);
    }
    private void send(String content){
        log.error("集群故障……发送邮件");
        Config config = BaseConfig.initConfig();
        MailAccount account = new MailAccount();
        account.setHost(config.getMailHost());
        account.setPort(config.getMailPort());
        account.setAuth(true);
        account.setFrom(config.getMailUser());
        account.setUser(config.getMailUser());
        account.setPass(config.getMailPass());
        //标题
        String subject =config.getMailSubject()+"……K8S群集故障";
        //内容
        if (StringUtils.isNotBlank(config.getMailPerson())){
            String[] split = config.getMailPerson().split(",");
            MailUtil.send(account,  CollUtil.newArrayList(split),
                    subject, content, false);
        }else{
            log.error("请设置收邮件人员……");
        }
    }

    public static void main(String[] args) {
        MailAccount account = new MailAccount();
        account.setHost("mail.xuanyuan.com.cn");
        account.setPort(25);
        account.setAuth(true);
        account.setFrom("sxzx@gupt.net");
        account.setUser("sxzx@gupt.net");
        account.setPass("Cb@123");
        //标题
        String subject ="K8S群集故障";
        //内容
       String content ="K8S群集故障，请及时处理……以下节点无法正常运行请检查：";
                MailUtil.send(account, CollUtil.newArrayList("576108653@qq.com"),
                subject, content, false);

    }


}
