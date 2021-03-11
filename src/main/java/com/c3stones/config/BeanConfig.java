package com.c3stones.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;


/**
 * @ClassName: BeanConfig
 * @Description: TODO
 * @Author: stone
 * @Date: 2021/1/25 15:52
 */
@Configuration
public class BeanConfig {


    @Bean
    public RestTemplate getRestTemplate(){
        return new RestTemplate();
    }




}
