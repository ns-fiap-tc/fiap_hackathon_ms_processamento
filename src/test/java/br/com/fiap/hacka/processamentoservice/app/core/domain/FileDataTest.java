package br.com.fiap.hacka.processamentoservice.app.core.domain;

import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class FileDataTest {

    @Test
    void constructor_ShouldCreateEmptyObject_WhenNoArgsConstructor() {
        FileData fileData = new FileData();

        assertNotNull(fileData);
        assertNull(fileData.getId());
        assertNull(fileData.getFileName());
        assertNull(fileData.getUserName());
    }

    @Test
    void constructor_ShouldCreateObjectWithAllFields_WhenAllArgsConstructor() {
        Date now = new Date();
        FileData fileData = new FileData("1", "testUser", "test.mp4", "frames.zip", "file.zip", now, now);

        assertEquals("1", fileData.getId());
        assertEquals("testUser", fileData.getUserName());
        assertEquals("test.mp4", fileData.getFileName());
        assertEquals("frames.zip", fileData.getFramesUrl());
        assertEquals("file.zip", fileData.getFileUrl());
        assertEquals(now, fileData.getCreatedAt());
        assertEquals(now, fileData.getUpdatedAt());
    }

    @Test
    void settersAndGetters_ShouldWorkCorrectly() {
        FileData fileData = new FileData();
        Date now = new Date();

        fileData.setId("1");
        fileData.setUserName("testUser");
        fileData.setFileName("test.mp4");
        fileData.setFramesUrl("frames.zip");
        fileData.setFileUrl("file.zip");
        fileData.setCreatedAt(now);
        fileData.setUpdatedAt(now);

        assertEquals("1", fileData.getId());
        assertEquals("testUser", fileData.getUserName());
        assertEquals("test.mp4", fileData.getFileName());
        assertEquals("frames.zip", fileData.getFramesUrl());
        assertEquals("file.zip", fileData.getFileUrl());
        assertEquals(now, fileData.getCreatedAt());
        assertEquals(now, fileData.getUpdatedAt());
    }
}