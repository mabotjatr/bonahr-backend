package za.co.bonalabs.bonahr.storage;

import java.io.InputStream;
import java.util.UUID;

public interface FileStorageService {

    StoredFile store(UUID organisationId, UUID employeeId, String fileName, String mimeType, long fileSize, InputStream inputStream);

    InputStream load(String storageKey);

    void delete(String storageKey);
}