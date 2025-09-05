package br.com.fiap.hacka.processamento.service;

import br.com.fiap.hacka.processamento.dto.ProcessingResult;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class VideoProcessingService {

    @Value("${file.upload-dir}")
    private String uploadDir;
    @Value("${file.output-dir}")
    private String outputDir;
    @Value("${file.temp-dir}")
    private String tempDir;

    public ProcessingResult processVideo(MultipartFile file) throws IOException {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        String filename = timestamp + "_" + originalFilename;
        Path uploadPath = Paths.get(uploadDir, filename);

        Files.createDirectories(uploadPath.getParent());
        Files.copy(file.getInputStream(), uploadPath, StandardCopyOption.REPLACE_EXISTING);

        Path tempOutputPath = Paths.get(tempDir, timestamp);
        Path zipFilePath = Paths.get(outputDir, "frames_" + timestamp + ".zip");

        try {
            Files.createDirectories(tempOutputPath);
            Files.createDirectories(zipFilePath.getParent());

            // **CHAMANDO O NOVO MÉTODO COM JAVACV**
            if (!extractFramesWithJavaCV(uploadPath, tempOutputPath)) {
                return new ProcessingResult(false, "Erro ao extrair frames com JavaCV.");
            }

            List<Path> frames;
            try (Stream<Path> stream = Files.list(tempOutputPath)) {
                frames = stream.filter(f -> f.toString().endsWith(".png")).collect(Collectors.toList());
            }

            if (frames.isEmpty()) {
                return new ProcessingResult(false, "Nenhum frame foi extraído do vídeo.");
            }

            createZipFile(frames, zipFilePath);

            List<String> imageNames = frames.stream()
                    .map(path -> path.getFileName().toString())
                    .collect(Collectors.toList());

            String message = String.format("Processamento concluído! %d frames extraídos.", frames.size());
            return new ProcessingResult(true, message, zipFilePath.getFileName().toString(), frames.size(), imageNames);

        } finally {
            Files.delete(uploadPath);
            try (Stream<Path> walk = Files.walk(tempOutputPath)) {
                walk.sorted(java.util.Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(java.io.File::delete);
            }
        }
    }

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

    // O método createZipFile permanece exatamente o mesmo
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
}
