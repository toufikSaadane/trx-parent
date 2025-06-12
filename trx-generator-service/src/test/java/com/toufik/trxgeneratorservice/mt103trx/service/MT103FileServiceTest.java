package com.toufik.trxgeneratorservice.mt103trx.service;

import com.toufik.trxgeneratorservice.mt103trx.model.Transaction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = "mt103.file.directory=src/test/resources/mt103-transactions")
public class MT103FileServiceTest {

    @Autowired
    private MT103FileService mt103FileService;

    private final Path directoryPath = Paths.get("src/test/resources/mt103-transactions");

    @Test
    public void testSaveTransactionMT103File() throws IOException {
        // Use a unique id to avoid conflict with any static files
        String uniqueId = "txn_" + System.currentTimeMillis();
        Transaction transaction = new Transaction(
                uniqueId,
                "fromAccount1",
                "toAccount1",
                new BigDecimal("1000.00"),
                "USD",
                "AAAABBCCDD",
                "EEEFFGGHHI",
                "Bank A",
                "Bank B",
                LocalDateTime.now(),
                "PENDING"
        );

        String mt103Content = "MT103 for transaction: " + transaction.getTransactionId() + "\n"
                + "From: " + transaction.getFromAccount() + ", Bank: " + transaction.getFromBankName() + "\n"
                + "To: " + transaction.getToAccount() + ", Bank: " + transaction.getToBankName();

        mt103FileService.saveMT103ToFile(transaction.getTransactionId(), mt103Content);

        String filePathString = mt103FileService.getMT103FilePath(transaction.getTransactionId());
        assertNotNull(filePathString, "File path should not be null");

        Path filePath = Paths.get(filePathString);
        assertTrue(Files.exists(filePath), "MT103 file should exist at " + filePath);

        String fileContent = Files.readString(filePath);
        assertEquals(mt103Content, fileContent, "File content should match the transaction data");

        long testFiles = Files.walk(directoryPath)
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().contains(transaction.getTransactionId()))
                .count();
        assertEquals(1, testFiles, "Only one transaction file should be created");
    }

    @AfterEach
    public void cleanup() throws IOException {
        // Delete only files that were generated during the test run (using dynamic id prefix)
        Files.walk(directoryPath)
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().startsWith("txn_"))
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException e) {
                        // ignore cleanup exception
                    }
                });
    }
}