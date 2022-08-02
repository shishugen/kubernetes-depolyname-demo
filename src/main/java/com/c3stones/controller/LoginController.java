package com.c3stones.controller;

import cn.hutool.core.util.StrUtil;
import com.c3stones.common.Response;
import com.c3stones.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;

/**
 * 系统登录Controller
 * 
 * @author CL
 *
 */
@Controller
public class LoginController {


	/**
	 * 用户
	 */
	@Value("${login.admin:admin}")
	private String loginAdmin;

	/**
	 * 密码
	 */
	@Value("${login.pass:123456}")
	private String loginPass;

	/**
	 * 登录页
	 * 
	 * @return
	 */
	@GetMapping(value = { "login", "" })
	public String login() {
		return "login";
	}

	/***
	 * 登录验证
	 * 
	 * @param user 系统用户
	 * @return
	 */
	@PostMapping(value = "login")
	@ResponseBody
	public Response<User> login(User user, HttpSession session) {
		if (StrUtil.isBlank(user.getUsername()) || StrUtil.isBlank(user.getPassword())) {
			return Response.error("用户名或密码不能为空");
		}
		User queryUser = new User();
		queryUser.setUsername(user.getUsername());
		queryUser.setNickname("管理员");
		if (loginAdmin.equals(user.getUsername()) &&loginPass.equals(user.getPassword())){
			session.setAttribute("user", queryUser);
           //session.setMaxInactiveInterval(60 * 1 * 60 * 24);
			return Response.success("登录成功", queryUser);
		}else{
			return Response.error("用户名或密码错误");
		}
	}

	/**
	 * 登出
	 * 
	 * @param httpSession
	 * @return
	 */
	@GetMapping(value = "logout")
	public String logout(HttpSession httpSession) {
		httpSession.invalidate();
		return "redirect:/login";
	}
	
}