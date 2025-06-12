package com.toufik.trxgeneratorservice.mt103trx.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class MT103FileService {

    private final Map<String, String> mt103FileMap = new ConcurrentHashMap<>();

    @Value("${mt103.file.directory:./mt103-transactions}")
    private String mt103Directory;


    public void saveMT103ToFile(String transactionId, String mt103Content) throws IOException {
        log.info("Attempting to save MT103 file for transaction: {}", transactionId);

        if (transactionId == null || mt103Content == null) {
            log.error("Transaction ID or MT103 content is null");
            return;
        }

        // Create directory if it doesn't exist
        Path directoryPath = Paths.get(mt103Directory);

        if (!Files.exists(directoryPath)) {
            Files.createDirectories(directoryPath);
            log.info("Created directory at: {}", directoryPath.toAbsolutePath());
        }

        // Generate filename
        String fileName = "MT103_" + transactionId.replaceAll("[^a-zA-Z0-9]", "_") + ".txt";
        Path filePath = directoryPath.resolve(fileName);

        // Write MT103 content to file
        try (FileWriter writer = new FileWriter(filePath.toFile())) {
            writer.write(mt103Content);
        }

        log.info("Saved MT103 file to: {}", filePath.toAbsolutePath());

        // Store in map for quick access
        mt103FileMap.put(transactionId, filePath.toString());
        log.info("Current map size: {}", mt103FileMap.size());
    }

    public String getMT103FilePath(String transactionId) {
        String path = mt103FileMap.get(transactionId);
        log.info("Retrieved path for transaction {}: {}", transactionId, path);
        return path;
    }
}