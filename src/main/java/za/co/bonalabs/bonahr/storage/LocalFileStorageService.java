package za.co.bonalabs.bonahr.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

public class LocalFileStorageService implements FileStorageService {

    private final Path storageRoot;

    public LocalFileStorageService(String storageRoot) {
        this.storageRoot = Path.of(storageRoot).toAbsolutePath().normalize();

        try {
            Files.createDirectories(this.storageRoot);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create storage directory", e);
        }
    }

    @Override
    public StoredFile store(UUID organisationId, UUID employeeId, String fileName, String mimeType, long fileSize, InputStream inputStream) {

        String extension = getExtension(fileName);

        String generatedFileName = UUID.randomUUID() + extension;

        String storageKey = "organisations/" + organisationId + "/employees/" + employeeId + "/documents/" + generatedFileName;

        Path target = resolveStorageKey(storageKey);

        try {

            Files.createDirectories(target.getParent());

            Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);

        } catch (IOException e) {

            throw new IllegalStateException("Could not store file: " + fileName, e);
        }

        return new StoredFile(storageKey, fileName, mimeType, fileSize);
    }

    @Override
    public InputStream load(String storageKey) {

        Path file = resolveStorageKey(storageKey);

        try {

            return Files.newInputStream(file);

        } catch (IOException e) {

            throw new IllegalStateException("Could not load file: " + storageKey, e);
        }
    }

    @Override
    public void delete(String storageKey) {

        Path file = resolveStorageKey(storageKey);

        try {

            Files.deleteIfExists(file);

        } catch (IOException e) {

            throw new IllegalStateException("Could not delete file: " + storageKey, e);
        }
    }

    private Path resolveStorageKey(String storageKey) {

        Path resolved = storageRoot.resolve(storageKey).normalize();

        if (!resolved.startsWith(storageRoot)) {

            throw new IllegalArgumentException("Invalid storage key");
        }

        return resolved;
    }

    private String getExtension(String fileName) {

        if (fileName == null) {
            return "";
        }

        String safeFileName = Path.of(fileName).getFileName().toString();

        int index = safeFileName.lastIndexOf('.');

        if (index <= 0 || index == safeFileName.length() - 1) {
            return "";
        }

        return safeFileName.substring(index);
    }
}