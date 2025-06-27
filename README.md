# TRX Services

A microservices-based transaction processing system with Kafka for event streaming and MongoDB for data persistence.

## Architecture

- **trx-generator-service**: Generates transaction events
- **trx-validation-service**: Validates incoming transactions
- **trx-alert-service**: Handles transaction alerts and notifications
- **Apache Kafka**: Event streaming platform
- **MongoDB**: Document database for data storage

## Prerequisites

- Docker and Docker Compose
- Java 17 (for local development)
- Maven 3.6+ (for building)

## Quick Start

### 1. Clean Start (Recommended)

Remove all existing containers, images, and volumes, then start fresh:

```bash
docker-compose down -v --rmi all --remove-orphans && docker-compose up -d
```

### 2. Regular Start

For subsequent runs after the initial setup:

```bash
docker-compose up -d
```

### 3. Stop Services

```bash
docker-compose down
```

## Services & Ports

| Service | Port | Description |
|---------|------|-------------|
| Kafka | 9092 | Kafka broker |
| Kafka UI | 8080 | Web interface for Kafka management |
| MongoDB | 27017 | MongoDB database |
| Mongo Express | 8081 | Web interface for MongoDB |

## Access Web Interfaces

- **Kafka UI**: http://localhost:8080
- **Mongo Express**: http://localhost:8081

## Kafka Topics

The following topics are automatically created:

- `transaction_generator` - Raw transaction events (3 partitions)
- `transaction_alert` - Alert notifications (3 partitions)

## Development

### Building the Services

```bash
mvn clean install
```

### Running Individual Services Locally

Each service can be run locally for development. Make sure the infrastructure (Kafka, MongoDB) is running via Docker Compose first.

## Health Checks

All services include health checks. You can monitor the status using:

```bash
docker-compose ps
```

## Troubleshooting

### View Logs

```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f kafka
docker-compose logs -f mongodb
```

### Reset Everything

If you encounter issues, perform a complete reset:

```bash
docker-compose down -v --rmi all --remove-orphans
docker system prune -f
docker-compose up -d
```

### Common Issues

- **Port conflicts**: Ensure ports 8080, 8081, 9092, and 27017 are available
- **Kafka startup**: Kafka may take 1-2 minutes to fully initialize
- **MongoDB connection**: Wait for MongoDB health check to pass before connecting

## Configuration

- Kafka is configured in KRaft mode (no Zookeeper required)
- MongoDB runs without authentication by default
- Auto-topic creation is disabled - topics are created explicitly via setup service