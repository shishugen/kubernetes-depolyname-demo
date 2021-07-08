package com.c3stones.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.c3stones.client.BaseConfig;
import com.c3stones.client.Dockers;
import com.c3stones.common.Response;
import com.c3stones.entity.HarborImage;
import com.c3stones.entity.Pages;
import com.c3stones.http.HttpHarbor;
import com.c3stones.util.OpenFileUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.json.JsonObject;
import java.util.List;

/**
 * @ClassName: HarborController
 * @Description: TODO
 * @Author: stone
 * @Date: 2021/1/25 15:55
 */
@Controller
@Slf4j
@RequestMapping(value = "harbor")
public class HarborController {


    @Autowired
    private HttpHarbor httpHarbor ;

    @Value("${harbor.image.project.name}")
    private String projectName;



    @RequestMapping(value = "listData")
    @ResponseBody
    public Response<Pages<HarborImage>> listData(String projectName,String version){
        Pages page = new Pages();
        List<HarborImage> harborList = httpHarbor.harborList(projectName,version);
        page.setRecords(harborList);
        page.setTotal(harborList.size());
        return Response.success(page);
    }

    @RequestMapping(value = "list")
    @ResponseBody
    public Response<List<HarborImage>> list(){
         List<HarborImage> harborList = httpHarbor.harborList( BaseConfig.initConfig().getHarborImageProjectName(),null);
        return Response.success(harborList);
    }

    /**
     * 多选
     * @return
     */
    @RequestMapping(value = "multiList")
    @ResponseBody
    public Response<JSONArray> multiList(String projectName,String version){
         List<HarborImage> harborList = httpHarbor.harborList(projectName,version);
         JSONArray jsonArray = new JSONArray();
        harborList.stream().forEach(a->{
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("title", a.getImageName()+":"+a.getVersion());
            jsonObject.put("value", a.getImageName()+":"+a.getVersion());
            jsonArray.add(jsonObject);
        });
        return Response.success(jsonArray);
    }


    /**
     * 删除
     *
     * @param projectName
     * @param tag
     * @return
     */
    @RequestMapping(value = "delete")
    @ResponseBody
    public Response<Boolean> delete(String projectName,String tag) {
        log.info("删除  projectName : {}:{}",projectName,tag);
        Assert.notNull(projectName, "projectName不能为空");
        Assert.notNull(tag, "tag不能为空");
        //删除镜像
        String urlParame = "repositories/"+projectName+"/tags/"+tag;
        ResponseEntity<JSONArray> exchange = httpHarbor.send(urlParame, HttpMethod.DELETE);
        if(exchange.getStatusCodeValue() == 200){
            return Response.success(true);
        }else{
            return Response.success(false);
        }
    }

    /**
     * 清空镜像文件
     *
     * @return
     */
    @RequestMapping(value = "deleteImageDir")
    @ResponseBody
    public Response<Boolean> deleteDir() {
        log.info("删除  deleteDir");
        OpenFileUtils.removeTempFile(Dockers.getHomeImagesDir());
        return Response.success(true);
    }
}
