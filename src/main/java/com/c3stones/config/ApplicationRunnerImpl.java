package com.c3stones.config;

import cn.hutool.cron.CronUtil;
import com.c3stones.client.BaseConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * @ClassName: ApplicationRunnerImpl
 * @Description: TODO
 * @Author: stone
 * @Date: 2021/4/6 15:07
 */
@Component
@Slf4j
public class ApplicationRunnerImpl implements ApplicationRunner {
    @Override
    public void run(ApplicationArguments args) throws Exception {
        File file = new File(System.getProperty("user.home") + File.separator + ".kube-deployment");
        if(!file.isDirectory()){
            file.mkdirs();
        }
        File file2 = new File(System.getProperty("user.home") + File.separator + ".kube-deployment/jar/");
        if(!file2.isDirectory()){
            file2.mkdirs();
        }

        BaseConfig.initConfig();
        log.info("启动完成……");
    }
}
