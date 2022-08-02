package com.c3stones.util;

import com.c3stones.controller.TestController;
import io.fabric8.kubernetes.api.model.*;
import lombok.Data;

import java.util.List;

/**
 * @ClassName: PodUtils
 * @Description: TODO
 * @Author: stone
 * @Date: 2022/6/28 9:22
 */
public class PodUtils {


    /**
     * 获取pod的状态
     * @param pod
     */
    public PodUtils.KubePodStatus getPodStatus(io.fabric8.kubernetes.api.model.Pod pod){
        PodStatus status = pod.getStatus();
        List<ContainerStatus> containerStatuses = status.getContainerStatuses();
        if(containerStatuses.size()== 0){
            List<PodCondition> conditions = status.getConditions();
            if(conditions != null && conditions.size() > 0){
                PodCondition podCondition = conditions.get(0);
                return  new PodUtils.KubePodStatus(podCondition.getStatus(),podCondition.getReason(), podCondition.getMessage());
            }else{
                return  new PodUtils.KubePodStatus("fail","fail", "未知异常！");

            }
        }
        ContainerState state = containerStatuses.get(0).getState();
        PodUtils.KubePodStatus podStatus = new PodUtils.KubePodStatus("Running","running","运行中");
        if(state.getRunning() != null){
            //运行
        }else if(state.getWaiting() != null){
            //等待
            ContainerStateWaiting waiting = state.getWaiting();
            podStatus = new PodUtils.KubePodStatus("waiting",waiting.getReason(),waiting.getMessage());
        }else if(state.getTerminated() != null){
            //完成
            ContainerStateTerminated terminated = state.getTerminated();
            podStatus = new PodUtils.KubePodStatus("terminated",terminated.getReason(),terminated.getMessage());
        }
        return podStatus;
    }


    /**
     * 状态 类
     */
    @Data
    public  class  KubePodStatus {
        private String status; //状态
        private String reason; //原因
        private String message; //信息

        public KubePodStatus(String status, String reason, String message) {
            this.status = status;
            this.reason = reason;
            this.message = message;
        }

    }

}
