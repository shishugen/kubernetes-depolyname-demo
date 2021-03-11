package com.c3stones.entity;

import lombok.Data;
import lombok.ToString;

/**
 * @ClassName: NacosEntity
 * @Description: TODO
 * @Author: stone
 * @Date: 2021/3/1 17:17
 */
@Data
@ToString
public class NacosEntity {

    private String namespace;
    private String namespaceShowName;
    private String quota;
    private String configCount;
    private String type;
}
