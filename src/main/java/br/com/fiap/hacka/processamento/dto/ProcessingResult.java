package br.com.fiap.hacka.processamento.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

// Ignora campos nulos na serialização JSON
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProcessingResult {
    private boolean success;
    private String message;
    private String zipUrl; // Campo alterado
    private Integer frameCount;
    private List<String> images;

    // Construtor para sucesso
    public ProcessingResult(boolean success, String message, String zipUrl, int frameCount, List<String> images) {
        this.success = success;
        this.message = message;
        this.zipUrl = zipUrl;
        this.frameCount = frameCount;
        this.images = images;
    }

    public ProcessingResult(boolean success, String message) {
    }

    // ... construtor de falha e getters/setters ...
    public String getZipUrl() { return zipUrl; }
    public void setZipUrl(String zipUrl) { this.zipUrl = zipUrl; }
    public boolean isSuccess() { return success; }
}
