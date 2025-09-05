package br.com.fiap.hacka;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@SpringBootApplication
public class ProcessamentoApplication {

    @Value("${file.upload-dir}")
    private String uploadDir;
    @Value("${file.output-dir}")
    private String outputDir;
    @Value("${file.temp-dir}")
    private String tempDir;

    public static void main(String[] args) {
        SpringApplication.run(ProcessamentoApplication.class, args);
        System.out.println("🎬 Servidor iniciado na porta 8080");
        System.out.println("🚀 Endpoint de upload: POST http://localhost:8080/upload");
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(uploadDir));
            Files.createDirectories(Paths.get(outputDir));
            Files.createDirectories(Paths.get(tempDir));
        } catch (IOException e) {
            throw new RuntimeException("Não foi possível criar os diretórios de armazenamento", e);
        }
    }
}
