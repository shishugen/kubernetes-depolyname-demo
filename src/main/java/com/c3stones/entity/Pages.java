package com.c3stones.entity;

import lombok.Data;

import java.util.List;

/**
 * @ClassName: Pages
 * @Description: TODO
 * @Author: stone
 * @Date: 2021/2/23 15:44
 */
@Data
public class Pages<T> {

    private List<T> records;
    private Integer total;
}
