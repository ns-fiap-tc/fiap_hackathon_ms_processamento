package br.com.fiap.hacka.processamentoservice.app.queue.impl;

import br.com.fiap.hacka.core.commons.dto.FilePartDto;
import br.com.fiap.hacka.core.commons.dto.NotificacaoDto;
import br.com.fiap.hacka.processamentoservice.app.queue.MessageConsumer;
import br.com.fiap.hacka.processamentoservice.app.rest.client.NotificacaoServiceClient;
import br.com.fiap.hacka.processamentoservice.app.service.FileDataService;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

@Slf4j
@RequiredArgsConstructor
@Component
public class UploadMessageConsumer implements MessageConsumer {
    private final Map<String, BlockingQueue<FilePartDto>> fileQueues = new ConcurrentHashMap<>();
    private static final Set<String> failedFiles = ConcurrentHashMap.newKeySet();
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final FileDataService fileDataService;
    private final NotificacaoServiceClient notificacaoServiceClient;

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @RabbitListener(queues = "${rabbitmq.queue.consumer.messageQueue}")
    public void receive(@Payload FilePartDto filePart) {
        String fileName = filePart.getFileName();
        String userName = filePart.getUserName();

        if (filePart.isFirstChunk()) {
            failedFiles.remove(fileName + "|" + userName);
        } else if (failedFiles.contains(fileName + "|" + userName)) {
            // skip files that already failed
            log.warn("Skipping chunk for failed file [{}], for user [{}]", fileName, userName);
            return;
        }

        log.info("Mensagem recebida: {}", fileName);

        fileQueues.computeIfAbsent(fileName, name -> {
            BlockingQueue<FilePartDto> queue = new LinkedBlockingQueue<>();

            // wrap processZip in a Runnable with try/catch
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        processZipToS3(fileName, userName, queue);
                    } catch (Exception e) {
                        log.error("Error processing zip for file [{}], for user [{}]: {}", fileName, userName, e.getMessage(), e);

                        // mark file as failed
                        failedFiles.add(fileName + "|" + userName);

                        // stop further processing by removing the queue
                        fileQueues.remove(fileName);
                    }
                }
            });

            return queue;
        }).offer(filePart);
    }

    /*
    public void receive(@Payload FilePartDto filePart) {
        log.info("Mensagem recebida: " + filePart.getFileName());

        fileQueues.computeIfAbsent(filePart.getFileName(), fileName -> {
            BlockingQueue<FilePartDto> queue = new LinkedBlockingQueue<>();
            //executor.submit(() -> processZipToS3(fileName, queue));
            executor.submit(() -> processZip(fileName, queue));
            return queue;
        }).offer(filePart);
    }
    */

    private void processZipToS3(String fileName, String userName, BlockingQueue<FilePartDto> queue) {
        //The ZIP thread writes chunks into the PipedOutputStream.
        //The S3 upload reads directly from the PipedInputStream, streaming them as they arrive.
        //This means S3 receives data as the ZIP is being created, not after the full file is in memory.
        final AtomicBoolean isOk = new AtomicBoolean(true);
        String s3Key = "files/"+ userName + "/" + fileName + ".zip";
        AtomicReference<String> url = new AtomicReference<>();
        try (PipedOutputStream pipedOut = new PipedOutputStream();
             PipedInputStream pipedIn = new PipedInputStream(pipedOut, FilePartDto.CHUNK_SIZE); //1MB same size of each chunk.
             BufferedOutputStream zipBufferedOut = new BufferedOutputStream(pipedOut)) {

            // Start ZIP thread
            Future<?> zipFuture = executor.submit(() -> {
                try (ZipOutputStream zipOut = new ZipOutputStream(zipBufferedOut)) {
                    ZipEntry entry = new ZipEntry(fileName);
                    zipOut.putNextEntry(entry);

                    while (true) {
                        FilePartDto part = queue.take();

                        if (part.getBytesRead() == -1) {
                            //grava o ultimo arquivo do processo, garantindo que o arquivo foi tratado em sua totalidade.
                            part.setFileUrl(getPresignedUrl(fileName + ".zip"));
                            fileDataService.save(part);
                            url.set(part.getWebhookUrl());
                            break;
                        } else if (part.getBytesRead() == -2) {
                            //houve uma falha no processo e o arquivo deve ser ignorado.
                            isOk.set(false);
                            url.set(part.getWebhookUrl());
                            break;
                        }
                        zipOut.write(part.getBytes(), 0, part.getBytesRead());
                    }
                    log.info("ZIP stream completed for file " + fileName);
                    // Only finish the zip if everything is ok
                    if (isOk.get()) {
                        zipOut.closeEntry();
                        zipOut.finish(); // <-- ensures central directory is written
                        log.info("File zipped successfully: " + fileName);
                    }

                } catch (IOException | InterruptedException e) {
                    isOk.set(false);
                    throw new RuntimeException("Failed to create ZIP stream", e);
                }
            });

            // Upload to S3 directly from piped input
            //PutObjectRequest putRequest = PutObjectRequest.builder().bucket(bucketName).key(fileName + ".zip").build();

            //s3Client.putObject(putRequest, RequestBody.fromInputStream(pipedIn, Long.MAX_VALUE));
            s3Client.putObject(
                    PutObjectRequest.builder().bucket(bucketName).key(s3Key).build(),
                    RequestBody.fromBytes(pipedIn.readAllBytes())
            );

            log.info(getPresignedUrl(s3Key));

            zipFuture.get(); // ensure ZIP thread completed
            log.info("File uploaded to S3: " + s3Key);
        } catch (Exception e) {
            isOk.set(false);
            log.error("Failed to process and upload file " + fileName, e);
        } finally {
            fileQueues.remove(fileName);

            if (isOk.get()) {
                notificacaoServiceClient.sendWebhook(
                        new NotificacaoDto(
                                "Arquivo " + fileName + " armazenado com sucesso.",
                                url.get()));
            } else if (!isOk.get()) {
                // Delete from S3 if uploaded (defensive cleanup)
                // mark the file as failed
                failedFiles.add(fileName + "|" + userName);

                notificacaoServiceClient.sendWebhook(
                        new NotificacaoDto(
                                "Ocorreu um erro durante o processamento do arquivo " + fileName + ".",
                                url.get()));
                try {
                    s3Client.deleteObject(DeleteObjectRequest.builder()
                            .bucket(bucketName)
                            .key(s3Key)
                            .build());
                    log.info("Partial S3 file deleted: s3://{}/{}", bucketName, s3Key);
                } catch (Exception ex) {
                    log.warn("Failed to delete partial file from S3: s3://{}/{}", bucketName, s3Key, ex);
                }
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

/*    private void processRaw(String fileName, BlockingQueue<FilePartDto> queue) {
        File outputFile = new File("/tmp/files/" + fileName + ".zip");
        new File("/tmp/files").mkdir();

        try (FileOutputStream fos = new FileOutputStream(outputFile);
             BufferedOutputStream bos = new BufferedOutputStream(fos)) {

            while (true) {
                FilePartDto part = queue.take();
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

    private void processZip(String fileName, String userName, BlockingQueue<FilePartDto> queue) {
        File outputFile = new File("/tmp/files/" + fileName + ".zip");
        boolean isOk = true;
        String url = null;
        try (FileOutputStream fos = new FileOutputStream(outputFile);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             ZipOutputStream zipOut = new ZipOutputStream(bos)) {

            ZipEntry entry = new ZipEntry(fileName);
            zipOut.putNextEntry(entry);

            while (true) {
                FilePartDto part = queue.take();

                if (part.getBytesRead() == -1) {
                    // End of file reached → stop writing
                    part.setFileUrl(outputFile.getAbsolutePath());
                    fileDataService.save(part);
                    url = part.getWebhookUrl();
                    break;
                } else if (part.getBytesRead() == -2) {
                    //houve uma falha no processo e o arquivo deve ser ignorado.
                    isOk = false;
                    url = part.getWebhookUrl();
                    break;
                }

                zipOut.write(part.getBytes(), 0, part.getBytesRead());
            }

            // Only finish the zip if everything is ok
            if (isOk) {
                zipOut.closeEntry();
                zipOut.finish(); // <-- ensures central directory is written
                log.info("File zipped successfully: " + fileName + " " + outputFile.getAbsolutePath());
            }
        } catch (Exception e) {
            isOk = false; // also mark as failed
            log.error("Failed to process file " + fileName, e);
        } finally {
            fileQueues.remove(fileName);

            if (isOk) {
                notificacaoServiceClient.sendWebhook(new NotificacaoDto("Arquivo " + fileName + " armazenado com sucesso.", url));
            } else if (!isOk && outputFile.exists()) {
                // mark the file as failed
                failedFiles.add(fileName + "|" + userName);

                notificacaoServiceClient.sendWebhook(new NotificacaoDto("Ocorreu um erro durante o processamento do arquivo " + fileName + ".", url));
                boolean deleted = outputFile.delete();
                if (!deleted) {
                    log.warn("Could not delete partial file: " + outputFile.getAbsolutePath());
                } else {
                    log.info("Partial file deleted: " + outputFile.getAbsolutePath());
                }
            }
        }
    }*/
}