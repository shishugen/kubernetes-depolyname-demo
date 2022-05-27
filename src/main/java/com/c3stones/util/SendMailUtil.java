package com.c3stones.util;

/**
 * @ClassName: SendMailUtil
 * @Description: TODO
 * @Author: stone
 * @Date: 2022/5/23 9:35
 */
import cn.hutool.core.collection.CollUtil;
import cn.hutool.extra.mail.MailAccount;
import cn.hutool.extra.mail.MailUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;



@Component
public class SendMailUtil {


    private String host;

    public static void send(){


    }
    public void send2(){
        System.out.println("host"+host);

    }


    public static void main(String[] args) {
        MailAccount account = new MailAccount();
        account.setHost("smtp.qq.com");
        account.setPort(25);
        account.setAuth(true);
        account.setFrom("576108653@qq.com");
        account.setUser("576108653@qq.com");
        account.setPass("yuszxblylmicbbaj");

        MailUtil.send(account, CollUtil.newArrayList("576108653@qq.com"), "测试", "邮件来自Hutool测试", false);


    }

}