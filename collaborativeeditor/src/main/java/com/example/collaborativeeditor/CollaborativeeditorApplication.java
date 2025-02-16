package com.example.collaborativeeditor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main Spring Boot Application class for the Collaborative Editor
 * This class serves as the entry point for the application
 */
@SpringBootApplication // Indicates that this is a Spring Boot application and enables
						// auto-configuration
public class CollaborativeeditorApplication {

	/**
	 * Main method that starts the Spring Boot application
	 * 
	 * @param args Command line arguments passed to the application
	 */
	public static void main(String[] args) {
		// Bootstraps the Spring application by creating the ApplicationContext
		// and starting the embedded web server
		SpringApplication.run(CollaborativeeditorApplication.class, args);
	}

}