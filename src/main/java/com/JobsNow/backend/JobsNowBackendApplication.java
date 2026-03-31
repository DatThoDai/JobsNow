package com.JobsNow.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class JobsNowBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(JobsNowBackendApplication.class, args);
	}

}
