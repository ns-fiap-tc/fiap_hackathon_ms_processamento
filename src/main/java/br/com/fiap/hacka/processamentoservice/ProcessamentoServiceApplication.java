package br.com.fiap.hacka.processamentoservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@SpringBootApplication
@EnableFeignClients
@EnableMongoAuditing
public class ProcessamentoServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProcessamentoServiceApplication.class, args);
	}

}