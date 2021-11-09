package com.c3stones.entity;

import lombok.Data;

/**
 * @ClassName: HarborTemp
 * @Description: 临时对像
 * @Author: stone
 * @Date: 2020/9/4 9:48
 */
@Data
public class HarborTemp {

    private String projectId;
    private String name;
    private String ownerId;
    private String creationTime;
    private String updateTime;
    private Boolean deleted;
    private Integer repoCount;
   // private Integer  size;
    private String created;
    private String dockerVersion;


    private String id;
    private Long pullCount;
    private Integer tagsCount;




}
