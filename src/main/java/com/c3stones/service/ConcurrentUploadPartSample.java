package com.c3stones.service;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.obs.services.ObsClient;
import com.obs.services.ObsConfiguration;
import com.obs.services.exception.ObsException;
import com.obs.services.model.CompleteMultipartUploadRequest;
import com.obs.services.model.InitiateMultipartUploadRequest;
import com.obs.services.model.InitiateMultipartUploadResult;
import com.obs.services.model.ListPartsRequest;
import com.obs.services.model.ListPartsResult;
import com.obs.services.model.Multipart;
import com.obs.services.model.PartEtag;
import com.obs.services.model.UploadPartRequest;
import com.obs.services.model.UploadPartResult;
/**
 * @ClassName: ConcurrentUploadPartSample
 * @Description: TODO
 * @Author: stone
 * @Date: 2022/1/19 10:06
 */
public class ConcurrentUploadPartSample {

    private static final String endPoint = "https://obs.cn-north-4.myhuaweicloud.com";

    private static final String ak = "O3IGKD6PUIZWDQICF5YD";

    private static final String sk = "emfI2NFCDfz13X30CyKUDnWmQMs6dmFIYRjVkk1s";

    private static ObsClient obsClient;

    private static String bucketName = "portal-video";

    private static String objectKey = "tesdd.mp4";


    private static ExecutorService executorService = Executors.newFixedThreadPool(5);

    private static List<PartEtag> partETags = Collections.synchronizedList(new ArrayList<PartEtag>());

    public static void main(String[] args)
            throws IOException
    {
        ObsConfiguration config = new ObsConfiguration();
        config.setSocketTimeout(30000);
        config.setConnectionTimeout(10000);
        config.setEndPoint(endPoint);
        try {
            /*
             * Constructs a obs client instance with your account for accessing OBS
             */
            obsClient = new ObsClient(ak, sk, config);

            /*
             * Create bucket
             */
            System.out.println("Create a new bucket for demo\n");
            //obsClient.createBucket(bucketName);

            /*
             * Claim a upload id firstly
             */
            String uploadId = claimUploadId();
            System.out.println("Claiming a new upload id " + uploadId + "\n");
             // 5MB
            long partSize = 1 * 1024 * 1024l;
          //  File sampleFile = createSampleFile();
            File sampleFile = new File("E:\\test.mp4");
            long fileLength = sampleFile.length();
            long partCount = fileLength % partSize == 0 ? fileLength / partSize : fileLength / partSize + 1;
            System.out.println("partCount=="+partCount);
            System.out.println("fileLength=="+fileLength);
            if (partCount > 10000) {
                throw new RuntimeException("Total parts count should not exceed 10000");
            } else {
                System.out.println("Total parts count " + partCount + "\n");
            }
            /*
             * Upload multiparts to your bucket
             */
            System.out.println("Begin to upload multiparts to OBS from a file\n");
            System.out.println("开始…………");
            for (int i = 0; i < partCount; i++){
                long offset = i * partSize;
                long currPartSize = (i + 1 == partCount) ? fileLength - offset : partSize;
                executorService.execute(new PartUploader(sampleFile, offset, currPartSize, i + 1, uploadId));
            }

            /*
             * Wait for all tasks to finish
             */
            executorService.shutdown();
            while (!executorService.isTerminated()){
                try {
                    executorService.awaitTermination(5, TimeUnit.SECONDS);
                }  catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            /*
             * Verify whether all tasks are finished
             */
            if (partETags.size() != partCount)
            {
                throw new IllegalStateException("Some parts are not finished");
            }
            else
            {
                System.out.println("Succeed to complete multiparts into an object named " + objectKey + "\n");
            }

            /*
             * View all parts uploaded recently
             */
            listAllParts(uploadId);

            /*
             * Complete to upload multiparts
             */
            completeMultipartUpload(uploadId);

        }
        catch (ObsException e)
        {
            System.out.println("Response Code: " + e.getResponseCode());
            System.out.println("Error Message: " + e.getErrorMessage());
            System.out.println("Error Code:       " + e.getErrorCode());
            System.out.println("Request ID:      " + e.getErrorRequestId());
            System.out.println("Host ID:           " + e.getErrorHostId());
        }
        finally
        {
            if (obsClient != null)
            {
                try
                {
                    /*
                     * Close obs client
                     */
                    obsClient.close();
                }
                catch (IOException e)
                {
                }
            }
        }

    }

    private static class PartUploader implements Runnable
    {

        private File sampleFile;

        private long offset;

        private long partSize;

        private int partNumber;

        private String uploadId;

        public PartUploader(File sampleFile, long offset, long partSize, int partNumber, String uploadId)
        {
            this.sampleFile = sampleFile;
            this.offset = offset;
            this.partSize = partSize;
            this.partNumber = partNumber;
            this.uploadId = uploadId;
        }

        @Override
        public void run()
        {
            try
            {
                System.out.println("进入上传方法…………");
                UploadPartRequest uploadPartRequest = new UploadPartRequest();
                uploadPartRequest.setBucketName(bucketName);
                uploadPartRequest.setObjectKey(objectKey);
                uploadPartRequest.setUploadId(this.uploadId);
                uploadPartRequest.setFile(this.sampleFile);
                uploadPartRequest.setPartSize(this.partSize);
                uploadPartRequest.setOffset(this.offset);
                uploadPartRequest.setPartNumber(this.partNumber);

                System.out.println("this.partNumber"+uploadPartRequest.toString());
                System.out.println("this.objectKey"+objectKey);
                System.out.println("this.uploadId"+uploadId);
                UploadPartResult uploadPartResult = obsClient.uploadPart(uploadPartRequest);
                System.out.println("Part#" + this.partNumber + " done\n");
                partETags.add(new PartEtag(uploadPartResult.getEtag(), uploadPartResult.getPartNumber()));
                System.out.println(uploadPartResult.getEtag());
                System.out.println("上传数="+uploadPartResult.getPartNumber());
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    private static String claimUploadId()
            throws ObsException
    {
        InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest(bucketName, objectKey);
        InitiateMultipartUploadResult result = obsClient.initiateMultipartUpload(request);
        return result.getUploadId();
    }

    private static File createSampleFile()
            throws IOException
    {
        File file = File.createTempFile("obs-java-sdk-", ".txt");
        file.deleteOnExit();

        Writer writer = new OutputStreamWriter(new FileOutputStream(file));
        for (int i = 0; i < 10000; i++)
        {
            writer.write(UUID.randomUUID() + "\n");
            writer.write(UUID.randomUUID() + "\n");
        }
        writer.flush();
        writer.close();

        return file;
    }

    private static void completeMultipartUpload(String uploadId)
            throws ObsException
    {
        // Make part numbers in ascending order
        Collections.sort(partETags, new Comparator<PartEtag>()
        {

            @Override
            public int compare(PartEtag o1, PartEtag o2)
            {
                return o1.getPartNumber() - o2.getPartNumber();
            }
        });

        System.out.println("Completing to upload multiparts\n");
        CompleteMultipartUploadRequest completeMultipartUploadRequest =
                new CompleteMultipartUploadRequest(bucketName, objectKey, uploadId, partETags);
        obsClient.completeMultipartUpload(completeMultipartUploadRequest);
    }

    private static void listAllParts(String uploadId)
            throws ObsException
    {
        System.out.println("Listing all parts......");
        ListPartsRequest listPartsRequest = new ListPartsRequest(bucketName, objectKey, uploadId);
        ListPartsResult partListing = obsClient.listParts(listPartsRequest);
        for (Multipart part : partListing.getMultipartList())
        {
            System.out.println("\tPart#" + part.getPartNumber() + ", ETag=" + part.getEtag());
        }
        System.out.println();
    }

}
