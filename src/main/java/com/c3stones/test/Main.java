//package name of the demo.
package com.c3stones.test;

//Import the external dependencies.

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.cloud.apigateway.sdk.utils.Client;
import com.cloud.apigateway.sdk.utils.Request;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        //Create a new request.
        Request request = new Request();
        try {
            //Set the AK/SK to sign and authenticate the request.
            request.setKey("A7ANWGIKSCIXWPBQDBMJ");
            request.setSecret("WZ8G8ApjwfaOZrtAUDfFDwdBc1UtIzzptg321COS");

            //The following example shows how to set the request URL and parameters to query a VPC list.

            //Specify a request method, such as GET, PUT, POST, DELETE, HEAD, and PATCH.
            request.setMethod("GET");

            //Set a request URL in the format of https://{Endpoint}/{URI}.
           // String url = "https://cce.cn-south-1.myhuaweicloud.com/api/v3/projects/0c9c9d416000f38b2f1bc017bcabd203/clusters/d3f06cd6-ca7c-11eb-a6c8-0255ac1022cb/nodes/d47e8d13-ca7c-11eb-a6c8-0255ac1022cb";
           // String url = "https://d3f06cd6-ca7c-11eb-a6c8-0255ac1022cb.cce.cn-south-1.myhuaweicloud.com/api/v1/pods";
            String url = "https://d3f06cd6-ca7c-11eb-a6c8-0255ac1022cb.cce.cn-south-1.myhuaweicloud.com/api/v1/resourcequotas";
           // String url = "https://d3f06cd6-ca7c-11eb-a6c8-0255ac1022cb.cce.cn-south-1.myhuaweicloud.com/api/v1/services";
          //  String url = "https://d3f06cd6-ca7c-11eb-a6c8-0255ac1022cb.cce.cn-south-1.myhuaweicloud.com/api/v1/namespaces/app-sys/services/env-mysql";
           // String url = "https://d3f06cd6-ca7c-11eb-a6c8-0255ac1022cb.cce.cn-south-1.myhuaweicloud.com/api/v1/namespaces/app-sys/pods/env-nacos";
            request.setUrl(url);

            //Add header parameters, for example, x-domain-id for invoking a global service and x-project-id for invoking a project-level service.
            request.addHeader("Content-Type", "application/json");

            //Add a body if you have specified the PUT or POST method. Special characters, such as the double quotation mark ("), contained in the body must be escaped.
            //request.setBody("demo");

        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        CloseableHttpClient client = null;
        try {
            //Sign the request.
            HttpRequestBase signedRequest = Client.sign(request);
           // HttpEntity<MultiValueMap<String, String>> requestEntity2 = new HttpEntity(headers);

            //Send the request.
            client = HttpClients.custom().build();
            HttpResponse execute = client.execute(signedRequest);

            HttpResponse response = execute;
            HttpEntity entity = response.getEntity();
            //Print the status line of the response.
            System.out.println(response.getStatusLine().toString());

            //Print the header fields of the response.
            Header[] resHeaders = response.getAllHeaders();
            for (Header h : resHeaders) {
              //  System.out.println(h.getName() + ":" + h.getValue());
            }

            //Print the body of the response.
            HttpEntity resEntity = response.getEntity();
            if (resEntity != null) {
                //System.out.println(System.getProperty("line.separator") + EntityUtils.toString(resEntity, "UTF-8"));
                ;//转换成JSON格式
              /*  JSONObject datas = JSONObject.parseObject(EntityUtils.toString(resEntity, "UTF-8"));
                System.out.println(datas);*/

                String s = System.getProperty("line.separator") + EntityUtils.toString(resEntity, "UTF-8");
                System.out.println(s);

                JSONObject datas = JSONObject.parseObject(s);


                JSONArray jsonArray = JSONArray.parseArray(datas.get("items").toString());
                List<PodList> podLists = jsonArray.toJavaList(PodList.class);
                System.out.println("pod-----"+podLists);
                for (int i = 0 ; i < jsonArray.size() ; i++){

              }




                //单容器
             /* Pod pod = JSONObject.parseObject(datas.toJSONString(), Pod.class);
                System.out.println(pod);
                String name = pod.getSpec().getContainers().get(0).getName();
                System.out.println("777-------"+name);*/



              /*  JSONArray records = JSONObject.parseArray(datas.get("items").toString());
                System.out.println(records.toJSONString());*/



                //JSON.parseObject(EntityUtils.toString(resEntity, "UTF-8"), PodList.class);
               // PodList podLists1 = JSONObject.parseObject(datas.toJSONString(), PodList.class);
              //  System.out.println(podLists1);



                //System.out.println("----:"+records);
               // List<Pod> dataArr = JSONArray.parseArray(records.toJSONString(),Pod.class);
               // Pod pod1 = JSONArray.to(records, Pod.class);

            /*    List<Pod> podList = JSONArray.parseArray(records.toJSONString(), Pod.class);
                System.out.println(podList);*/

              //  String lineArray = JSONArray.toJSONString(records);
                // List<SingleOrder> singleOrderList = JSON.parseArray(JSON.parseObject(lineArray).getString("singleOrderList"),SingleOrder.class);

                // List<Pod> singleOrderList = JSON.parseArray(datas.toJSONString(),Pod.class);



            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (client != null) {
                    client.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

   class   Test{

   }

}