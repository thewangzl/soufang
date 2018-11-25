package com.thewangzl.sf.security;

import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

import com.google.common.collect.ImmutableMap;

/**
 * 基于角色的登陆入口控制器
 * @author thewangzl
 *
 */
public class LoginUrlEntryPoint extends LoginUrlAuthenticationEntryPoint {

	private final Map<String,String> authEntryPointMap;
	
	private PathMatcher pathMatcher = new AntPathMatcher();
	
	public LoginUrlEntryPoint(String loginFormUrl) {
		super(loginFormUrl);
		authEntryPointMap = ImmutableMap.<String, String> builder()//
				.put("/user/**", "/user/login")//普通用户登录入口映射
				.put("/admin/**","/admin/login")//管理员入口登陆映射
				.build()//
				;
	}
	
	/**
	 *  根据请求跳转到指定的页面，父类是默认使用loginFormUrl
	 */
	@Override
	protected String determineUrlToUseForThisRequest(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException exception) {

		String uri = request.getRequestURI().replace(request.getContextPath(), "");
		for(Entry<String,String> entry : this.authEntryPointMap.entrySet()) {
			if(this.pathMatcher.match(entry.getKey(), uri)) {
				return entry.getValue();
			}
		}
		return super.determineUrlToUseForThisRequest(request, response, exception);
	}

}
