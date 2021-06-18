package com.c3stones.entity;

import com.c3stones.client.BaseConfig;
import lombok.Data;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Value;

/**
 * 
 *
 * @author stone
 * @date 2020-10-21
 */
@Data
public class Pods {


    private String bindK8sIP ;

    public String getBindK8sIP() {
        Config config = BaseConfig.initConfig();
        if(StringUtils.isNotBlank(config.getBindK8sIP())){
            return  config.getBindK8sIP();
        }
        return hostIp;
    }
    private String podName;

    private String deploymentName;

    private String namespace;

    private String hostIp;

    private String podIp;

    private String podStatus;

    private String date;

    private String ports;

    private String openUrl;

    private String nacosName;


}
