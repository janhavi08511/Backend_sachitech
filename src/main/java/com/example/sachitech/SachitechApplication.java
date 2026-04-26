package com.example.sachitech;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching
@EnableScheduling
@EnableJpaRepositories(basePackages = "com.example.sachitech.repository")
@EntityScan(basePackages = "com.example.sachitech.entity")
public class SachitechApplication {

	public static void main(String[] args) {
		SpringApplication.run(SachitechApplication.class, args);
	}

}
