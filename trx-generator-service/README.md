# Transaction Generator Service

A Spring Boot microservice that generates realistic financial transactions and converts them to SWIFT MT103 message format. This service is designed for testing and simulation purposes in banking and financial systems, with support for both valid and invalid transaction scenarios.

## Architecture Overview

The service follows a modular architecture with clear separation of concerns:

```
┌─────────────────────────────────────────────────────────────┐
│                Transaction Generator Service                │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────────────┐    ┌──────────────────┐                │
│  │ Valid Scheduler │    │ Invalid Scheduler│                │
│  │ (Every 10s)     │    │ (Every 15s)      │                │
│  └─────────────────┘    └──────────────────┘                │
│           │                       │                        │
│           ▼                       ▼                        │
│  ┌─────────────────┐    ┌──────────────────┐              │
│  │ Transaction     │    │ Invalid Transaction│             │
│  │ Generator       │    │ Factory           │             │
│  └─────────────────┘    └──────────────────┘              │
│           │                       │                        │
│           │              ┌──────────────────┐              │
│           │              │ Scenario Selector│              │
│           │              │ & Message        │              │
│           │              │ Corruptor        │              │
│           │              └──────────────────┘              │
│           │                       │                        │
│           ▼                       ▼                        │
│  ┌─────────────────┐    ┌──────────────────┐              │
│  │ Bank Data       │    │ MT103 Message    │              │
│  │ Service         │    │ Formatter        │              │
│  └─────────────────┘    └──────────────────┘              │
│           │                       │                        │
│           ▼                       ▼                        │
│  ┌─────────────────┐    ┌──────────────────┐              │
│  │ File Storage    │    │ Kafka Producer   │              │
│  │ Service         │───▶│                  │              │
│  └─────────────────┘    └──────────────────┘              │
│                                   │                        │
│                                   ▼                        │
│                         transaction_generator topic        │
└─────────────────────────────────────────────────────────────┘
```

## Features

### Core Transaction Generation
- **Automated Transaction Generation**: Creates realistic financial transactions every 10 seconds
- **SWIFT MT103 Support**: Full compliance with SWIFT MT103 message format
- **Multi-Currency Support**: Handles EUR, USD, and other major currencies
- **IBAN Generation**: Country-specific IBAN generation and validation
- **Cross-Border Detection**: Automatically detects and handles international transfers

### Invalid Transaction Testing
- **Invalid Transaction Generation**: Creates corrupted transactions every 15 seconds for testing error handling
- **Multiple Invalid Scenarios**: Supports 8 different corruption patterns:
    - Missing mandatory fields
    - Invalid BIC formats
    - Invalid date formats
    - Invalid amount formats
    - Missing header blocks
    - Invalid field structures
    - Truncated messages
    - Invalid characters
- **Weighted Scenario Selection**: Configurable probability weights for different invalid scenarios
- **On-Demand Generation**: Manual generation of invalid transactions for specific testing

### Integration & Storage
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
    com.toufik.trxgeneratorservice.mt103trx.service.InvalidTransactionGeneratorService: WARN
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

### Valid Transaction Object
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

### Invalid Transaction Scenarios

The service generates various types of invalid transactions for testing:

1. **Missing Mandatory Fields**: Missing required fields like `:20:`, `:23B:`, or `:32A:`
2. **Invalid BIC Format**: Malformed SWIFT codes (too short, too long, invalid characters)
3. **Invalid Date Format**: Corrupted date formats in `:32A:` field
4. **Invalid Amount Format**: Malformed amount values (negative, non-numeric, wrong decimal places)
5. **Missing Header Blocks**: Missing Block 1, 2, or 3 headers
6. **Invalid Field Structure**: Corrupted field separators and formatting
7. **Truncated Messages**: Incomplete MT103 messages cut off at various points
8. **Invalid Characters**: Control characters and non-printable characters

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

### Invalid Scenario Weights

The service uses configurable weights for different invalid scenarios:

- Missing Mandatory Fields: 20%
- Invalid BIC Format: 15%
- Invalid Date Format: 15%
- Invalid Amount Format: 15%
- Missing Header Blocks: 10%
- Invalid Field Structure: 10%
- Truncated Messages: 10%
- Invalid Characters: 5%

## Project Structure

```
src/main/java/com/toufik/trxgeneratorservice/
├── mt103trx/
│   ├── model/
│   │   ├── BankInfo.java
│   │   ├── InvalidScenario.java          # Invalid scenario enumeration
│   │   ├── Transaction.java
│   │   └── TransactionWithMT103Event.java
│   ├── service/
│   │   ├── BankDataService.java          # Bank data management
│   │   ├── InvalidScenarioSelector.java  # Weighted scenario selection
│   │   ├── InvalidTransactionFactory.java # Invalid transaction creation
│   │   ├── InvalidTransactionGeneratorService.java # Invalid transaction scheduler
│   │   ├── MT103FileService.java         # File storage operations
│   │   ├── MT103MessageCorruptor.java    # Message corruption logic
│   │   ├── MT103MessageFormatter.java    # SWIFT message formatting
│   │   ├── TransactionGenerator.java     # Basic transaction generation
│   │   ├── TransactionGeneratorService.java # Main orchestrator
│   │   └── TransactionProducer.java      # Kafka integration
│   └── util/
│       └── MT103Constants.java           # SWIFT format constants
└── resources/
    └── banks.csv                         # Bank configuration data
```

## Key Components

### TransactionGeneratorService
- **Purpose**: Main orchestrator that generates valid transactions every 10 seconds
- **Key Features**:
    - Scheduled execution using `@Scheduled`
    - Random transaction generation
    - Integration with all other services

### InvalidTransactionGeneratorService
- **Purpose**: Generates invalid transactions for error testing every 15 seconds
- **Key Features**:
    - Scheduled invalid transaction generation
    - On-demand invalid transaction creation
    - Comprehensive logging of invalid scenarios

### InvalidScenarioSelector
- **Purpose**: Manages weighted selection of invalid scenarios
- **Key Features**:
    - Configurable probability weights
    - Uniform and weighted selection modes
    - Runtime weight adjustment capabilities

### MT103MessageCorruptor
- **Purpose**: Creates corrupted MT103 messages for testing
- **Key Features**:
    - 8 different corruption patterns
    - Realistic invalid data generation
    - Maintains message structure where possible

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

- **INFO**: Valid transaction generation, file operations, Kafka publishing
- **WARN**: Invalid transaction generation and details
- **DEBUG**: IBAN generation details, detailed processing steps
- **ERROR**: Exception handling, failed operations

Monitor the following logs:
```bash
# Valid transaction generation
Generated transaction: <transaction-id>

# Invalid transaction generation
Generated INVALID transaction: <transaction-id>
======================= INVALID TRANSACTION =============================
Invalid Transaction Details: <transaction-details>
Invalid MT103 Content Preview: <mt103-preview>

# File operations  
Saved MT103 file to: <file-path>

# Kafka publishing
Sent transaction with MT103 info to Kafka: <transaction-details>
```

## Testing

### Manual Testing
1. Start the application
2. Monitor logs for both valid and invalid transaction generation
3. Check Kafka topic for published messages:
   ```bash
   bin/kafka-console-consumer.sh --topic transaction_generator --from-beginning --bootstrap-server localhost:9092
   ```
4. Verify MT103 files in the configured directory

### Sample Output
```
# Valid Transaction
Generated transaction: a1b2c3d4-e5f6-7890-abcd-ef1234567890
From IBAN: DE89370400440532013000
To IBAN: FR1420041010050500013M02606
Saved MT103 file to: ./mt103-transactions/MT103_a1b2c3d4_e5f6_7890_abcd_ef1234567890.txt

# Invalid Transaction
Generated INVALID transaction: b2c3d4e5-f6g7-8901-bcde-fg2345678901
======================= INVALID TRANSACTION =============================
Invalid Transaction Details: {...}
Invalid MT103 Content Preview: {1:F01INVALIDXXX0}{2:I103TOOLONGBICCODE123456...
Successfully sent invalid transaction: b2c3d4e5-f6g7-8901-bcde-fg2345678901
```