package br.com.fiap.hacka.core.commons.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FilePartDtoTest {

    @Test
    void constructor_ShouldCreateEmptyObject_WhenNoArgsConstructor() {
        FilePartDto dto = new FilePartDto();

        assertNotNull(dto);
        assertNull(dto.getFileName());
        assertNull(dto.getUserName());
        assertEquals(0, dto.getBytesRead());
        assertFalse(dto.isFirstChunk());
    }

    @Test
    void constructor_ShouldCreateObjectWithAllFields_WhenAllArgsConstructor() {
        byte[] testBytes = new byte[]{1, 2, 3};
        FilePartDto dto = new FilePartDto("test.mp4", 1024, testBytes, true, "testUser", "http://webhook", "frames.zip", "file.zip");

        assertEquals("test.mp4", dto.getFileName());
        assertEquals(1024, dto.getBytesRead());
        assertArrayEquals(testBytes, dto.getBytes());
        assertTrue(dto.isFirstChunk());
        assertEquals("testUser", dto.getUserName());
        assertEquals("http://webhook", dto.getWebhookUrl());
        assertEquals("frames.zip", dto.getFrameFilePath());
        assertEquals("file.zip", dto.getFileUrl());
    }

    @Test
    void chunkSize_ShouldBe1MB() {
        assertEquals(1024 * 1024, FilePartDto.CHUNK_SIZE);
    }

    @Test
    void settersAndGetters_ShouldWorkCorrectly() {
        FilePartDto dto = new FilePartDto();
        byte[] testBytes = new byte[]{1, 2, 3};

        dto.setFileName("test.mp4");
        dto.setBytesRead(1024);
        dto.setBytes(testBytes);
        dto.setFirstChunk(true);
        dto.setUserName("testUser");
        dto.setWebhookUrl("http://webhook");
        dto.setFrameFilePath("frames.zip");
        dto.setFileUrl("file.zip");

        assertEquals("test.mp4", dto.getFileName());
        assertEquals(1024, dto.getBytesRead());
        assertArrayEquals(testBytes, dto.getBytes());
        assertTrue(dto.isFirstChunk());
        assertEquals("testUser", dto.getUserName());
        assertEquals("http://webhook", dto.getWebhookUrl());
        assertEquals("frames.zip", dto.getFrameFilePath());
        assertEquals("file.zip", dto.getFileUrl());
    }
}