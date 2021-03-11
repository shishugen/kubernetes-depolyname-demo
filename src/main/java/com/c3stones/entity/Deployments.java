package com.c3stones.entity;

import lombok.Data;

/**
 * @ClassName: Deployments
 * @Description: TODO
 * @Author: stone
 * @Date: 2021/3/9 10:28
 */
@Data
public class Deployments {
    private String name;
    private Integer replicas;
    private String namespace;
    private String date;
    private String image;

    public Deployments(String name, Integer replicas,String date,String namespace,String image) {
        this.name = name;
        this.replicas = replicas;
        this.date = date;
        this.namespace = namespace;
        this.image = image;
    }
}
