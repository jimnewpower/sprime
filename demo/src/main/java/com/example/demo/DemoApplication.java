package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@SpringBootApplication
public class DemoApplication {

	private static final Logger logger = LogManager.getLogger(DemoApplication.class);

	public static void main(String[] args) {
		logger.info("Starting DemoApplication...");
		SpringApplication.run(DemoApplication.class, args);
		logger.info("DemoApplication started successfully.");
	}

}
