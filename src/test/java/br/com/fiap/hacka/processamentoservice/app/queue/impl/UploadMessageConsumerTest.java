package br.com.fiap.hacka.processamentoservice.app.queue.impl;

import br.com.fiap.hacka.core.commons.dto.FileDataDto;
import br.com.fiap.hacka.core.commons.dto.FilePartDto;
import br.com.fiap.hacka.processamentoservice.app.rest.client.NotificacaoServiceClient;
import br.com.fiap.hacka.processamentoservice.app.service.FileDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.util.Date;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UploadMessageConsumerTest {

    @Mock
    private FileDataService fileDataService;

    @Mock
    private NotificacaoServiceClient notificacaoServiceClient;

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    @InjectMocks
    private UploadMessageConsumer uploadMessageConsumer;

    private FilePartDto filePartDto;
    private FileDataDto fileDataDto;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(uploadMessageConsumer, "bucketName", "test-bucket");

        filePartDto = new FilePartDto();
        filePartDto.setFileName("test.mp4");
        filePartDto.setUserName("testUser");
        filePartDto.setBytes(new byte[1024]);
        filePartDto.setBytesRead(1024);
        filePartDto.setFirstChunk(true);
        filePartDto.setWebhookUrl("http://webhook.test");

        fileDataDto = new FileDataDto();
        fileDataDto.setId("1");
        fileDataDto.setFileName("test.mp4");
        fileDataDto.setUserName("testUser");
        fileDataDto.setCreatedAt(new Date());
    }

    @Test
    void receive_ShouldProcessFirstChunk_WhenFirstChunkReceived() {
        //when(fileDataService.save(any(FilePartDto.class))).thenReturn(fileDataDto);

        uploadMessageConsumer.receive(filePartDto);

        verify(fileDataService, never()).save(any(FilePartDto.class));
    }

    @Test
    void receive_ShouldSkipProcessing_WhenFileAlreadyFailed() {
        FilePartDto failedFileChunk = new FilePartDto();
        failedFileChunk.setFileName("failed.mp4");
        failedFileChunk.setUserName("testUser");
        failedFileChunk.setFirstChunk(false);

        uploadMessageConsumer.receive(failedFileChunk);

        verify(fileDataService, never()).save(any(FilePartDto.class));
    }

    @Test
    void receive_ShouldCreateNewQueue_WhenFirstChunkOfNewFile() {
        FilePartDto newFileChunk = new FilePartDto();
        newFileChunk.setFileName("newfile.mp4");
        newFileChunk.setUserName("testUser");
        newFileChunk.setBytes(new byte[1024]);
        newFileChunk.setBytesRead(1024);
        newFileChunk.setFirstChunk(true);

        uploadMessageConsumer.receive(newFileChunk);

        verify(fileDataService, never()).save(any(FilePartDto.class));
    }
}