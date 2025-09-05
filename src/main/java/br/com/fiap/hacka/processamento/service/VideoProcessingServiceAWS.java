package br.com.fiap.hacka.processamento.service;

import br.com.fiap.hacka.processamento.dto.ProcessingResult;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class VideoProcessingServiceAWS {
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${file.temp-dir}")
    private String tempDir;

    @Value("${aws.region}")
    private String awsRegion;

    public VideoProcessingServiceAWS(S3Client s3Client, S3Presigner s3Presigner) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
    }

    public ProcessingResult processVideo(MultipartFile file) throws IOException {
        String uniqueId = UUID.randomUUID().toString();
        String originalVideoKey = "videos/" + uniqueId + "-" + file.getOriginalFilename();
        String zipKey = "zips/" + uniqueId + "-frames.zip";

        // Definindo caminhos temporários locais
        Path localTempVideoPath = Paths.get(tempDir, originalVideoKey.replace("videos/", ""));
        Path tempFramesDirPath = Paths.get(tempDir, uniqueId);
        Path localZipPath = Paths.get(tempDir, zipKey.replace("zips/", ""));

        try {
            // 1. Enviar vídeo original para o S3
            uploadFileToS3(file, originalVideoKey);

            // 2. Baixar o vídeo do S3 para processamento local
            Files.createDirectories(localTempVideoPath.getParent());
            downloadFileFromS3(originalVideoKey, localTempVideoPath);

            // 3. Extrair frames do vídeo local
            Files.createDirectories(tempFramesDirPath);
            if (!extractFramesWithJavaCV(localTempVideoPath, tempFramesDirPath)) {
                return new ProcessingResult(false, "Erro ao extrair frames com JavaCV.");
            }

            // 4. Compactar os frames em um arquivo ZIP local
            List<Path> frames = listFiles(tempFramesDirPath, ".png");
            if (frames.isEmpty()) {
                return new ProcessingResult(false, "Nenhum frame foi extraído.");
            }
            createZipFile(frames, localZipPath);

            // 5. Enviar o arquivo ZIP para o S3
            uploadFileToS3(localZipPath, zipKey);


            // 6. Preparar a resposta de sucesso com a nova URL pública
            List<String> imageNames = frames.stream().map(p -> p.getFileName().toString()).collect(Collectors.toList());
            String message = String.format("Processamento concluído! %d frames extraídos.", frames.size());
            return new ProcessingResult(true, message, getUrlDownload(zipKey), frames.size(), imageNames);

        } finally {
            // 8. Limpeza rigorosa de todos os arquivos temporários locais
            deleteIfExists(localTempVideoPath);
            deleteDirectory(tempFramesDirPath);
            deleteIfExists(localZipPath);
        }
    }

    private String getUrlDownload(String zipKey) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(zipKey)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(30)) // tempo de validade da URL
                .getObjectRequest(getObjectRequest)
                .build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    private void uploadFileToS3(MultipartFile file, String key) throws IOException {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(file.getContentType())
                .build();
        s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
    }

    private void uploadFileToS3(Path filePath, String key) throws IOException {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
        s3Client.putObject(putObjectRequest, RequestBody.fromFile(filePath));
    }

    private void downloadFileFromS3(String key, Path destinationPath) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();
        s3Client.getObject(getObjectRequest, ResponseTransformer.toFile(destinationPath));
    }

    // Métodos de extração de frames e compactação
    /**
     * Extrai frames de um vídeo usando JavaCV.
     * @param videoPath Caminho do arquivo de vídeo.
     * @param outputDir Diretório onde os frames serão salvos.
     * @return true se a extração for bem-sucedida, false caso contrário.
     */
    private boolean extractFramesWithJavaCV(Path videoPath, Path outputDir) {
        // Usar try-with-resources para garantir que o grabber seja fechado
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(videoPath.toFile())) {
            grabber.start();

            // Para extrair 1 frame por segundo, pulamos N frames, onde N é o framerate
            int framesToSkip = (int) Math.round(grabber.getFrameRate());
            System.out.printf("Vídeo com framerate: %.2f. Pulando %d frames para obter ~1fps.%n", grabber.getFrameRate(), framesToSkip);

            Java2DFrameConverter converter = new Java2DFrameConverter();
            Frame frame;
            int frameCount = 0;
            int savedFrameCount = 0;

            // Loop para percorrer todos os frames do vídeo
            while ((frame = grabber.grabImage()) != null) {
                if (frameCount % framesToSkip == 0) {
                    // Converte o frame do formato nativo para um BufferedImage do Java
                    BufferedImage bufferedImage = converter.convert(frame);
                    if (bufferedImage != null) {
                        savedFrameCount++;
                        Path outputPath = outputDir.resolve(String.format("frame_%04d.png", savedFrameCount));
                        // Salva a imagem como um arquivo PNG
                        ImageIO.write(bufferedImage, "png", outputPath.toFile());
                    }
                }
                frameCount++;
            }
            System.out.printf("Processo finalizado. %d frames salvos.%n", savedFrameCount);
            grabber.stop();
            return true;
        } catch (IOException e) {
            System.err.println("Falha ao extrair frames com JavaCV: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private void createZipFile(List<Path> files, Path zipFilePath) throws IOException {
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(zipFilePath.toFile());
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            for (Path file : files) {
                ZipEntry zipEntry = new ZipEntry(file.getFileName().toString());
                zos.putNextEntry(zipEntry);

                Files.copy(file, zos);
                zos.closeEntry();
            }
        }
    }

    // Utilitários de manipulação de arquivos
    private List<Path> listFiles(Path dir, String extension) throws IOException {
        try (Stream<Path> stream = Files.list(dir)) {
            return stream.filter(f -> f.toString().endsWith(extension)).collect(Collectors.toList());
        }
    }
    private void deleteIfExists(Path path) throws IOException {
        if (Files.exists(path)) Files.delete(path);
    }
    private void deleteDirectory(Path path) throws IOException {
        if (!Files.exists(path)) return;
        try (Stream<Path> walk = Files.walk(path)) {
            walk.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.delete(p);
                } catch (IOException e) {
                    System.err.println("Falha ao deletar arquivo temporário: " + p);
                }
            });
        }
    }
}
