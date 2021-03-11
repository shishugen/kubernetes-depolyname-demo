package com.c3stones.util;

/**
 * @ClassName: UnRar
 * @Description: TODO
 * @Author: stone
 * @Date: 2021/2/4 10:29
 */
import com.github.junrar.Archive;
import com.github.junrar.rarfile.FileHeader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class UnRar {

    public static void unRar(File rarFile, String outDir) throws Exception {
        File outFileDir = new File(outDir);
        if (!outFileDir.exists()) {
            boolean isMakDir = outFileDir.mkdirs();
            if (isMakDir) {
                System.out.println("创建压缩目录成功");
            }
        }
        Archive archive = new Archive(new FileInputStream(rarFile));
        FileHeader fileHeader = archive.nextFileHeader();
        while (fileHeader != null) {
            if (fileHeader.isDirectory()) {
                fileHeader = archive.nextFileHeader();
                continue;
            }
            File out = new File(outDir +File.separator+ fileHeader.getFileNameString());
            if (!out.exists()) {
                if (!out.getParentFile().exists()) {
                    out.getParentFile().mkdirs();
                }
                out.createNewFile();
            }
            FileOutputStream os = new FileOutputStream(out);
            archive.extractFile(fileHeader, os);

            os.close();

            fileHeader = archive.nextFileHeader();
        }
        archive.close();
    }

    public static void main(String[] args) {
        String originDir = "D:\\JavaPro\\TestFile\\";

        String rarPath = originDir + "new.rar";
        File rarFile = new File(rarPath);

        try {
            unRar(new File("E:\\dockerfile\\dist.rar"), "E:\\dockerfile\\");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
