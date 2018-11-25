package com.thewangzl.sf.config;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import com.alibaba.druid.spring.boot.autoconfigure.DruidDataSourceBuilder;

@Configuration
@EnableJpaRepositories(basePackages="com.thewangzl.sf.repository")
public class JPAConfig {

//	@Bean
//	public DataSource dataSource(Environment env) {
//		return DruidDataSourceBuilder.create().build(env, "spring.datasource.druid.");
//	}
	
//	@Bean
//	public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
//		HibernateJpaVendorAdapter jpaVendorAdapter = new HibernateJpaVendorAdapter();
//		jpaVendorAdapter.setGenerateDdl(false);
//		
//		LocalContainerEntityManagerFactoryBean entityManagerFactoryBean = new LocalContainerEntityManagerFactoryBean();
//		entityManagerFactoryBean.setDataSource(dataSource);
//		entityManagerFactoryBean.setJpaVendorAdapter(jpaVendorAdapter);
//		entityManagerFactoryBean.setPackagesToScan("com.thewangzl.sf.domain");
//		
//		return entityManagerFactoryBean;
//	}
	
	@Bean
	public EntityManagerFactory entityManagerFactory(JpaProperties jpaProperties, @Qualifier("dataSource") DataSource dataSource) {
		HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
		LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
		factory.setJpaVendorAdapter(vendorAdapter);
		factory.setPackagesToScan("com.thewangzl.sf.domain");
		factory.setDataSource(dataSource);// 数据源
		factory.setJpaPropertyMap(jpaProperties.getProperties());
		factory.afterPropertiesSet();// 在完成了其它所有相关的配置加载以及属性设置后,才初始化
		EntityManagerFactory bean = factory.getObject();
		return bean;
	}
	
	@Bean
	public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
		JpaTransactionManager jpaTransactionManager = new JpaTransactionManager();
		jpaTransactionManager.setEntityManagerFactory(entityManagerFactory);
		
		return jpaTransactionManager;
	}
}
