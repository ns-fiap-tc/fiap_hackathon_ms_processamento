package br.com.fiap.hacka.processamentoservice.app.persistence.repository;

import br.com.fiap.hacka.core.commons.dto.FileDataDto;
import br.com.fiap.hacka.processamentoservice.app.core.domain.FileData;
import br.com.fiap.hacka.processamentoservice.app.persistence.entity.FileDataEntity;
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
class FileDataRepositoryImplTest {

    @Mock
    private FileDataJpaRepository jpaRepository;

    @InjectMocks
    private FileDataRepositoryImpl fileDataRepository;

    private FileData fileData;
    private FileDataEntity fileDataEntity;

    @BeforeEach
    void setUp() {
        Date now = new Date();
        
        fileData = new FileData();
        fileData.setId("1");
        fileData.setFileName("test.mp4");
        fileData.setUserName("testUser");
        fileData.setFileUrl("http://test.com/file");
        fileData.setCreatedAt(now);

        fileDataEntity = new FileDataEntity();
        fileDataEntity.setId("1");
        fileDataEntity.setFileName("test.mp4");
        fileDataEntity.setUserName("testUser");
        fileDataEntity.setFileUrl("http://test.com/file");
        fileDataEntity.setCreatedAt(now);
    }

    @Test
    void save_ShouldReturnFileData_WhenValidInput() {
        when(jpaRepository.save(any(FileDataEntity.class))).thenReturn(fileDataEntity);

        FileData result = fileDataRepository.save(fileData);

        assertNotNull(result);
        assertEquals(fileData.getFileName(), result.getFileName());
        assertEquals(fileData.getUserName(), result.getUserName());
        verify(jpaRepository).save(any(FileDataEntity.class));
    }

    @Test
    void findByUserName_ShouldReturnListOfFileDataDto_WhenUserExists() {
        List<FileDataEntity> entities = Arrays.asList(fileDataEntity);
        when(jpaRepository.findByUserName("testUser")).thenReturn(entities);

        List<FileDataDto> result = fileDataRepository.findByUserName("testUser");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("testUser", result.get(0).getUserName());
        verify(jpaRepository).findByUserName("testUser");
    }

    @Test
    void findByUserName_ShouldReturnEmptyList_WhenUserNotExists() {
        when(jpaRepository.findByUserName("nonExistentUser")).thenReturn(Arrays.asList());

        List<FileDataDto> result = fileDataRepository.findByUserName("nonExistentUser");

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(jpaRepository).findByUserName("nonExistentUser");
    }
}