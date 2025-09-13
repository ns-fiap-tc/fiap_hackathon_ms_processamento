package br.com.fiap.processamentoservice.app.queue;

import br.com.fiap.processamentoservice.core.commons.FilePart;
import br.com.fiap.processamentoservice.infra.config.RabbitMqConfig;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

@CommonsLog
@Component
public class FileUploadMessageConsumer {
    private final Map<String, BlockingQueue<FilePart>> fileQueues = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    public FileUploadMessageConsumer(S3Client s3Client, S3Presigner s3Presigner) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
    }

    @RabbitListener(queues = RabbitMqConfig.QUEUE_NAME)
    public void receive(@Payload FilePart filePart) {
        log.info("Mensagem recebida: " + filePart.getFileName());

        fileQueues.computeIfAbsent(filePart.getFileName(), fileName -> {
            BlockingQueue<FilePart> queue = new LinkedBlockingQueue<>();
            executor.submit(() -> processZipToS3(fileName, queue));
            return queue;
        }).offer(filePart);
    }

    private void processZipToS3(String fileName, BlockingQueue<FilePart> queue) {
        //The ZIP thread writes chunks into the PipedOutputStream.
        //The S3 upload reads directly from the PipedInputStream, streaming them as they arrive.
        //This means S3 receives data as the ZIP is being created, not after the full file is in memory.

        try (PipedOutputStream pipedOut = new PipedOutputStream();
            PipedInputStream pipedIn = new PipedInputStream(pipedOut, FilePart.CHUNK_SIZE); //1MB same size of each chunk.
            BufferedOutputStream zipBufferedOut = new BufferedOutputStream(pipedOut)) {

            // Start ZIP thread
            Future<?> zipFuture = executor.submit(() -> {
                try (ZipOutputStream zipOut = new ZipOutputStream(zipBufferedOut)) {
                    ZipEntry entry = new ZipEntry(fileName);
                    zipOut.putNextEntry(entry);

                    while (true) {
                        FilePart part = queue.take();
                        if (part.getBytesRead() == -1) {
                            break;
                        }
                        zipOut.write(part.getBytes(), 0, part.getBytesRead());
                    }
                    zipOut.closeEntry();
                    zipOut.finish();
                    log.info("ZIP stream completed for file " + fileName);
                } catch (IOException | InterruptedException e) {
                    throw new RuntimeException("Failed to create ZIP stream", e);
                }
            });

            // Upload to S3 directly from piped input
            //PutObjectRequest putRequest = PutObjectRequest.builder().bucket(bucketName).key(fileName + ".zip").build();

            //s3Client.putObject(putRequest, RequestBody.fromInputStream(pipedIn, Long.MAX_VALUE));
            s3Client.putObject(
                    PutObjectRequest.builder().bucket(bucketName).key(fileName+".zip").build(),
                    RequestBody.fromBytes(pipedIn.readAllBytes())
            );

            log.info(getPresignedUrl(fileName + ".zip"));

            zipFuture.get(); // ensure ZIP thread completed
            log.info("File uploaded to S3: " + fileName + ".zip");
        } catch (Exception e) {
            log.error("Failed to process and upload file " + fileName, e);
        } finally {
            fileQueues.remove(fileName);
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

    private void processRaw(String fileName, BlockingQueue<FilePart> queue) {
        File outputFile = new File("/tmp/files/" + fileName + ".zip");

        try (FileOutputStream fos = new FileOutputStream(outputFile);
             BufferedOutputStream bos = new BufferedOutputStream(fos)) {

            while (true) {
                FilePart part = queue.take();
                if (part.getBytesRead() == -1) {
                    // End of file marker → stop writing
                    break;
                }

                bos.write(part.getBytes(), 0, part.getBytesRead());
            }

            bos.flush();
            log.info("File zipped successfully: " + fileName + " " + outputFile.getAbsolutePath());
        } catch (Exception e) {
            log.error("Failed to process file " + fileName, e);
        } finally {
            fileQueues.remove(fileName);
        }
    }

    private void processZip(String fileName, BlockingQueue<FilePart> queue) {
        File outputFile = new File("/tmp/files/" + fileName + ".zip");

        try (FileOutputStream fos = new FileOutputStream(outputFile);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             ZipOutputStream zipOut = new ZipOutputStream(bos)) {

            ZipEntry entry = new ZipEntry(fileName);
            zipOut.putNextEntry(entry);

            while (true) {
                FilePart part = queue.take();

                if (part.getBytesRead() == -1) {
                    // End of file reached → stop writing
                    break;
                }

                zipOut.write(part.getBytes(), 0, part.getBytesRead());
            }

            zipOut.closeEntry();
            zipOut.finish(); // <-- ensures central directory is written

            log.info("File zipped successfully: " + fileName + " " + outputFile.getAbsolutePath());

        } catch (Exception e) {
            log.error("Failed to process file " + fileName, e);
        } finally {
            fileQueues.remove(fileName);
        }
    }
}