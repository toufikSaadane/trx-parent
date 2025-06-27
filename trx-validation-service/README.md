# Transaction Validation Service

A Spring Boot microservice that validates MT103 SWIFT transaction messages and forwards valid transactions to an alert topic for downstream processing.

## Overview

This service consumes transaction events containing MT103 messages from Kafka, validates them against SWIFT MT103 standards, and forwards only valid transactions to an alert topic for further processing.

## Architecture

```
Kafka Topic: transaction_generator
        ↓
Transaction Consumer Service
        ↓
Simple Transaction Filter Service
        ↓ (valid transactions only)
Kafka Topic: transaction_alert
```

## Key Components

- **TransactionConsumerService**: Consumes transactions from `transaction_generator` topic
- **SimpleTransactionFilterService**: Validates MT103 content and filters transactions
- **TransactionProducerService**: Sends valid transactions to `transaction_alert` topic

## Business Rules

### Transaction Processing Flow
1. **Consume** transactions from the input topic
2. **Validate** MT103 message structure and content
3. **Forward** only valid transactions to the alert topic
4. **Log** invalid transactions with rejection reasons

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

### Application Properties
```properties
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.consumer.group-id=transaction-validator-group
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
  "timestamp": "datetime",
  // ... additional fields
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

## Error Handling

- **Consumer Errors**: Logged and message processing continues
- **Validation Failures**: Transaction rejected, reason logged
- **Producer Errors**: Exception thrown, transaction processing fails
- **Kafka Connectivity**: Automatic retry with exponential backoff

## Monitoring & Logging

The service provides comprehensive logging for:
- Transaction consumption events
- Validation results (success/failure with reasons)
- Producer send confirmations
- Error conditions with stack traces

## Testing

### Unit Tests
- `SimpleTransactionFilterServiceTest`: Validation logic testing
- `TransactionValidationComponentTest`: Component interaction testing

### Integration Tests
- `TransactionValidationIntegrationTest`: End-to-end flow with embedded Kafka
