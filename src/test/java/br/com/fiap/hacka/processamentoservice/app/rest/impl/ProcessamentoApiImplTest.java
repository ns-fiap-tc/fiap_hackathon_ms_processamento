package br.com.fiap.hacka.processamentoservice.app.rest.impl;

import br.com.fiap.hacka.core.commons.dto.FileDataDto;
import br.com.fiap.hacka.processamentoservice.app.service.FileDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessamentoApiImplTest {

    @Mock
    private FileDataService fileDataService;

    @InjectMocks
    private ProcessamentoApiImpl processamentoApi;

    private FileDataDto fileDataDto;

    @BeforeEach
    void setUp() {
        fileDataDto = new FileDataDto();
        fileDataDto.setId("1");
        fileDataDto.setFileName("test.mp4");
        fileDataDto.setUserName("testUser");
        fileDataDto.setFileUrl("http://test.com/file");
        fileDataDto.setCreatedAt(new Date());
    }

    @Test
    void findByUserName_ShouldReturnOkWithData_WhenFilesExist() {
        List<FileDataDto> files = Arrays.asList(fileDataDto);
        when(fileDataService.findByUserName("testUser")).thenReturn(files);

        ResponseEntity<?> response = processamentoApi.findByUserName("testUser");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(files, response.getBody());
        verify(fileDataService).findByUserName("testUser");
    }

    @Test
    void findByUserName_ShouldReturnNoContent_WhenNoFilesExist() {
        when(fileDataService.findByUserName("testUser")).thenReturn(Arrays.asList());

        ResponseEntity<?> response = processamentoApi.findByUserName("testUser");

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
        verify(fileDataService).findByUserName("testUser");
    }

    @Test
    void findByUserName_ShouldReturnNoContent_WhenNullReturned() {
        when(fileDataService.findByUserName("testUser")).thenReturn(null);

        ResponseEntity<?> response = processamentoApi.findByUserName("testUser");

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
        verify(fileDataService).findByUserName("testUser");
    }
}