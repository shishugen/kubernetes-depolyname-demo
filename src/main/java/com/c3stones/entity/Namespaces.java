package com.c3stones.entity;

/**
 * @ClassName: Namespaces
 * @Description: TODO
 * @Author: stone
 * @Date: 2021/1/28 16:31
 */
public class Namespaces {

    private String name;

    private String data;

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public Namespaces(String name, String data) {
        this.name = name;
        this.data = data;
    }

    public Namespaces(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
