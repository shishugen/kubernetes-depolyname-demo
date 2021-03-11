package com.c3stones.controller;

import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import redis.clients.jedis.Jedis;

/**
 * 系统首页Controller
 * 
 * @author CL
 *
 */
@Controller
public class IndexController {

	/**
	 * 首页
	 * 
	 * @return
	 */
	@GetMapping(value = "index")
	public String index(Model model, HttpSession httpSession) {
		model.addAttribute("user", httpSession.getAttribute("user"));
		return "index";
	}
	/**
	 * 首页
	 *
	 * @return
	 */
	@GetMapping(value = "explain")
	public String explain(Model model, HttpSession httpSession) {
		model.addAttribute("user", httpSession.getAttribute("user"));
		return "explain";
	}
	
	/**
	 * 控制台
	 * 
	 * @return
	 */
	@GetMapping(value = "view")
	public String view() {
		return "pages/view";
	}

	public static void main(String[] args) {
		Jedis jedis = new Jedis("10.49.0.12", 30020);
		//Jedis jedis = new Jedis("localhost", 6379);
		jedis.auth("123456");
		System.out.println("服务正在运行："+ jedis.ping());
	}
}
