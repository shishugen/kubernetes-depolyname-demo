package com.c3stones.util;

import com.c3stones.client.BaseConfig;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * @ClassName: XYFileUtils
 * @Description: TODO
 * @Author: stone
 * @Date: 2021/1/25 12:37
 */
public class  XYFileUtils {


    /**
     * 根据行号修改文件内容
     *
     * @param path
     * @param replaceList
     */
    public static void rpFileContentByLineNo(File file, List<LineReplaceEntity> replaceList) {
        BufferedReader br = null;
        FileReader in = null;
        // 内存流, 作为临时流
        CharArrayWriter tempStream = null;
        FileWriter out = null;
        int maxLineNo = 3;
        int index = 0;
        try {
            in = new FileReader(file);
            br = new BufferedReader(in);
            // 内存流, 作为临时流
            tempStream = new CharArrayWriter();
            // 替换
            String line = null;
            while ((line = br.readLine()) != null) {
                index++;
                if (index <= maxLineNo) {
                    for (LineReplaceEntity rpEntity : replaceList) {
                        if (index == rpEntity.getLineNo()) {
                            line = rpEntity.getReplaceStr();
                        }
                    }
                }
                // 将该行写入内存
                tempStream.write(line);
                // 添加换行符
                tempStream.append(System.getProperty("line.separator"));
            }
            // 将内存中的流 写入 文件
            out = new FileWriter(file);
            tempStream.writeTo(out);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
            tempStream.close();
            out.close();
            br.close();
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static String readZipFileName(String path) throws IOException {
        ZipEntry zipEntry = null;
        File file = new File(path);
        if(file.exists()){
            //解决包内文件存在中文时的中文乱码问题
            ZipInputStream zipInputStream = new ZipInputStream( new FileInputStream(path), Charset.forName("GBK"));
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                if(zipEntry.isDirectory()) {
                    System.out.println(zipEntry.getName());
                    return zipEntry.getName();
                }
            }
        }
        return  null;
    }

    /**
     * MultipartFile 转 File
     *
     * @param file
     * @throws Exception
     */
    public static File multipartFileToFile(MultipartFile file) throws Exception {
        File toFile = null;
        if (file.equals("") || file.getSize() <= 0) {
            file = null;
        } else {
            InputStream ins = null;
            ins = file.getInputStream();
            toFile = new File(BaseConfig.getHomeJarFile()+File.separator+file.getOriginalFilename());
            inputStreamToFile(ins, toFile);
            ins.close();
        }
        return toFile;
    }

    //获取流文件
    private static void inputStreamToFile(InputStream ins, File file) {
        try {
            OutputStream os = new FileOutputStream(file);
            int bytesRead = 0;
            byte[] buffer = new byte[8192];
            while ((bytesRead = ins.read(buffer, 0, 8192)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            os.close();
            ins.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        String zipFileName = "testt/";
        String name = zipFileName.substring(0,zipFileName.lastIndexOf("/"));
        System.out.println(name);

    }


    /**
     * @ClassName LineReplaceEntity
     * @Description TODO
     * @Author lxp
     * @Date 2020/06/17 10:29
     */
    public static class LineReplaceEntity {

        private static final long serialVersionUID = 1L;

        /**
         * 行号
         */
        private int lineNo;

        /**
         * 替换内容
         */
        private String replaceStr;

        public int getLineNo() {
            return lineNo;
        }

        public void setLineNo(int lineNo) {
            this.lineNo = lineNo;
        }

        public String getReplaceStr() {
            return replaceStr;
        }

        public void setReplaceStr(String replaceStr) {
            this.replaceStr = replaceStr;
        }

        public LineReplaceEntity(int lineNo, String replaceStr) {
            this.lineNo = lineNo;
            this.replaceStr = replaceStr;
        }
    }

}
