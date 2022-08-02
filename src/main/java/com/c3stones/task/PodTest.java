package com.c3stones.task;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.net.NetUtil;
import cn.hutool.extra.mail.MailAccount;
import cn.hutool.extra.mail.MailUtil;
import com.c3stones.client.BaseConfig;
import com.c3stones.client.Kubes;
import com.c3stones.client.pod.TestPod;
import com.c3stones.entity.Config;
import com.c3stones.entity.K8sNode;
import com.c3stones.util.KubeUtils;
import com.c3stones.util.PodUtils;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName: K8sMain
 * @Description: TODO
 * @Author: stone
 * @Date: 2022/5/23 15:59
 */
@Component
//@EnableScheduling
@Slf4j
public class PodTest extends BaseConfig{


    private static Integer sum=0;

    private static Integer task=0;

    @Value("${pod.number:1}")
    private Integer podNumber;

    @Autowired
    private  Kubes kubes;

    @Autowired
    private TestPod testPod;


    //@Scheduled(cron = "${pod.test.cron}")
    public void task() {
        log.info("运行测试");
        KubernetesClient kubeclinet = kubes.getKubeclinet();
        PodList list = kubeclinet.pods().withLabel(LABELS_KEY_TEST).list();
        List<Pod> items = list.getItems();
        if (list != null && items != null && items.size() > 0){
             for (Pod pod : items){
                 kubeclinet.namespaces().withName(pod.getMetadata().getNamespace()).delete();
             }
        }else{
            log.info("第{}创建容器,一次创建{}个容器",sum,podNumber);
            sum++;
            for (int i =0 ; i < podNumber ;i++ ){
                String namespace = KubeUtils.randomPortName();
                testPod.create(namespace,1);
            }
        }

    }

    public static void main(String[] args) {

    }

}
