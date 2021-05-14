package com.c3stones.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @ClassName: Pvc
 * @Description: TODO
 * @Author: stone
 * @Date: 2021/5/11 13:41
 */
@Data
@AllArgsConstructor
public class Pvc {
    private String storage;
    private String name;
    private String namespace;
    private String data;
}
