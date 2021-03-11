package com.c3stones.entity;

import lombok.Data;

/**
 * 
 *
 * @author stone
 * @date 2020-10-21
 */
@Data
public class Pods {



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
