package com.DecanatoOrtizSerrano.OrtizSerranoTP3;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class OrtizSerranoTp3Application {

	public static void main(String[] args) {
		SpringApplication.run(OrtizSerranoTp3Application.class, args);
	}

}
