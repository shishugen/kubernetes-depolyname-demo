package com.c3stones.entity;

import lombok.Data;

/**
 * @ClassName: DockerConfigs
 * @Description: TODO docker 环境 配置
 * @Author: stone
 * @Date: 2021/4/1 17:38
 */
@Data
public class DockerConfigs {

   private String serverIp;
   private Integer port = 2375;
   private String fileName;
   private String status;
}
