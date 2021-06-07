package com.codefleck.assignment;

import java.io.File;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import controller.FileUploadController;

@Configuration
@EnableAutoConfiguration
@ComponentScan({"demo","controller"})
public class FileUploadApplication {

	public static void main(String[] args) {
		new File(FileUploadController.UPLOAD_DIRECTORY).mkdir();
		SpringApplication.run(FileUploadApplication.class, args);
	}
}
