# Transaction Generator Service

A Spring Boot microservice that generates realistic financial transactions and converts them to SWIFT MT103 message format. This service is designed for testing and simulation purposes in banking and financial systems.

## Architecture Overview

The service follows a modular architecture with clear separation of concerns:

```
┌─────────────────────────────────────────────────────────────┐
│                Transaction Generator Service                │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────────┐    ┌──────────────────┐                │
│  │ Scheduler       │    │ Bank Data        │                │
│  │ (Every Xs)     │───▶│ Service          │                │
│  └─────────────────┘    └──────────────────┘                │
│           │                       │                        │
│           ▼                       ▼                        │
│  ┌─────────────────┐    ┌──────────────────┐              │
│  │ Transaction     │    │ IBAN Generation  │              │
│  │ Generator       │◄───┤ & Validation     │              │
│  └─────────────────┘    └──────────────────┘              │
│           │                                                │
│           ▼                                                │
│  ┌─────────────────┐    ┌──────────────────┐              │
│  │ MT103 Message   │    │ File Storage     │              │
│  │ Formatter       │───▶│ Service          │              │
│  └─────────────────┘    └──────────────────┘              │
│           │                                                │
│           ▼                                                │
│  ┌─────────────────┐                                      │
│  │ Kafka Producer  │───▶ transaction_generator topic       │
│  └─────────────────┘                                      │
└─────────────────────────────────────────────────────────────┘
```

## Features

- **Automated Transaction Generation**: Creates realistic financial transactions every 10 seconds
- **SWIFT MT103 Support**: Full compliance with SWIFT MT103 message format
- **Multi-Currency Support**: Handles EUR, USD, and other major currencies
- **IBAN Generation**: Country-specific IBAN generation and validation
- **Cross-Border Detection**: Automatically detects and handles international transfers
- **Kafka Integration**: Publishes transaction events to Kafka topics
- **File Storage**: Persists MT103 messages to the filesystem
- **Realistic Data**: Generates transactions with varying amounts, statuses, and bank combinations

## Prerequisites

- Java 17+
- Apache Kafka
- Maven 3.6+
- Spring Boot 3.x

## Installation & Setup

### 1. Clone the Repository
```bash
git clone <repository-url>
cd transaction-generator-service
```

### 2. Configure Application Properties
Create `application.yml` or `application.properties`:

```yaml
# Kafka Configuration
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer

# MT103 File Storage
mt103:
  file:
    directory: ./mt103-transactions

# Logging
logging:
  level:
    com.toufik.trxgeneratorservice: INFO
```

### 3. Prepare Bank Data
Create `src/main/resources/banks.csv` with the following format:

```csv
swiftCode,countryCode,bankName,routingNumber,currencyCode,ibanPrefix,ibanLength
DEUTDEFF,DE,Deutsche Bank,37040044,EUR,DE,22
BNPAFRPP,FR,BNP Paribas,30004,EUR,FR,27
CHASUS33,US,JPMorgan Chase,021000021,USD,,0
BARCGB22,GB,Barclays Bank,20-00-00,GBP,GB,22
```

### 4. Start Kafka
```bash
# Start Zookeeper
bin/zookeeper-server-start.sh config/zookeeper.properties

# Start Kafka
bin/kafka-server-start.sh config/server.properties

# Create topic
bin/kafka-topics.sh --create --topic transaction_generator --bootstrap-server localhost:9092
```

### 5. Run the Application
```bash
mvn spring-boot:run
```

## Generated Data Structure

### Transaction Object
```json
{
  "transactionId": "uuid-string",
  "fromAccount": "123456789012",
  "toAccount": "987654321098",
  "amount": 1500.00,
  "currency": "EUR",
  "fromBankSwift": "DEUTDEFF",
  "toBankSwift": "BNPAFRPP",
  "fromBankName": "Deutsche Bank",
  "toBankName": "BNP Paribas",
  "timestamp": "2025-06-11T14:30:00",
  "status": "PENDING",
  "fromIBAN": "DE89370400440532013000",
  "toIBAN": "FR1420041010050500013M02606",
  "fromCountryCode": "DE",
  "toCountryCode": "FR"
}
```

### MT103 Message Structure
The service generates complete SWIFT MT103 messages with all required fields:

- **Block 1**: Basic Header with sender information
- **Block 2**: Application Header with message type and receiver
- **Block 3**: User Header with transaction reference
- **Block 4**: Text Block with transaction details
- **Block 5**: Trailer with MAC and checksum

## Configuration Options

| Property | Default | Description |
|----------|---------|-------------|
| `mt103.file.directory` | `./mt103-transactions` | Directory for storing MT103 files |
| `spring.kafka.bootstrap-servers` | `localhost:9092` | Kafka broker addresses |

## Project Structure

```
src/main/java/com/toufik/trxgeneratorservice/
├── mt103trx/
│   ├── model/
│   │   ├── BankInfo.java
│   │   ├── Transaction.java
│   │   └── TransactionWithMT103Event.java
│   ├── service/
│   │   ├── BankDataService.java          # Bank data management
│   │   ├── MT103FileService.java         # File storage operations
│   │   ├── MT103MessageFormatter.java    # SWIFT message formatting
│   │   ├── TransactionGeneratorService.java # Main orchestrator
│   │   └── TransactionProducer.java      # Kafka integration
│   └── util/
│       └── MT103Constants.java           # SWIFT format constants
└── resources/
    └── banks.csv                         # Bank configuration data
```

## Key Components

### TransactionGeneratorService
- **Purpose**: Main orchestrator that generates transactions every 10 seconds
- **Key Features**:
    - Scheduled execution using `@Scheduled`
    - Random transaction generation
    - Integration with all other services

### BankDataService
- **Purpose**: Manages bank data and IBAN generation
- **Key Features**:
    - CSV-based bank data loading
    - Country-specific IBAN generation
    - IBAN validation and formatting

### MT103MessageFormatter
- **Purpose**: Converts transactions to SWIFT MT103 format
- **Key Features**:
    - Complete MT103 block structure
    - Cross-border transaction detection
    - Intermediary bank routing
    - Proper field formatting and validation

### TransactionProducer
- **Purpose**: Publishes transaction events to Kafka
- **Key Features**:
    - JSON serialization
    - File storage coordination
    - Error handling and logging

## Monitoring & Logging

The service provides comprehensive logging at multiple levels:

- **INFO**: Transaction generation, file operations, Kafka publishing
- **DEBUG**: IBAN generation details, detailed processing steps
- **ERROR**: Exception handling, failed operations

Monitor the following logs:
```bash
# Transaction generation
Generated transaction: <transaction-id>

# File operations  
Saved MT103 file to: <file-path>

# Kafka publishing
Sent transaction with MT103 info to Kafka: <transaction-details>
```

## Testing

### Manual Testing
1. Start the application
2. Monitor logs for transaction generation
3. Check Kafka topic for published messages:
   ```bash
   bin/kafka-console-consumer.sh --topic transaction_generator --from-beginning --bootstrap-server localhost:9092
   ```
4. Verify MT103 files in the configured directory

### Sample Output
```
Generated transaction: a1b2c3d4-e5f6-7890-abcd-ef1234567890
From IBAN: DE89370400440532013000
To IBAN: FR1420041010050500013M02606
Saved MT103 file to: ./mt103-transactions/MT103_a1b2c3d4_e5f6_7890_abcd_ef1234567890.txt
```

---

**Note**: This service is designed for testing and simulation purposes. Do not use with real financial data or in production banking systems without proper security reviews and compliance checks.

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For questions and support, please open an issue in the repository or contact the development team.