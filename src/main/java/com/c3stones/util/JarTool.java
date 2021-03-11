package com.c3stones.util;

import org.apache.commons.lang.StringUtils;
import java.io.*;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

/**
 * @ClassName: JarTool
 * @Description: TODO
 * @Author: stone
 * @Date: 2021/1/26 9:46
 */
public class JarTool {

    /***
     *
     * @param jarPath 源jar包路径
     * @param newJarPath 新生成的jar存放路径
     * @param confPath 需要修改的配置文件相对路径
     * @param jarConfChange 配置文件内容替换逻辑
     * @return
     * @throws IOException
     */
    public static final File change(String jarPath,String newJarPath,String confPath,StringReplace jarConfChange) throws IOException {

        if(StringUtils.isNotBlank(jarPath)) {
            File file = new File(jarPath);
            JarFile jarFile = new JarFile(file);
            JarEntry entry = jarFile.getJarEntry(confPath.replace("\\", "/"));
            List<JarEntry> lists = new LinkedList<JarEntry>();
            for (Enumeration<JarEntry> entrys = jarFile.entries(); entrys.hasMoreElements();) {
                JarEntry jarEntry = entrys.nextElement();
                lists.add(jarEntry);
            }

            String buffer = confContent(jarFile.getInputStream(entry), jarConfChange);

            return write(lists,jarFile,newJarPath, buffer,confPath);
        }
        return null;
    }

    /**
     * 获取替换后的文件内容
     * @param inputStream
     * @param jarConfChange
     * @return
     * @throws IOException
     */
    private static final String confContent(InputStream inputStream,StringReplace jarConfChange) throws IOException {
        InputStreamReader isr = new InputStreamReader(inputStream);
        BufferedReader br = new BufferedReader(isr);
        StringBuffer buf = new StringBuffer();
        String line = null;
        try {
            while ((line = br.readLine()) != null) {
                buf.append(jarConfChange.process(line));
            }
        } finally {
            br.close();
            isr.close();
        }
        return buf.toString();
    }

    /**
     *	 写入到新的jar文件中
     * @param lists
     * @param jarFile
     * @param newJarPath
     * @param content
     * @param confPath
     * @return
     * @throws IOException
     */
    private static final File write(List<JarEntry> lists,JarFile jarFile,
                                    String newJarPath, String content,String confPath) throws IOException {
        JarOutputStream jos = null;

        File outFile = new File(newJarPath);
        if (outFile.exists()) {
            outFile.delete();
        }
        outFile.createNewFile();
        FileOutputStream fos = new FileOutputStream(outFile);
        jos = new JarOutputStream(fos);

        try {
            for (JarEntry je : lists) {
                JarEntry newEntry = new JarEntry(je.getName());
                jos.putNextEntry(newEntry);
                if (je.getName().equals(confPath)) {
                    jos.write(content.getBytes());
                    continue;
                }
                if (jarFile.getInputStream(je) != null) {
                    byte[] b = new byte[jarFile.getInputStream(je).available()];
                    jarFile.getInputStream(je).read(b);
                    jos.write(b);
                }
            }
        } finally {
            // 关闭流
            if (jos != null) {
                try {
                    jos.close();
                } catch (IOException e) {
                    jos = null;
                }
            }
            fos.close();
        }

        return outFile;
    }
    public static void main(String[] args) throws IOException {
        String srcJarPath = "F:\\temp\\test.jar";
        String newJarPath = "F:\\temp\\test1.jar";
        String confPath = "BOOT-INF/classes/application.properties";
        File fi = change(srcJarPath, newJarPath, confPath, srcConf -> {
            if (srcConf.startsWith("admin")) {

                return "admin = 12345";
            }
            return srcConf;
        });
        System.out.println(fi.getName());
    }
}
