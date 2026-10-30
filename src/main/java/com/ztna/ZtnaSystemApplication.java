package com.ztna;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ZtnaSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(ZtnaSystemApplication.class, args);
	}

}
