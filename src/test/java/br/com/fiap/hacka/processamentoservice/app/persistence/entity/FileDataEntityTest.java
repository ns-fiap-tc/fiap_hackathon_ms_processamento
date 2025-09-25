package br.com.fiap.hacka.processamentoservice.app.persistence.entity;

import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class FileDataEntityTest {

    @Test
    void constructor_ShouldCreateEmptyObject_WhenNoArgsConstructor() {
        FileDataEntity entity = new FileDataEntity();

        assertNotNull(entity);
        assertNull(entity.getId());
        assertNull(entity.getFileName());
        assertNull(entity.getUserName());
    }

    @Test
    void constructor_ShouldCreateObjectWithAllFields_WhenAllArgsConstructor() {
        Date now = new Date();
        FileDataEntity entity = new FileDataEntity("1", "testUser", "test.mp4", "frames.zip", "file.zip", now, now);

        assertEquals("1", entity.getId());
        assertEquals("testUser", entity.getUserName());
        assertEquals("test.mp4", entity.getFileName());
        assertEquals("frames.zip", entity.getFramesUrl());
        assertEquals("file.zip", entity.getFileUrl());
        assertEquals(now, entity.getCreatedAt());
        assertEquals(now, entity.getUpdatedAt());
    }

    @Test
    void settersAndGetters_ShouldWorkCorrectly() {
        FileDataEntity entity = new FileDataEntity();
        Date now = new Date();

        entity.setId("1");
        entity.setUserName("testUser");
        entity.setFileName("test.mp4");
        entity.setFramesUrl("frames.zip");
        entity.setFileUrl("file.zip");
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        assertEquals("1", entity.getId());
        assertEquals("testUser", entity.getUserName());
        assertEquals("test.mp4", entity.getFileName());
        assertEquals("frames.zip", entity.getFramesUrl());
        assertEquals("file.zip", entity.getFileUrl());
        assertEquals(now, entity.getCreatedAt());
        assertEquals(now, entity.getUpdatedAt());
    }
}