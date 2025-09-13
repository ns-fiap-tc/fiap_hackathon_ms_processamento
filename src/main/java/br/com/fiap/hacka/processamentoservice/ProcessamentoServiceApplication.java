package br.com.fiap.processamentoservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class ProcessamentoServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProcessamentoServiceApplication.class, args);
	}

}