package com.c3stones.constant;

/**
 * @ClassName: NfsTypeEnum
 * @Description: TODO
 * @Author: stone
 * @Date: 2022/3/18 11:30
 */
public enum NfsTypeEnum {
    NFS_DATA(1,"nfs","挂载外部"),
    K8S_DATA(2,"k8s","K8S集群自带NFS");
    private Integer index;
   private String name;
   private String explain;

    NfsTypeEnum(Integer index, String name, String explain) {
        this.index = index;
        this.name = name;
        this.explain = explain;
    }

    public String getExplain() {
        return explain;
    }

    public void setExplain(String explain) {
        this.explain = explain;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }
}
