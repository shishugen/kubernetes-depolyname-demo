package com.c3stones.socket.pojo;

import lombok.Data;

/**
 * @ClassName: Pods
 * @Description: TODO
 * @Author: stone
 * @Date: 2021/2/20 15:23
 */
@Data
public class Pods {
    private String namespace;
    private String podName;
    private Integer rowNumber;

}
