package com.c3stones.entity;

import lombok.Data;

/**
 * @ClassName: PodParameter
 * @Description: TODO
 * @Author: stone
 * @Date: 2021/12/29 9:14
 */
@Data
public class PodParameter {

    private String name;
    private String namespace;
    private String mysqlServer;
    private Integer mysqlPort;
    private String mysqlPwd;
    private String mysqlUser;
    private String mysqlDatabase;

    private String nacosServer;
    private Integer nacosPort;
    private String nacosUser;
    private String nacosPwd;
    private String nacosNamespace;
    private String nacosConfigNamespace;
    private String nacosSeataGroup;



}
