package com.thewangzl.sf.config;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElasticSearchConfig {

	@Bean
	public TransportClient esClient() throws UnknownHostException {
		Settings settings = Settings.builder()//
				.put("cluster.name","es-demo")//
				.put("client.transport.sniff",true)//
				.build();
		
		InetSocketTransportAddress master = new InetSocketTransportAddress(
				InetAddress.getByName("192.168.179.131"),9300
			);
		
		TransportClient client = new PreBuiltTransportClient(settings)//
					.addTransportAddress(master);
		
		return client;
	}
}
