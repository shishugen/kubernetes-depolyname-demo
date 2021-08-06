package com.c3stones.http;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.c3stones.client.BaseConfig;
import com.c3stones.entity.HarborImage;
import com.c3stones.entity.HarborTemp;
import com.c3stones.util.DateUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import sun.misc.BASE64Encoder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @ClassName: HttpHarbor
 * @Description: TODO
 * @Author: stone
 * @Date: 2020/9/4 10:59
 */
@Component
public class HttpHarbor extends BaseConfig {

    @Autowired
    private RestTemplate harborClient ;

    public  ResponseEntity<JSONArray> send(String urlName , HttpMethod httpMethod, Map params){
        ResponseEntity<JSONArray> responseEntity = new ResponseEntity(HttpStatus.NOT_FOUND);
        HttpHeaders headers = new HttpHeaders();
        HttpMethod method = httpMethod;
        headers.setContentType(MediaType.APPLICATION_JSON);
        String url = harborUrl + "/api/"+urlName;
        byte[]  userPass = (harborUser+":"+harborPassword).getBytes();
        headers.add("authorization" , "Basic "+new BASE64Encoder().encode(userPass));
        HttpEntity<MultiValueMap<String, String>> requestEntity2 = new HttpEntity(params,headers);
        try {
            responseEntity = harborClient.exchange(url, method, requestEntity2, JSONArray.class);
        }catch (HttpClientErrorException e){
            //404
            if(e.getStatusCode().value() == HttpStatus.NOT_FOUND.value()){
           //    return responseEntity;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return responseEntity;
    }

    public  ResponseEntity<JSONArray> send(String urlName , HttpMethod httpMethod){
        ResponseEntity<JSONArray> responseEntity = new ResponseEntity(HttpStatus.NOT_FOUND);
        HttpHeaders headers = new HttpHeaders();
        HttpMethod method = httpMethod;
        headers.setContentType(MediaType.APPLICATION_JSON);
        String url = harborUrl + "/api/"+urlName;
        RestTemplate harborClient = new RestTemplate();
        byte[]  userPass = (harborUser+":"+harborPassword).getBytes();
        headers.add("authorization" , "Basic "+new BASE64Encoder().encode(userPass));
        HttpEntity<MultiValueMap<String, String>> requestEntity2 = new HttpEntity(headers);
        try {
            responseEntity = harborClient.exchange(url, method, requestEntity2, JSONArray.class);
        }catch (HttpClientErrorException e){
            //412
            if(e.getStatusCode().value() == HttpStatus.PRECONDITION_FAILED.value()){
                return  new ResponseEntity(HttpStatus.PRECONDITION_FAILED);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return responseEntity;
    }

    public List<HarborImage> harborList(String harborProjectName ,String version){

        List<HarborImage> harborList = new ArrayList();
        JSONArray jsonObject = harborClient.getForObject(harborUrl+"/api/projects?name="+ BaseConfig.initConfig().getHarborImageProjectName(), JSONArray.class);
        if(jsonObject != null && jsonObject.size() > 0){
            JSONObject objectJSONObject = jsonObject.getJSONObject(0);
            HarborTemp harborProject = objectJSONObject.toJavaObject(HarborTemp.class);
            JSONArray repositoriesArray = harborClient.getForObject(harborUrl+ "/api/repositories?project_id=" + harborProject.getProjectId(), JSONArray.class);
            for (int j = 0 ; j < repositoriesArray.size(); j++){
                JSONObject repositories = repositoriesArray.getJSONObject(j);
                HarborTemp harborRepositories = repositories.toJavaObject(HarborTemp.class);
                String name = harborRepositories.getName();
                JSONArray harborVersionAarry = harborClient.getForObject( harborUrl+"/api/repositories/"+ name +"/tags", JSONArray.class);
                for (int k = 0 ; k < harborVersionAarry.size(); k++){
                    JSONObject harborVersionObject = harborVersionAarry.getJSONObject(k);
                    HarborTemp harborVersion = harborVersionObject.toJavaObject(HarborTemp.class);
                    HarborImage harbor = new HarborImage();
                    harbor.setCreated(DateUtils.StringFormat(harborRepositories.getUpdateTime()));
                    harbor.setImageName(name);
                    harbor.setVersion(harborVersion.getName());
                     if(StringUtils.isNotBlank(harborProjectName) && StringUtils.isNotBlank(version)){
                         if(name.contains(harborProjectName)&&harborVersion.getName().contains(version)){
                             harborList.add(harbor);
                         }
                     }else if(StringUtils.isNotBlank(harborProjectName) ){
                         if(name.contains(harborProjectName)){
                             harborList.add(harbor);
                         }
                     }else if(StringUtils.isNotBlank(version)){
                         if( harborVersion.getName().contains(version)){
                             harborList.add(harbor);
                         }
                     } else{
                         harborList.add(harbor);
                     }



                   /* if(StringUtils.isNotBlank(version)){
                        if (harborVersion.getName().equals(version)){
                            harborList.add(harbor);
                        }
                    }else{
                        harborList.add(harbor);
                    }*/
                }
            }
        }
        return harborList;
    }
}
