package com.thewangzl.sf.base;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ErrorAttributes;
import org.springframework.boot.autoconfigure.web.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

/**
 * 错误处理
 * @author thewangzl
 *
 */
@Controller
public class AppErrorController implements ErrorController {

	private static final String ERROR_PATH = "/error";
	
	private ErrorAttributes errorAttributes;
	
	@Override
	public String getErrorPath() {
		return ERROR_PATH;
	}

	@Autowired
	public AppErrorController(ErrorAttributes errorAttributes) {
		this.errorAttributes = errorAttributes;
	}
	
	/**
	 * Web页面错误处理
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value=ERROR_PATH, produces="text/html")
	public String errorPageHandler(HttpServletRequest request, HttpServletResponse response) {
		int status = response.getStatus();
		switch (status) {
		case 403:
			return "403";
		case 404:
			return "404";
		case 500:
			return "500";
		default:
			return "index";
		}
	}
	
	/**
	 * 除了Web页面外的错误处理
	 * @param request
	 * @return
	 */
	@RequestMapping(value=ERROR_PATH)
	@ResponseBody
	public ApiResponse errorApiHandler(HttpServletRequest request) {
		WebRequest attributes = new ServletWebRequest(request);
		
		Map<String,Object> attr = this.errorAttributes.getErrorAttributes((WebRequest) attributes, false);
		
		int status = this.getStatus(request);
		
		return ApiResponse.ofMessage(status, String.valueOf(attr.getOrDefault("message", "error")));
	}
	
	private int getStatus(HttpServletRequest request) {
		Integer status = (Integer) request.getAttribute("javax.servlet.error.status_code");
		if(status != null) {
			return status;
		}
		return 500;
	}

}
