package com.thewangzl.sf.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.thewangzl.sf.security.AuthProvider;
import com.thewangzl.sf.security.LoginAuthFailHandler;
import com.thewangzl.sf.security.LoginUrlEntryPoint;

@EnableWebSecurity
@EnableGlobalMethodSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

	/**
	 * HTTP权限控制
	 */
	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http.authorizeRequests().antMatchers("/admin/login").permitAll()//
				.antMatchers("/static/**").permitAll()//
				.antMatchers("/user/login").permitAll()//
				.antMatchers("/admin/**").hasRole("ADMIN")//
				.antMatchers("/user/**").hasAnyRole("ADMIN","USER")//
				.antMatchers("/api/user/**").hasAnyRole("ADMIN", "USER")//
				.and()//
				.formLogin()//
				.loginProcessingUrl("/login")//
				.failureHandler(loginAuthFailHandler())//
				.and()//
				.logout()//
				.logoutUrl("/logout")//
				.logoutSuccessUrl("/logout/page")//
				.deleteCookies("JSESSIONID")//
				.invalidateHttpSession(true)//
				.and()//
				.exceptionHandling()//
				.authenticationEntryPoint(loginUrlEntryPoint())//
				;

		http.csrf().disable();
		http.headers().frameOptions().sameOrigin();
	}
	
//	@Bean
//	public BCryptPasswordEncoder PasswordEncoder() {
//		return new BCryptPasswordEncoder();
//	}
	
	@Autowired
	public void configGlobal(AuthenticationManagerBuilder auth) throws Exception {
//		auth.inMemoryAuthentication().withUser("admin").password(PasswordEncoder().encode("admin")).roles("ADMIN");
	//	auth.inMemoryAuthentication().withUser("admin").password("admin").roles("ADMIN");
		
		auth.authenticationProvider(authProvider()).eraseCredentials(true);
	}
	
	@Bean
	public AuthProvider authProvider() {
		return new AuthProvider();
	}
	
	@Bean
	public LoginUrlEntryPoint loginUrlEntryPoint() {
		return new LoginUrlEntryPoint("/user/login");
	}
	
	@Bean
	public LoginAuthFailHandler loginAuthFailHandler() {
		return new LoginAuthFailHandler(loginUrlEntryPoint());
	}
}
