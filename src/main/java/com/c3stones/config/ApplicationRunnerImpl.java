package com.c3stones.config;

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
        new File(System.getProperty("user.home") + File.separator + ".kube-deployment").mkdirs();
        log.info("初始化完成……");
    }
}
