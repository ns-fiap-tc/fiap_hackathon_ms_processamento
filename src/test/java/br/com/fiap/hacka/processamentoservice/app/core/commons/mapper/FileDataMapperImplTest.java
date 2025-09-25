package br.com.fiap.hacka.processamentoservice.app.core.commons.mapper;

import br.com.fiap.hacka.core.commons.dto.FileDataDto;
import br.com.fiap.hacka.core.commons.dto.FilePartDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FileDataMapperImplTest {

    private FileDataMapperImpl mapper;
    private FilePartDto filePartDto;

    @BeforeEach
    void setUp() {
        mapper = new FileDataMapperImpl();
        
        filePartDto = new FilePartDto();
        filePartDto.setFileName("test.mp4");
        filePartDto.setUserName("testUser");
        filePartDto.setFrameFilePath("frames.zip");
        filePartDto.setFileUrl("file.zip");
    }

    @Test
    void toDto_ShouldReturnNull_WhenFilePartDtoIsNull() {
        FileDataDto result = mapper.toDto((FilePartDto) null);
        
        assertNull(result);
    }

    @Test
    void toDto_ShouldMapCorrectly_WhenFilePartDtoIsValid() {
        FileDataDto result = mapper.toDto(filePartDto);
        
        assertNotNull(result);
        assertEquals("test.mp4", result.getFileName());
        assertEquals("testUser", result.getUserName());
        assertEquals("frames.zip", result.getFramesUrl());
        assertEquals("file.zip", result.getFileUrl());
        assertNull(result.getId());
        assertNull(result.getCreatedAt());
        assertNull(result.getUpdatedAt());
    }
}