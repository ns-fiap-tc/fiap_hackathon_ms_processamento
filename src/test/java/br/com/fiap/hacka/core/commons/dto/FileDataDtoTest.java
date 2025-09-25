package br.com.fiap.hacka.core.commons.dto;

import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class FileDataDtoTest {

    @Test
    void constructor_ShouldCreateEmptyObject_WhenNoArgsConstructor() {
        FileDataDto dto = new FileDataDto();

        assertNotNull(dto);
        assertNull(dto.getId());
        assertNull(dto.getFileName());
        assertNull(dto.getUserName());
    }

    @Test
    void constructor_ShouldCreateObjectWithAllFields_WhenAllArgsConstructor() {
        Date now = new Date();
        FileDataDto dto = new FileDataDto("1", "testUser", "test.mp4", "frames.zip", "file.zip", now, now);

        assertEquals("1", dto.getId());
        assertEquals("testUser", dto.getUserName());
        assertEquals("test.mp4", dto.getFileName());
        assertEquals("frames.zip", dto.getFramesUrl());
        assertEquals("file.zip", dto.getFileUrl());
        assertEquals(now, dto.getCreatedAt());
        assertEquals(now, dto.getUpdatedAt());
    }

    @Test
    void settersAndGetters_ShouldWorkCorrectly() {
        FileDataDto dto = new FileDataDto();
        Date now = new Date();

        dto.setId("1");
        dto.setUserName("testUser");
        dto.setFileName("test.mp4");
        dto.setFramesUrl("frames.zip");
        dto.setFileUrl("file.zip");
        dto.setCreatedAt(now);
        dto.setUpdatedAt(now);

        assertEquals("1", dto.getId());
        assertEquals("testUser", dto.getUserName());
        assertEquals("test.mp4", dto.getFileName());
        assertEquals("frames.zip", dto.getFramesUrl());
        assertEquals("file.zip", dto.getFileUrl());
        assertEquals(now, dto.getCreatedAt());
        assertEquals(now, dto.getUpdatedAt());
    }
}