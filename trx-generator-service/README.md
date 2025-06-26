# TRX Generator Service

A Spring Boot service that generates financial transactions with MT103 SWIFT message formatting 
for testing and simulation purposes.

## Overview

This service automatically generates three types of financial transactions:
- **Normal transactions** - Standard valid transactions
- **Fraud transactions** - Transactions with suspicious patterns
- **Invalid transactions** - Malformed MT103 messages

All transactions are sent to Kafka and stored in MongoDB.


## Features

- Automatic transaction generation with configurable intervals
- MT103 SWIFT message formatting
- Bank data from CSV with IBAN support
- Real-time monitoring endpoints
- Kafka message publishing
- MongoDB persistence

## Technology Stack

- Spring Boot 3.x
- Spring Kafka
- MongoDB
- ModelMapper
- Lombok
- Jackson (JSON/Time handling)

## Quick Start

### Prerequisites
- Java 17+
- MongoDB running on localhost:27017
- Kafka running on localhost:9092

### Running the Application
```bash
mvn spring-boot:run
```

The service starts on port 3002.

## Configuration

### Application Properties (application.yml)
```yaml
server:
  port: 3002

spring:
  kafka:
    bootstrap-servers: localhost:9092
  data:
    mongodb:
      uri: mongodb://localhost:27017/transactions
```

### Generation Intervals
- Normal transactions: every 10 seconds
- Fraud transactions: every 10 seconds
- Invalid transactions: every 15 seconds

## API Endpoints

### Monitoring
- `GET /api/monitoring/stats` - Transaction statistics
- `GET /api/monitoring/recent` - Recent 10 transactions
- `GET /api/monitoring/fraud` - All fraud transactions
- `GET /api/monitoring/type/{type}` - Transactions by type (NORMAL/FRAUD/INVALID)
- `GET /api/monitoring/today` - Today's transactions
- `GET /api/monitoring/all` - All transactions

## Transaction Types

### Normal Transactions
- Valid MT103 format
- Realistic bank data
- Medium amounts (10K-100K)
- Standard processing

### Fraud Transactions
- High amounts (1M-10M)
- Off-hours timing
- Suspicious remittance patterns
- Cross-border high-risk countries

### Invalid Transactions
- Missing mandatory fields
- Invalid BIC formats
- Malformed dates/amounts
- Truncated messages

## Kafka Integration

Publishes to topic: `transaction_generator`
Message format: `TransactionWithMT103Event` containing transaction data and MT103 content.

## Development

### Running Tests
```bash
mvn test
```

### Building
```bash
mvn clean package
```

## MT103 Message Examples

### Valid MT103 Message
```
{1:F01DEUTDEFFXXX0}{2:I103CHASUS33XXXN}{3:{108:12345678}}
{4:
:20:12345678
:23B:CRED
:32A:240115EUR1500,75
:33B:EUR1500,75
:71A:SHA
:50K:/DE89370400440532013000
Deutsche Bank
123 Main Street
Frankfurt, Germany
:52A:DEUTDEFF
:53B:/DEUTDEFF
:56A:CHASUS33XXX
:57A:CHASUS33
:59:/US1234567890123456789
JPMorgan Chase
456 Business Ave
New York, United States
:70:Payment for services - TXN ID: 12345678 - Cross-border transfer
:72:/INS/DEUTDEFF
}
{5:{MAC:A1B2C3D4}{CHK:123456789ABC}}
```

### Fraud MT103 Message
```
{1:F01DEUTDEFFXXX0}{2:I103CHASUS33XXXN}{3:{108:87654321}}
{4:
:20:87654321
:23B:CRED
:32A:240115USD25000,00
:33B:USD25000,00
:71A:SHA
:50K:/DE89370400440532013000
Deutsche Bank
789 Commercial Blvd
Frankfurt, Germany
:52A:DEUTDEFF
:53B:/DEUTDEFF
:56A:CHASUS33XXX
:57A:CHASUS33
:59:/US9876543210987654321
JPMorgan Chase
321 Financial District
New York, United States
:70:Large value transfer - Ref: 87654321
:72:/INS/DEUTDEFF
}
{5:{MAC:E5F6G7H8}{CHK:987654321DEF}}
```

### Invalid/Corrupted MT103 Message
```
{1:F01DEUTDEFFXXX0}{2:I103CHASUS33XXXN}
{4:
20:INVALID123
:23B;CRED
:32A:20241301USD1000,INVALID
:50K/ACC123456789
Missing Bank Name
:59/ACC987654321
Incomplete beneficiary
```

## CORS Configuration

Configured to allow requests from `http://localhost:3002` for development purposes.