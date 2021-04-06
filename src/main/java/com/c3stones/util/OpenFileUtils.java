package com.c3stones.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * @ClassName: FileUtils
 * @Description: TODO
 * @Author: stone
 * @Date: 2021/4/1 10:04
 */
public class OpenFileUtils {

    public static String fileConverString(MultipartFile file) throws IOException {
        return  readFile(file.getInputStream());
    }

    /**
     * @param file         上传文件
     * @param saveFilePath 保存文件名
     * @throws IOException
     */

    public static void writeFile(MultipartFile file, File saveFilePath) throws IOException {
        saveFilePath.createNewFile();
        FileOutputStream outputStream = new FileOutputStream(saveFilePath);
        outputStream.write(fileConverString(file).getBytes());
        if(outputStream != null){
            outputStream.flush();
            outputStream.close();
        }
    }
   /**
     * @param file         上传文件
     * @param saveFilePath 保存文件名
     * @throws IOException
     */

    public static void writeFile(File file, File saveFilePath) throws IOException {
        saveFilePath.createNewFile();
        FileOutputStream outputStream = new FileOutputStream(saveFilePath);
        outputStream.write(readFile(new FileInputStream(file)).getBytes());
        if(outputStream != null){
            outputStream.flush();
            outputStream.close();
        }
    }

    /**
     * 读取文件
     * @param inputStream
     * @return
     */
    public static String readFile(InputStream inputStream){
        Reader reader = null;
        String scriptContent = "";
        BufferedReader bufferedReader = null;
        try {
            reader = new InputStreamReader(inputStream , StandardCharsets.UTF_8);
             bufferedReader = new BufferedReader(reader);
            StringBuilder stringBuilder = new StringBuilder();
            String lineStr;
            //逐行读取
            while (null != (lineStr = bufferedReader.readLine())) {
                //TODO 按你的需求处理
                stringBuilder.append(lineStr).append("\n");
            }
            scriptContent = stringBuilder.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                if(inputStream != null){
                    inputStream.close();
                }
                if (reader != null){
                    reader.close();
                }
                if (bufferedReader != null){
                    bufferedReader.close();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return  scriptContent;
    }

    /**
     * 以字符为单位读取文件，常用于读文本，数字等类型的文件
     */
    public static String readFileByChars(String fileName) throws FileNotFoundException {

        return readFile(new FileInputStream(new File(fileName)));
    }

    /**
     * 以行为单位读取文件，常用于读面向行的格式化文件
     */
    public static String readFileByLines(String fileName) {
        File file = new File(fileName);
        BufferedReader reader = null;
        StringBuilder builder = new StringBuilder();
        try {
            System.out.println("以行为单位读取文件内容，一次读一整行：");
            reader = new BufferedReader(new FileReader(file));
            String tempString = null;
            int line = 1;
            // 一次读入一行，直到读入null为文件结束
            while ((tempString = reader.readLine()) != null) {
                // 显示行号
                line++;
                builder.append(tempString);
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                }
            }
        }
        return builder.toString();
    }


    /**
     * 删除文件
     * @param file
     */
    public static void  deleteFile(File file){
        if(file != null && file.isDirectory()){
            File[] files = file.listFiles();
            for (File f:files ) {
                if(f.isDirectory() && f.listFiles().length > 0){
                    deleteFile(f);
                }
                f.delete();
            }
        }
        file.delete();
    }

}
