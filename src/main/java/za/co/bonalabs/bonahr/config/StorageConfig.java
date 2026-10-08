package za.co.bonalabs.bonahr.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import za.co.bonalabs.bonahr.storage.FileStorageService;
import za.co.bonalabs.bonahr.storage.LocalFileStorageService;

@Configuration
public class StorageConfig {

    @Bean
    public FileStorageService fileStorageService(@Value("${bonahr.storage.local.root:./data/uploads}") String storageRoot) {
        return new LocalFileStorageService(storageRoot);
    }
}