package com.c3stones.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.extra.mail.MailAccount;
import cn.hutool.extra.mail.MailUtil;
import com.c3stones.client.BaseConfig;
import com.c3stones.common.Response;
import com.c3stones.entity.Config;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @ClassName: MailController
 * @Description: TODO
 * @Author: stone
 * @Date: 2022/5/27 10:31
 */
@Controller
@Slf4j
@RequestMapping(value = "mail")
public class MailController {

    /**
     * 添加
     *
     * @return
     */
    @RequestMapping(value = "test")
    @ResponseBody
    public Response test(Config config) {
        try {
            log.error("集群故障……发送邮件");
            MailAccount account = new MailAccount();
            account.setHost(config.getMailHost());
            account.setPort(config.getMailPort());
            account.setAuth(true);
            account.setFrom(config.getMailUser());
            account.setUser(config.getMailUser());
            account.setPass(config.getMailPass());
            //标题
            String subject =config.getMailSubject()+"……K8S群集故障";
            //内容
            String content ="测试邮箱是否正确……";
            if (StringUtils.isNotBlank(config.getMailPerson())){
                String[] split = config.getMailPerson().split(",");
                MailUtil.send(account,  CollUtil.newArrayList(split),
                        subject, content, false);
            }else{
                log.error("请设置收邮件人员……");
            }

        }catch (Exception e){
            e.printStackTrace();
            return Response.error("失败……"+e.getMessage());
        }
        return Response.success("OK");
    }

}
