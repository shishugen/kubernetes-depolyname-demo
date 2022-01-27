package com.c3stones.service;

import com.obs.services.ObsClient;
import com.obs.services.ObsConfiguration;
import com.obs.services.exception.ObsException;
import com.obs.services.model.ListPartsRequest;
import com.obs.services.model.ListPartsResult;
import com.obs.services.model.Multipart;
import com.obs.services.model.PutObjectResult;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @ClassName: Test
 * @Description: TODO
 * @Author: stone
 * @Date: 2021/3/23 10:11
 */
public class Test {




    private static final String endPoint = "https://obs.cn-north-4.myhuaweicloud.com";

    private static final String ak = "O3IGKD6PUIZWDQICF5YD";

    private static final String sk = "emfI2NFCDfz13X30CyKUDnWmQMs6dmFIYRjVkk1s";

    private static ObsClient obsClient;

    private static String bucketName = "portal-video";

    private static void listAllParts(String uploadId)
            throws ObsException
    {
        System.out.println("Listing all parts......");
        ListPartsRequest listPartsRequest = new ListPartsRequest(bucketName, "adfa2f07999584bac648e411d0bf780f.mp4\n", uploadId);
        ListPartsResult partListing = obsClient.listParts(listPartsRequest);
        for (Multipart part : partListing.getMultipartList())
        {
            System.out.println("\tPart#" + part.getPartNumber() + ", ETag=" + part.getEtag());
        }
        System.out.println();
    }

    public static void main(String[] args) {
        try {
            ObsConfiguration config = new ObsConfiguration();
            config.setSocketTimeout(30000);
            config.setConnectionTimeout(10000);
            config.setEndPoint(endPoint);
            obsClient = new ObsClient(ak, sk, config);
            listAllParts("0000017E907009F866D45D5B956E6E54");

        } catch (ObsException e) {
            e.printStackTrace();
        }



    }



}
