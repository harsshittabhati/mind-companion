package com.mindcompanion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MindCompanionApplication {

	public static void main(String[] args) {
		SpringApplication.run(MindCompanionApplication.class, args);
	}

}
