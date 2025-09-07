package br.com.fiap.hacka.processamento.controller;

import br.com.fiap.hacka.processamento.dto.ProcessingResult;
import br.com.fiap.hacka.processamento.service.VideoProcessingService;
import br.com.fiap.hacka.processamento.service.VideoProcessingServiceAWS;
import br.com.fiap.hacka.processamento.service.VideoProcessingServiceAWSTransferManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
public class VideoController {

    //private final VideoProcessingServiceAWS videoProcessingService;
    private final VideoProcessingServiceAWSTransferManager videoProcessingService;

    public VideoController(VideoProcessingServiceAWSTransferManager videoProcessingService) {
        this.videoProcessingService = videoProcessingService;
    }

    @PostMapping("/upload")
    public ResponseEntity<ProcessingResult> handleVideoUpload(@RequestParam("video") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new ProcessingResult(false, "Por favor, selecione um arquivo para upload."));
        }

        try {
            ProcessingResult result = videoProcessingService.processVideo(file);
            if (result.isSuccess()) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ProcessingResult(false, "Erro no servidor ao processar o vídeo: " + e.getMessage()));
        }
    }
}
