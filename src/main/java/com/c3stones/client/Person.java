package com.c3stones.client;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @ClassName: Person
 * @Description: TODO
 * @Author: stone
 * @Date: 2021/1/28 9:33
 */
public class Person {

    public static void main(String[] args) {
        File file = new File("D:" + File.separator + "dock");
        try {
            file.mkdir();
            // R ： 只读文件属性。A：存档文件属性。S：系统文件属性。H：隐藏文件属性。
            String sets = "attrib +H \"" + file.getAbsolutePath() + "\"";
            System.out.println(sets);
            // 运行命令
            Runtime.getRuntime().exec(sets);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
