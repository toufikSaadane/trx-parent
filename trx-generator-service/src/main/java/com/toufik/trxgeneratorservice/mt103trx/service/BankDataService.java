package com.toufik.trxgeneratorservice.mt103trx.service;

import com.toufik.trxgeneratorservice.mt103trx.model.BankInfo;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;

@Service
@Slf4j
public class BankDataService {

    private List<BankInfo> banks = new ArrayList<>();
    private Random random = new Random();

    @PostConstruct
    public void loadBanksFromCsv() {
        try {
            ClassPathResource resource = new ClassPathResource("banks.csv");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
                String line;
                boolean isHeader = true;

                while ((line = reader.readLine()) != null) {
                    if (isHeader) {
                        isHeader = false;
                        continue;
                    }

                    String[] fields = line.split(",");
                    if (fields.length >= 5) {
                        String ibanPrefix = fields.length > 5 ? fields[5].trim() : "";
                        String ibanLengthStr = fields.length > 6 ? fields[6].trim() : "";

                        Integer ibanLength = null;
                        if (!ibanLengthStr.isEmpty()) {
                            try {
                                ibanLength = Integer.parseInt(ibanLengthStr);
                            } catch (NumberFormatException e) {
                                log.warn("Invalid IBAN length for {}: {}", fields[0], ibanLengthStr);
                            }
                        }

                        BankInfo bank = new BankInfo(
                                fields[0].trim(), // swiftCode
                                fields[1].trim(), // countryCode
                                fields[2].trim(), // bankName
                                fields[3].trim(), // routingNumber
                                fields[4].trim(), // currencyCode
                                ibanPrefix.isEmpty() ? fields[1].trim() : ibanPrefix, // use CSV prefix or country code
                                ibanLength // use CSV length or null
                        );
                        banks.add(bank);
                    }
                }
            }
            log.info("Loaded {} banks", banks.size());
        } catch (Exception e) {
            log.error("Error loading banks: {}", e.getMessage());
            throw new RuntimeException("Failed to load bank data", e);
        }
    }

    public BankInfo getRandomBank() {
        return banks.get(random.nextInt(banks.size()));
    }

    public String generateIBAN(BankInfo bank, String accountNumber) {
        // Check if country uses IBAN based on CSV data
        if (bank.getIbanLength() == null || bank.getIbanLength() == 0) {
            return "This country does not use IBAN";
        }

        String countryCode = bank.getIbanPrefix();
        String bankCode = bank.getRoutingNumber().replaceAll("[^0-9]", "");
        if (bankCode.length() > 8) bankCode = bankCode.substring(0, 8);

        String account = padAccount(accountNumber, bank.getIbanLength(), bankCode);

        // Calculate check digits
        String temp = bankCode + account + countryCode + "00";
        String checkDigits = String.format("%02d", 98 - mod97(temp));

        return countryCode + checkDigits + bankCode + account;
    }

    private String padAccount(String accountNumber, int ibanLength, String bankCode) {
        int neededLength = ibanLength - 4 - bankCode.length(); // 4 = country(2) + check(2)
        if (neededLength <= 0) neededLength = 10; // fallback

        long accountNum = Math.abs(accountNumber.hashCode()) % (long)Math.pow(10, neededLength);
        return String.format("%0" + neededLength + "d", accountNum);
    }

    private int mod97(String input) {
        StringBuilder numeric = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (Character.isLetter(c)) {
                numeric.append(c - 'A' + 10);
            } else {
                numeric.append(c);
            }
        }

        int remainder = 0;
        for (char digit : numeric.toString().toCharArray()) {
            remainder = (remainder * 10 + Character.getNumericValue(digit)) % 97;
        }
        return remainder;
    }

    /**
     * Returns all available banks in the system
     * @return List of all bank information
     */
    public List<BankInfo> getAllBanks() {
        return new ArrayList<>(banks);
    }
}