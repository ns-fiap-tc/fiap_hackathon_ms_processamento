package br.com.fiap.hacka.processamentoservice.app.queue.impl;

import br.com.fiap.hacka.core.commons.dto.FileDataDto;
import br.com.fiap.hacka.core.commons.dto.FilePartDto;
import br.com.fiap.hacka.core.commons.dto.NotificacaoDto;
import br.com.fiap.hacka.processamentoservice.app.rest.client.NotificacaoServiceClient;
import br.com.fiap.hacka.processamentoservice.app.service.FileDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.lang.reflect.Field;
import java.net.URL;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UploadMessageConsumerAdvancedTest {

    @Mock
    private FileDataService fileDataService;

    @Mock
    private NotificacaoServiceClient notificacaoServiceClient;

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    @Mock
    private PresignedGetObjectRequest presignedRequest;

    @InjectMocks
    private UploadMessageConsumer uploadMessageConsumer;

    @BeforeEach
    void setUp() throws Exception {
        ReflectionTestUtils.setField(uploadMessageConsumer, "bucketName", "test-bucket");
        clearFailedFiles();
        clearFileQueues();
        
        when(presignedRequest.url()).thenReturn(new URL("http://test-url.com"));
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presignedRequest);
        when(fileDataService.save(any(FilePartDto.class))).thenReturn(new FileDataDto());
    }

    @Test
    void processZipToS3_ShouldHandleEndOfFile_WhenBytesReadMinusOne() throws Exception {
        BlockingQueue<FilePartDto> queue = new LinkedBlockingQueue<>();
        
        FilePartDto endMarker = new FilePartDto();
        endMarker.setBytesRead(-1);
        endMarker.setWebhookUrl("http://webhook.test");
        queue.offer(endMarker);

        invokeProcessZipToS3("test.mp4", "testUser", queue);

        verify(fileDataService).save(any(FilePartDto.class));
        verify(notificacaoServiceClient).sendWebhook(any(NotificacaoDto.class));
    }

/*
    @Test
    void processZipToS3_ShouldHandleFailure_WhenBytesReadMinusTwo() throws Exception {
        BlockingQueue<FilePartDto> queue = new LinkedBlockingQueue<>();
        
        FilePartDto failureMarker = new FilePartDto();
        failureMarker.setBytesRead(-2);
        failureMarker.setWebhookUrl("http://webhook.test");
        queue.offer(failureMarker);

        invokeProcessZipToS3("test.mp4", "testUser", queue);

        verify(notificacaoServiceClient).sendWebhook(argThat(dto -> 
            dto.getMensagem().contains("erro durante o processamento")));
        verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
    }
*/

    @Test
    void processZipToS3_ShouldProcessNormalChunk_WhenValidBytes() throws Exception {
        BlockingQueue<FilePartDto> queue = new LinkedBlockingQueue<>();
        
        FilePartDto chunk = new FilePartDto();
        chunk.setBytesRead(1024);
        chunk.setBytes(new byte[1024]);
        queue.offer(chunk);
        
        FilePartDto endMarker = new FilePartDto();
        endMarker.setBytesRead(-1);
        endMarker.setWebhookUrl("http://webhook.test");
        queue.offer(endMarker);

        invokeProcessZipToS3("test.mp4", "testUser", queue);

        verify(s3Client).putObject(any(PutObjectRequest.class), ArgumentMatchers.<RequestBody>any());
        verify(notificacaoServiceClient).sendWebhook(argThat(dto -> 
            dto.getMensagem().contains("armazenado com sucesso")));
    }

    @Test
    void processZipToS3_ShouldHandleException_WhenS3Fails() throws Exception {
        BlockingQueue<FilePartDto> queue = new LinkedBlockingQueue<>();
        
        FilePartDto endMarker = new FilePartDto();
        endMarker.setBytesRead(-1);
        endMarker.setWebhookUrl("http://webhook.test");
        queue.offer(endMarker);

        doThrow(new RuntimeException("S3 Error")).when(s3Client).putObject(any(PutObjectRequest.class), ArgumentMatchers.<RequestBody>any());

        invokeProcessZipToS3("test.mp4", "testUser", queue);

        assertTrue(getFailedFiles().contains("test.mp4|testUser"));
        verify(notificacaoServiceClient).sendWebhook(argThat(dto -> 
            dto.getMensagem().contains("erro durante o processamento")));
    }

/*    @Test
    void processRaw_ShouldProcessFile_WhenValidChunks() throws Exception {
        BlockingQueue<FilePartDto> queue = new LinkedBlockingQueue<>();
        
        FilePartDto chunk = new FilePartDto();
        chunk.setBytesRead(1024);
        chunk.setBytes(new byte[1024]);
        queue.offer(chunk);
        
        FilePartDto endMarker = new FilePartDto();
        endMarker.setBytesRead(-1);
        queue.offer(endMarker);

        invokeProcessRaw("test.mp4", queue);

        assertFalse(getFileQueues().containsKey("test.mp4"));
    }*/

/*    @Test
    void processZip_ShouldHandleSuccess_WhenValidFile() throws Exception {
        BlockingQueue<FilePartDto> queue = new LinkedBlockingQueue<>();
        
        FilePartDto chunk = new FilePartDto();
        chunk.setBytesRead(1024);
        chunk.setBytes(new byte[1024]);
        queue.offer(chunk);
        
        FilePartDto endMarker = new FilePartDto();
        endMarker.setBytesRead(-1);
        endMarker.setWebhookUrl("http://webhook.test");
        queue.offer(endMarker);

        invokeProcessZip("test.mp4", "testUser", queue);

        verify(fileDataService).save(any(FilePartDto.class));
        verify(notificacaoServiceClient).sendWebhook(argThat(dto -> 
            dto.getMensagem().contains("armazenado com sucesso")));
    }

    @Test
    void processZip_ShouldHandleFailure_WhenBytesReadMinusTwo() throws Exception {
        BlockingQueue<FilePartDto> queue = new LinkedBlockingQueue<>();
        
        FilePartDto failureMarker = new FilePartDto();
        failureMarker.setBytesRead(-2);
        failureMarker.setWebhookUrl("http://webhook.test");
        queue.offer(failureMarker);

        invokeProcessZip("test.mp4", "testUser", queue);

        assertTrue(getFailedFiles().contains("test.mp4|testUser"));
        verify(notificacaoServiceClient).sendWebhook(argThat(dto -> 
            dto.getMensagem().contains("erro durante o processamento")));
    }*/

    private void invokeProcessZipToS3(String fileName, String userName, BlockingQueue<FilePartDto> queue) throws Exception {
        ReflectionTestUtils.invokeMethod(uploadMessageConsumer, "processZipToS3", fileName, userName, queue);
    }

    private void invokeProcessRaw(String fileName, BlockingQueue<FilePartDto> queue) throws Exception {
        ReflectionTestUtils.invokeMethod(uploadMessageConsumer, "processRaw", fileName, queue);
    }

    private void invokeProcessZip(String fileName, String userName, BlockingQueue<FilePartDto> queue) throws Exception {
        ReflectionTestUtils.invokeMethod(uploadMessageConsumer, "processZip", fileName, userName, queue);
    }

    private void clearFailedFiles() throws Exception {
        Field failedFilesField = UploadMessageConsumer.class.getDeclaredField("failedFiles");
        failedFilesField.setAccessible(true);
        Set<String> failedFiles = (Set<String>) failedFilesField.get(null);
        failedFiles.clear();
    }

    private Set<String> getFailedFiles() throws Exception {
        Field failedFilesField = UploadMessageConsumer.class.getDeclaredField("failedFiles");
        failedFilesField.setAccessible(true);
        return (Set<String>) failedFilesField.get(null);
    }

    private void clearFileQueues() throws Exception {
        Field fileQueuesField = UploadMessageConsumer.class.getDeclaredField("fileQueues");
        fileQueuesField.setAccessible(true);
        Map<String, BlockingQueue<FilePartDto>> fileQueues = (Map<String, BlockingQueue<FilePartDto>>) fileQueuesField.get(uploadMessageConsumer);
        fileQueues.clear();
    }

    private Map<String, BlockingQueue<FilePartDto>> getFileQueues() throws Exception {
        Field fileQueuesField = UploadMessageConsumer.class.getDeclaredField("fileQueues");
        fileQueuesField.setAccessible(true);
        return (Map<String, BlockingQueue<FilePartDto>>) fileQueuesField.get(uploadMessageConsumer);
    }
}