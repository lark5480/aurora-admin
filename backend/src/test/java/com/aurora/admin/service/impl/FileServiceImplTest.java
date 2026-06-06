package com.aurora.admin.service.impl;

import com.aurora.admin.config.ConfigCache;
import com.aurora.admin.entity.FileRecord;
import com.aurora.admin.mapper.FileMapper;
import com.aurora.admin.util.SecurityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileServiceImplTest {

    @Mock
    private FileMapper fileMapper;

    @Mock
    private ConfigCache configCache;

    @InjectMocks
    private FileServiceImpl fileService;

    private MockedStatic<SecurityUtils> securityUtilsMock;

    private FileRecord testFile;

    @BeforeEach
    void setUp() {
        securityUtilsMock = mockStatic(SecurityUtils.class);
        ReflectionTestUtils.setField(fileService, "uploadDir", "/tmp/uploads");

        testFile = new FileRecord();
        testFile.setId(1L);
        testFile.setFileName("test.pdf");
        testFile.setFilePath("2024/01/test.pdf");
        testFile.setFileSize(1024L);
        testFile.setFileType("application/pdf");
        testFile.setUploadUserId(1L);
        testFile.setIsDeleted(0);
    }

    @AfterEach
    void tearDown() {
        securityUtilsMock.close();
    }

    @Nested
    class DeleteFile {

        @Test
        void shouldDeleteFile_whenOwner() {
            when(fileMapper.findById(1L)).thenReturn(testFile);
            securityUtilsMock.when(SecurityUtils::isCurrentUserAdmin).thenReturn(false);
            when(fileMapper.softDelete(1L)).thenReturn(1);

            boolean result = fileService.deleteFile(1L, 1L);

            assertTrue(result);
            verify(fileMapper).softDelete(1L);
        }

        @Test
        void shouldDeleteFile_whenAdmin() {
            when(fileMapper.findById(1L)).thenReturn(testFile);
            securityUtilsMock.when(SecurityUtils::isCurrentUserAdmin).thenReturn(true);
            when(fileMapper.softDelete(1L)).thenReturn(1);

            boolean result = fileService.deleteFile(1L, 999L); // different user, but admin

            assertTrue(result);
        }

        @Test
        void shouldNotDeleteFile_whenNotOwner() {
            when(fileMapper.findById(1L)).thenReturn(testFile);
            securityUtilsMock.when(SecurityUtils::isCurrentUserAdmin).thenReturn(false);

            boolean result = fileService.deleteFile(1L, 999L); // different user, not admin

            assertFalse(result);
            verify(fileMapper, never()).softDelete(anyLong());
        }

        @Test
        void shouldReturnFalse_whenFileNotExists() {
            when(fileMapper.findById(999L)).thenReturn(null);

            boolean result = fileService.deleteFile(999L, 1L);

            assertFalse(result);
        }
    }

    @Nested
    class GetUserFiles {

        @Test
        void shouldReturnUserFiles() {
            List<FileRecord> files = List.of(testFile);
            when(fileMapper.findByUserId(eq(1L), eq(0), eq(10), eq(""))).thenReturn(files);

            List<FileRecord> result = fileService.getUserFiles(1L, 1, 10, "");

            assertEquals(1, result.size());
            assertEquals("test.pdf", result.get(0).getFileName());
        }

        @Test
        void shouldReturnEmptyList_whenNoFiles() {
            when(fileMapper.findByUserId(eq(1L), eq(0), eq(10), eq("notfound"))).thenReturn(List.of());

            List<FileRecord> result = fileService.getUserFiles(1L, 1, 10, "notfound");

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    class GetAllFiles {

        @Test
        void shouldReturnAllFiles_forAdmin() {
            List<FileRecord> files = List.of(testFile);
            when(fileMapper.findPage(eq(0), eq(10), eq(""))).thenReturn(files);

            List<FileRecord> result = fileService.getAllFiles(1, 10, "");

            assertEquals(1, result.size());
        }
    }

    @Nested
    class GetFileById {

        @Test
        void shouldReturnFile_whenExists() {
            when(fileMapper.findById(1L)).thenReturn(testFile);

            FileRecord result = fileService.getFileById(1L);

            assertNotNull(result);
            assertEquals("test.pdf", result.getFileName());
        }

        @Test
        void shouldReturnNull_whenNotExists() {
            when(fileMapper.findById(999L)).thenReturn(null);

            FileRecord result = fileService.getFileById(999L);

            assertNull(result);
        }
    }

    @Nested
    class UploadValidation {

        @Test
        void shouldRejectFileExceedingSizeLimit() {
            MultipartFile file = mock(MultipartFile.class);
            when(file.getOriginalFilename()).thenReturn("large.zip");
            when(file.getSize()).thenReturn(20L * 1024 * 1024); // 20MB
            when(configCache.getInt("upload.max_size", 10)).thenReturn(10);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> fileService.uploadFile(file, 1L, "user"));

            assertTrue(ex.getMessage().contains("文件大小超过限制"));
        }

        @Test
        void shouldRejectUnsupportedFileType() {
            MultipartFile file = mock(MultipartFile.class);
            when(file.getOriginalFilename()).thenReturn("malware.exe");
            when(file.getSize()).thenReturn(1024L);
            when(configCache.getInt("upload.max_size", 10)).thenReturn(10);
            when(configCache.getString("upload.allowed_types", "")).thenReturn(".jpg,.png,.pdf");

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> fileService.uploadFile(file, 1L, "user"));

            assertTrue(ex.getMessage().contains("不支持的文件类型"));
        }
    }
}
