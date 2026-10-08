package za.co.bonalabs.bonahr.storage;

public record StoredFile(
        String storageKey,
        String fileName,
        String mimeType,
        long fileSize
) {
}