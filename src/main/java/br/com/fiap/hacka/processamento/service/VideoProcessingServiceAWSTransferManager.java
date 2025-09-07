package br.com.fiap.hacka.processamento.service;

import br.com.fiap.hacka.processamento.dto.ProcessingResult;
import jakarta.annotation.PreDestroy;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.Upload;
import software.amazon.awssdk.transfer.s3.model.UploadRequest;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class VideoProcessingServiceAWSTransferManager {
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3TransferManager transferManager;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    public VideoProcessingServiceAWSTransferManager(S3Client s3Client, S3Presigner s3Presigner, S3TransferManager transferManager) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.transferManager = transferManager;
    }

    @PreDestroy
    public void shutdownExecutor() {
        System.out.println("Desligando o ExecutorService...");
        executor.shutdown();
    }

    public ProcessingResult processVideo(MultipartFile file) throws IOException {
        String uniqueId = UUID.randomUUID().toString();
        String originalVideoKey = "videos/" + uniqueId + "-" + file.getOriginalFilename();
        String framesPrefix = "temp-frames/" + uniqueId + "/";
        String zipKey = "zips/" + uniqueId + "-frames.zip";

        List<String> frameKeys = new ArrayList<>();

        try {
            // 1. Enviar vídeo original para o S3 usando S3TransferManager (mais robusto)
            uploadMultipartFileToS3(file, originalVideoKey);

            // 2. Extrair frames, lendo o vídeo diretamente do S3 via InputStream
            //    e salvando cada frame diretamente no S3.
            frameKeys = extractAndUploadFrames(originalVideoKey, framesPrefix);

            if (frameKeys.isEmpty()) {
                return new ProcessingResult(false, "Nenhum frame foi extraído.");
            }

            // 3. Criar arquivo ZIP em memória a partir dos frames no S3
            //    e fazer upload do ZIP para o S3.
            createAndUploadZipFromS3Frames(frameKeys, zipKey, framesPrefix);

            // 4. Preparar a resposta de sucesso
            List<String> imageNames = frameKeys.stream()
                    .map(key -> key.substring(framesPrefix.length()))
                    .collect(Collectors.toList());
            String message = String.format("Processamento concluído! %d frames extraídos.", frameKeys.size());
            return new ProcessingResult(true, message, getPresignedUrl(zipKey), frameKeys.size(), imageNames);

        } finally {
            // 5. Limpeza dos arquivos temporários no S3 (vídeo original e frames)
            cleanupS3Objects(originalVideoKey, frameKeys);
        }
    }

    private void uploadMultipartFileToS3(MultipartFile file, String key) throws IOException {
        UploadRequest uploadRequest = UploadRequest.builder()
                .putObjectRequest(req -> req.bucket(bucketName).key(key).contentType(file.getContentType()))

                .requestBody(AsyncRequestBody.fromInputStream(
                        file.getInputStream(),
                        file.getSize(),
                        this.executor
                ))
                .build();

        Upload upload = transferManager.upload(uploadRequest);
        upload.completionFuture().join();
    }

    private List<String> extractAndUploadFrames(String videoKey, String outputPrefix) throws IOException {
        List<String> frameKeys = new ArrayList<>();
        GetObjectRequest getRequest = GetObjectRequest.builder().bucket(bucketName).key(videoKey).build();

        try (ResponseInputStream<GetObjectResponse> s3is = s3Client.getObject(getRequest);
             FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(s3is)) {

            grabber.start();

            int framesToSkip = (int) Math.round(grabber.getFrameRate());
            System.out.printf("Vídeo com framerate: %.2f. Pulando %d frames para obter ~1fps.%n", grabber.getFrameRate(), framesToSkip);

            Java2DFrameConverter converter = new Java2DFrameConverter();
            Frame frame;
            int frameCount = 0;
            int savedFrameCount = 0;

            while ((frame = grabber.grabImage()) != null) {
                if (frameCount % framesToSkip == 0) {
                    BufferedImage bufferedImage = converter.convert(frame);
                    if (bufferedImage != null) {
                        savedFrameCount++;
                        String frameKey = outputPrefix + String.format("frame_%04d.png", savedFrameCount);

                        // Converte imagem para byte array em memória
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        ImageIO.write(bufferedImage, "png", baos);
                        byte[] imageBytes = baos.toByteArray();

                        // Faz upload do frame para o S3
                        s3Client.putObject(
                                PutObjectRequest.builder().bucket(bucketName).key(frameKey).build(),
                                RequestBody.fromBytes(imageBytes)
                        );
                        frameKeys.add(frameKey);
                    }
                }
                frameCount++;
            }
            System.out.printf("Processo finalizado. %d frames salvos no S3.%n", savedFrameCount);
            grabber.stop();
        }
        return frameKeys;
    }

    private void createAndUploadZipFromS3Frames(List<String> frameKeys, String zipKey, String framesPrefix) throws IOException {
        // Usa um stream na memória para criar o ZIP
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {

            for (String frameKey : frameKeys) {
                // Adiciona uma nova entrada no ZIP
                String fileName = frameKey.substring(framesPrefix.length());
                zos.putNextEntry(new ZipEntry(fileName));

                // Baixa o frame do S3 como um stream
                try (InputStream frameStream = s3Client.getObject(GetObjectRequest.builder().bucket(bucketName).key(frameKey).build())) {
                    // Escreve o conteúdo do frame no ZIP
                    frameStream.transferTo(zos);
                }
                zos.closeEntry();
            }
            zos.finish(); // Finaliza a criação do ZIP

            // Faz o upload do ZIP (que está no ByteArrayOutputStream) para o S3
            byte[] zipBytes = baos.toByteArray();
            s3Client.putObject(
                    PutObjectRequest.builder().bucket(bucketName).key(zipKey).contentType("application/zip").build(),
                    RequestBody.fromInputStream(new ByteArrayInputStream(zipBytes), zipBytes.length)
            );
        }
    }

    private void cleanupS3Objects(String videoKey, List<String> frameKeys) {
        System.out.println("Iniciando limpeza de arquivos temporários no S3...");
        List<ObjectIdentifier> objectsToDelete = new ArrayList<>();
        //objectsToDelete.add(ObjectIdentifier.builder().key(videoKey).build());
        for (String key : frameKeys) {
            objectsToDelete.add(ObjectIdentifier.builder().key(key).build());
        }

        if (!objectsToDelete.isEmpty()) {
            try {
                DeleteObjectsRequest deleteRequest = DeleteObjectsRequest.builder()
                        .bucket(bucketName)
                        .delete(Delete.builder().objects(objectsToDelete).build())
                        .build();
                s3Client.deleteObjects(deleteRequest);
                System.out.println("Limpeza concluída com sucesso.");
            } catch (S3Exception e) {
                System.err.println("Erro ao deletar objetos temporários do S3: " + e.awsErrorDetails().errorMessage());
            }
        }
    }

    private String getPresignedUrl(String objectKey) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(30))
                .getObjectRequest(getObjectRequest)
                .build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }
}