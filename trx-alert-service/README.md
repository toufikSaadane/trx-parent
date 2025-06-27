# Transaction Alert Service

A Spring Boot microservice that consumes transaction events from Kafka, performs fraud detection using multiple rules, and sends email alerts for suspicious transactions.

## Features

- **Real-time Transaction Processing**: Consumes transaction events from Kafka
- **Multi-Rule Fraud Detection**: Implements 4 fraud detection rules
- **Email Notifications**: Sends HTML email alerts for fraudulent transactions
- **Transaction Storage**: Persists all transactions with fraud analysis to MongoDB
- **REST API**: Provides endpoints to query transactions and fraud alerts
- **CORS Support**: Configured for frontend integration

## Architecture

```
Kafka Topic → Alert Service → Fraud Detection → Email + MongoDB Storage
```

## Fraud Detection Rules

1. **High Risk Country Detection** (HIGH)
    - Flags transactions to/from: Afghanistan, Iran, North Korea, Myanmar, Syria, Yemen

2. **High Amount Detection** (HIGH)
    - Triggers on transactions ≥ €1,000,000 (with currency conversion)

3. **Suspicious Remittance Detection** (MEDIUM)
    - Detects pattern amounts: 999, 9999, 99999, 999999

4. **Off Hours Detection** (LOW)
    - Flags transactions between 23:00-06:00

## Quick Start

### Prerequisites

- Java 17+
- MongoDB running on `localhost:27017`
- Kafka running on `localhost:9092`
- Gmail account with App Password (for email alerts)

### Environment Variables

```bash
export GMAIL_USERNAME=your-email@gmail.com
export GMAIL_APP_PASSWORD=your-app-password
export FRAUD_SENDER_EMAIL=fraud-alert@yourcompany.com
```

### Run the Service

```bash
# Clone and navigate to project
./mvnw spring-boot:run
```

The service will start on port `3004`.

## Configuration

### Kafka Topic
- **Topic Name**: `transaction_alert`
- **Consumer Group**: `transaction-alert-group`

### Email Configuration
Update `application.yml`:
```yaml
fraud:
  email:
    recipient-email: your-email@gmail.com
    sender-email: fraud-alert@yourcompany.com
```

### Database
- **MongoDB**: `mongodb://localhost:27017/transactions`
- **Collection**: `transactions`

## API Endpoints

### Get All Transactions
```http
GET /api/transactions
```

### Get Fraudulent Transactions
```http
GET /api/transactions/fraudulent
GET /api/transactions/fraudulent?start=2024-01-01T00:00:00&end=2024-01-31T23:59:59
```

## Message Format

The service expects Kafka messages in this format:

```json
{
  "transaction": {
    "transactionId": "TXN123456",
    "fromAccount": "ACC001",
    "toAccount": "ACC002",
    "amount": 50000.00,
    "currency": "EUR",
    "fromBankSwift": "DEUTDEFF",
    "toBankSwift": "CHASUS33",
    "fromBankName": "Deutsche Bank",
    "toBankName": "Chase Bank",
    "timestamp": "2024-01-15T14:30:00",
    "status": "PENDING",
    "fromIBAN": "DE89370400440532013000",
    "toIBAN": "US64SVBKUS6S3300958879",
    "fromCountryCode": "DE",
    "toCountryCode": "US"
  },
  "mt103Content": "SWIFT MT103 message content..."
}
```

## Email Alerts

When fraud is detected, the service sends HTML email alerts containing:
- Alert summary with count and timestamp
- Detailed breakdown of each fraud rule triggered
- Transaction details and severity levels
- Professional styling with responsive design

## Technology Stack

- **Spring Boot 3.x**: Main framework
- **Spring Kafka**: Message consumption
- **Spring Data MongoDB**: Data persistence
- **Spring Mail**: Email notifications
- **Thymeleaf**: Email template processing
- **Lombok**: Code generation
- **Jackson**: JSON processing

## Development

### Project Structure
```
src/main/java/com/toufik/trxalertservice/
├── config/          # Configuration classes
├── controller/      # REST controllers
├── entity/          # MongoDB entities
├── fraud/           # Fraud detection engine & rules
├── model/           # Data transfer objects
├── repository/      # MongoDB repositories
└── service/         # Business logic services
```

### Adding New Fraud Rules

1. Implement `FraudDetectionRule` interface
2. Add rule to `FraudDetectionEngine`
3. Configure severity in `determineSeverity()` method


## Monitoring

Check application logs for:
- Transaction processing status
- Fraud detection results
- Email delivery confirmation
- Error handling

### Common Issues

1. **Email not sending**: Verify Gmail App Password and SMTP settings
2. **Kafka connection**: Ensure Kafka is running on `localhost:9092`
3. **MongoDB connection**: Verify MongoDB is accessible on `localhost:27017`
4. **CORS issues**: Frontend origin is configured for `http://localhost:3000`
