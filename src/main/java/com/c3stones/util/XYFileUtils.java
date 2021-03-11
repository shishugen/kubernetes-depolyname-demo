package com.c3stones.util;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName: XYFileUtils
 * @Description: TODO
 * @Author: stone
 * @Date: 2021/1/25 12:37
 */
public class XYFileUtils {


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

    public static void main(String[] args) {
        String path = "E:\\dockerfile\\dockerfile\\Dockerfile";
        List<LineReplaceEntity> list = new ArrayList<>();
        //将第二行替换为你想要的内容
        list.add(new LineReplaceEntity(2,"test"));
       // rpFileContentByLineNo(path,list);
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
