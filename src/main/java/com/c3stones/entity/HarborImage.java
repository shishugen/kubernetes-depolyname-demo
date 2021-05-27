package com.c3stones.entity;

import lombok.Data;

import java.util.List;

/**
 * 
 *
 * @author stone
 * @date 2020-10-21
 */
@Data
public class HarborImage {

    private static final long serialVersionUID=1L;



    private String parentId;


    private String harborProjectId;

    private String imageName;

    private Integer status;

    private String version;

    private String created;


    private String harborDomain;

    private String parentName;

    private String imageStatus;


    private List<String> successData;

    private List<String> errorData;







}
