package com.thewangzl.sf.security;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;

/**
 * 登陆验证失败处理器
 * @author thewangzl
 *
 */
public class LoginAuthFailHandler extends SimpleUrlAuthenticationFailureHandler {

	private final LoginUrlEntryPoint loginUrlEntryPoint;

	public LoginAuthFailHandler(LoginUrlEntryPoint loginUrlEntryPoint) {

		this.loginUrlEntryPoint = loginUrlEntryPoint;
	}
	
	@Override
	public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException exception) throws IOException, ServletException {
		String targetUrl = this.loginUrlEntryPoint.determineUrlToUseForThisRequest(request, response, exception);
		targetUrl += "?" + exception.getMessage();
		super.setDefaultFailureUrl(targetUrl);
		super.onAuthenticationFailure(request, response, exception);
	}
	
	
}
