package br.com.fiap.hacka.processamentoservice.app.service;

import br.com.fiap.hacka.core.commons.dto.FileDataDto;
import br.com.fiap.hacka.core.commons.dto.FilePartDto;
import br.com.fiap.hacka.processamentoservice.app.core.domain.FileData;
import br.com.fiap.hacka.processamentoservice.app.persistence.repository.FileDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileDataServiceTest {

    @Mock
    private FileDataRepository fileDataRepository;

    @InjectMocks
    private FileDataService fileDataService;

    private FilePartDto filePartDto;
    private FileData fileData;
    private FileDataDto fileDataDto;

    @BeforeEach
    void setUp() {
        filePartDto = new FilePartDto();
        filePartDto.setFileName("test.mp4");
        filePartDto.setUserName("testUser");
        filePartDto.setFileUrl("http://test.com/file");

        fileData = new FileData();
        fileData.setId("1");
        fileData.setFileName("test.mp4");
        fileData.setUserName("testUser");
        fileData.setFileUrl("http://test.com/file");
        fileData.setCreatedAt(new Date());

        fileDataDto = new FileDataDto();
        fileDataDto.setId("1");
        fileDataDto.setFileName("test.mp4");
        fileDataDto.setUserName("testUser");
        fileDataDto.setFileUrl("http://test.com/file");
        fileDataDto.setCreatedAt(new Date());
    }

    @Test
    void save_ShouldReturnFileDataDto_WhenValidFilePartDto() {
        when(fileDataRepository.save(any(FileData.class))).thenReturn(fileData);

        FileDataDto result = fileDataService.save(filePartDto);

        assertNotNull(result);
        assertEquals(fileData.getFileName(), result.getFileName());
        assertEquals(fileData.getUserName(), result.getUserName());
        verify(fileDataRepository).save(any(FileData.class));
    }

    @Test
    void findByUserName_ShouldReturnListOfFileDataDto_WhenUserExists() {
        List<FileDataDto> expectedList = Arrays.asList(fileDataDto);
        when(fileDataRepository.findByUserName("testUser")).thenReturn(expectedList);

        List<FileDataDto> result = fileDataService.findByUserName("testUser");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("testUser", result.get(0).getUserName());
        verify(fileDataRepository).findByUserName("testUser");
    }

    @Test
    void findByUserName_ShouldReturnEmptyList_WhenUserNotExists() {
        when(fileDataRepository.findByUserName("nonExistentUser")).thenReturn(Arrays.asList());

        List<FileDataDto> result = fileDataService.findByUserName("nonExistentUser");

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(fileDataRepository).findByUserName("nonExistentUser");
    }
}