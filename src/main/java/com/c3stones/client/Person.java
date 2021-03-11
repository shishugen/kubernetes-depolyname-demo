package com.c3stones.client;

import java.util.List;

/**
 * @ClassName: Person
 * @Description: TODO
 * @Author: stone
 * @Date: 2021/1/28 9:33
 */
public class Person {

    private String name;
    private int age;
    private String Sex;
    private List<Person> children;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getSex() {
        return Sex;
    }

    public void setSex(String sex) {
        Sex = sex;
    }

    public List<Person> getChildren() {
        return children;
    }

    public void setChildren(List<Person> children) {
        this.children = children;
    }
}
