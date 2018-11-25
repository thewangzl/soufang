package com.thewangzl.sf.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.thewangzl.sf.base.ApiResponse;

@Controller
public class IndexController {

	@GetMapping({"/","/index"})
	public String index(Model model) {
		return "index";
	}
	
	@GetMapping("/ok")
	@ResponseBody
	public ApiResponse ok() {
		return ApiResponse.ofMessage(200, "访问成功");
	}
	
	@GetMapping("404")
	public String notFound() {
		return "404";
	}
	
	@GetMapping("403")
	public String accessError() {
		return "403";
	}
	
	@GetMapping("500")
	public String internalError() {
		return "500";
	}
	
	@GetMapping("/logout/page")
	public String logoutPage() {
		return "logout";
	}
}
