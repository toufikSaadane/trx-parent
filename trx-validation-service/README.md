# Transaction Validation Service

A Spring Boot microservice that validates MT103 SWIFT transaction messages, stores validation results in MongoDB, and forwards valid transactions to an alert topic for downstream processing.

## Overview

This service consumes transaction events containing MT103 messages from Kafka, validates them against SWIFT MT103 standards, stores all transactions (valid and invalid) in MongoDB with validation results, and forwards only valid transactions to an alert topic for further processing.

## Architecture

```
Kafka Topic: transaction_generator
        ↓
Transaction Consumer Service
        ↓
Transaction Filter Service
        ↓
MongoDB (transactions_validation collection)
        ↓ (valid transactions only)
Kafka Topic: transaction_alert
```

## Key Components

- **TransactionConsumerService**: Consumes transactions from `transaction_generator` topic
- **TransactionFilterService**: Validates MT103 content and filters transactions
- **TransactionProducerService**: Sends valid transactions to `transaction_alert` topic
- **TransactionValidationController**: REST API for querying transaction validation results
- **TransactionRepository**: MongoDB repository for transaction persistence

## REST API Endpoints

The service exposes the following REST endpoints for querying transaction validation results:

### Get All Transactions
```http
GET /api/transactions/all
```
**Description**: Retrieves all processed transactions (both valid and invalid)
**Response**: List of Transaction objects with validation status

### Get Valid Transactions
```http
GET /api/transactions/valid
```
**Description**: Retrieves only transactions that passed validation
**Response**: List of valid Transaction objects

### Get Invalid Transactions
```http
GET /api/transactions/invalid
```
**Description**: Retrieves only transactions that failed validation
**Response**: List of invalid Transaction objects with validation reasons

### Response Format
```json
[
  {
    "id": "mongo-generated-id",
    "transactionId": "cd6d508c-5049-4a",
    "fromAccount": "201093193710",
    "toAccount": "destination-account",
    "amount": 38329.19,
    "currency": "EUR",
    "fromBankSwift": "COBADEFF",
    "toBankSwift": "UNCRITMM",
    "fromBankName": "Commerzbank AG",
    "toBankName": "UniCredit Bank AG",
    "timestamp": "2025-06-22T10:30:00",
    "status": "PENDING",
    "fromIBAN": "DE89370400440532013000",
    "toIBAN": "DE89370400440532013001",
    "fromCountryCode": "DE",
    "toCountryCode": "IT",
    "isValid": true,
    "validationReason": "Valid",
    "processedAt": "2025-06-27T14:15:30"
  }
]
```

## MongoDB Configuration

### Database Connection
- **Database**: `transactions`
- **Collection**: `transactions_validation`
- **Connection URI**: `mongodb://localhost:27017/transactions`

### Transaction Document Structure
The service stores each processed transaction with the following additional validation fields:

```javascript
{
  // Original transaction fields
  "transactionId": "string",
  "fromAccount": "string",
  "toAccount": "string",
  "amount": "decimal",
  "currency": "string",
  "fromBankSwift": "string",
  "toBankSwift": "string",
  "fromBankName": "string",
  "toBankName": "string",
  "timestamp": "datetime",
  "status": "string",
  "fromIBAN": "string",
  "toIBAN": "string",
  "fromCountryCode": "string",
  "toCountryCode": "string",
  
  // Validation-specific fields
  "isValid": "boolean",           // true if transaction passed validation
  "validationReason": "string",   // "Valid" or specific failure reason
  "processedAt": "datetime"       // timestamp when validation was performed
}
```

### MongoDB Indexes
Consider adding indexes for optimal query performance:
```javascript
// Index on validation status for filtering endpoints
db.transactions_validation.createIndex({ "isValid": 1 })

// Index on transaction ID for unique lookups
db.transactions_validation.createIndex({ "transactionId": 1 })

// Index on processed timestamp for time-based queries
db.transactions_validation.createIndex({ "processedAt": -1 })
```

## Business Rules

### Transaction Processing Flow
1. **Consume** transactions from the input topic
2. **Validate** MT103 message structure and content
3. **Store** transaction in MongoDB with validation results
4. **Forward** only valid transactions to the alert topic
5. **Log** all processing activities

### Validation Rules

#### MT103 Structure Validation
- **Block Structure**: Must contain all 5 required blocks `{1:...}{2:...}{3:...}{4:...}{5:...}`
- **Header Block 1**: Format `{1:F01[11-char BIC][1 digit]}`
- **Header Block 2**: Format `{2:I103[11-char BIC][1 digit][1 char]}`
- **Trailer Block 5**: Format `{5:{MAC:[hex]}{CHK:[hex]}}`

#### Mandatory Fields (Block 4)
- **:20:** - Transaction Reference Number
- **:23B:** - Bank Operation Code (e.g., CRED)
- **:32A:** - Value Date, Currency Code, and Amount

#### Field Format Validation
- **BIC Codes** (fields :52A:, :57A:):
  - Length: 8-11 characters
  - Format: `[4 letters][2 letters][2 alphanumeric][optional 3 alphanumeric]`

- **Date Format** (field :32A:):
  - Format: YYMMDD (6 digits)
  - Valid month: 01-12
  - Valid day: 01-31

- **Amount Format** (field :32A:):
  - Numeric with optional decimal separator (comma or period)
  - Pattern: `\d+([,.]?\d{1,2})?`

#### Field Structure Integrity
- No double colons (`::`) allowed
- No semicolons (`;`) instead of colons in field tags
- Proper field tag format: `:[2 digits][optional letter]:`

## Configuration

### Application Properties (application.yml)
```yaml
server:
  port: 3003

spring:
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: transaction-validator-group
      auto-offset-reset: latest
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
  
  data:
    mongodb:
      uri: mongodb://localhost:27017/transactions
```

### Kafka Topics
- **Input**: `transaction_generator`
- **Output**: `transaction_alert`

## Data Models

### Transaction
```java
{
  "transactionId": "string",
  "fromAccount": "string",
  "toAccount": "string",
  "amount": "decimal",
  "currency": "string",
  "fromBankSwift": "string",
  "toBankSwift": "string",
  "fromBankName": "string",
  "toBankName": "string",
  "timestamp": "datetime",
  "status": "string",
  "fromIBAN": "string",
  "toIBAN": "string",
  "fromCountryCode": "string",
  "toCountryCode": "string",
  "isValid": "boolean",
  "validationReason": "string",
  "processedAt": "datetime"
}
```

### TransactionWithMT103Event
```java
{
  "transaction": Transaction,
  "mt103Content": "string"
}
```

## Validation Examples

### Valid MT103 Structure
```
{1:F01COBADEFFXXX0}{2:I103UNCRITMMXXX0N}{3:{108:cd6d508c-5049-4a}}
{4:
:20:cd6d508c-5049-4a
:23B:CRED
:32A:250622EUR38329,19
:52A:COBADEFF
:57A:UNCRITMM
:59:/201093193710
...
}
{5:{MAC:9A90B885}{CHK:E065669BF6C5}}
```

### Common Validation Failures
- Missing mandatory fields (`:20:`, `:23B:`, `:32A:`)
- Invalid BIC format in `:52A:` or `:57A:` fields
- Invalid date format in `:32A:` field
- Invalid amount format in `:32A:` field
- Missing or malformed structure blocks
- Content is null or blank
- Invalid field structure with double colons or semicolons

## Error Handling

- **Consumer Errors**: Logged and message processing continues
- **Validation Failures**: Transaction stored as invalid with reason, not forwarded
- **Producer Errors**: Exception thrown, transaction processing fails
- **Database Errors**: Logged, processing continues
- **Kafka Connectivity**: Automatic retry with exponential backoff

## Monitoring & Logging

The service provides comprehensive logging for:
- Transaction consumption events with topic, partition, and offset details
- Validation results (success/failure with specific reasons)
- Database save operations
- Producer send confirmations
- Error conditions with stack traces
- REST API request handling

## Running the Service

### Prerequisites
- Java 11 or higher
- Apache Kafka running on `localhost:9092`
- MongoDB running on `localhost:27017`

### Startup
1. Start Kafka and MongoDB
2. Run the Spring Boot application: `./mvnw spring-boot:run`
3. Service will be available on port `3003`
4. REST API endpoints accessible at `http://localhost:3003/api/transactions/`

## Testing

### Manual Testing with REST API
```bash
# Get all processed transactions
curl http://localhost:3003/api/transactions/all

# Get only valid transactions
curl http://localhost:3003/api/transactions/valid

# Get only invalid transactions
curl http://localhost:3003/api/transactions/invalid
```

### Unit Tests
- `TransactionFilterServiceTest`: Validation logic testing
- `TransactionValidationComponentTest`: Component interaction testing

### Integration Tests
- `TransactionValidationIntegrationTest`: End-to-end flow with embedded Kafka and MongoDB