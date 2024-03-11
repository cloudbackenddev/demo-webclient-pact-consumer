package com.example.pact.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(value = {AppEnvConfig.class})
public class DemoWebclientPactConsumerApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoWebclientPactConsumerApplication.class, args);
	}

}
