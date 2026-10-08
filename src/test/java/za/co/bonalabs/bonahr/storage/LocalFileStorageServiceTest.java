package za.co.bonalabs.bonahr.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LocalFileStorageServiceTest {

    @TempDir
    Path tempDirectory;

    @Test
    void shouldStoreAndLoadFile() throws Exception {

        LocalFileStorageService storageService = new LocalFileStorageService(tempDirectory.toString());

        UUID organisationId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();

        byte[] content = "BonaHR employee document".getBytes(StandardCharsets.UTF_8);

        StoredFile storedFile;

        try (InputStream inputStream = new java.io.ByteArrayInputStream(content)) {

            storedFile = storageService.store(organisationId, employeeId, "employment-contract.pdf", "application/pdf", content.length, inputStream);
        }

        assertNotNull(storedFile);

        assertNotNull(storedFile.storageKey());

        assertEquals("employment-contract.pdf", storedFile.fileName());

        assertEquals("application/pdf", storedFile.mimeType());

        assertEquals(content.length, storedFile.fileSize());

        assertTrue(storedFile.storageKey().startsWith("organisations/" + organisationId + "/employees/" + employeeId + "/documents/"));

        byte[] loadedContent;

        try (InputStream inputStream = storageService.load(storedFile.storageKey())) {

            loadedContent = inputStream.readAllBytes();
        }

        assertArrayEquals(content, loadedContent);
    }

    @Test
    void shouldDeleteStoredFile() throws Exception {

        LocalFileStorageService storageService = new LocalFileStorageService(tempDirectory.toString());

        UUID organisationId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();

        byte[] content = "BonaHR document to delete".getBytes(StandardCharsets.UTF_8);

        StoredFile storedFile;

        try (InputStream inputStream = new java.io.ByteArrayInputStream(content)) {

            storedFile = storageService.store(organisationId, employeeId, "id-document.pdf", "application/pdf", content.length, inputStream);
        }

        // Prove the file exists before deleting it.
        try (InputStream inputStream = storageService.load(storedFile.storageKey())) {

            assertArrayEquals(content, inputStream.readAllBytes());
        }

        storageService.delete(storedFile.storageKey());

        assertThrows(IllegalStateException.class, () -> storageService.load(storedFile.storageKey()));
    }

    @Test
    void shouldRejectPathTraversalOutsideStorageRoot() {

        LocalFileStorageService storageService = new LocalFileStorageService(tempDirectory.toString());

        String maliciousStorageKey = "../../outside-bonahr.txt";

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> storageService.load(maliciousStorageKey));

        assertEquals("Invalid storage key", exception.getMessage());
    }
}