package com.govconnect;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;
import java.sql.Connection;

@SpringBootApplication
public class GovConnectApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(GovConnectApiApplication.class, args);
	}

	@Bean
	CommandLineRunner test(DataSource dataSource) {
		return args -> {
			try (Connection connection = dataSource.getConnection()) {
				System.out.println("--------------------------------");
				System.out.println("Conectado correctamente a:");
				System.out.println(connection.getCatalog());
				System.out.println("--------------------------------");
			}
		};
	}
}

