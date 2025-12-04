# Energy Management System

This project is a microservices-based Energy Management System orchestrated using Docker Compose. It includes services for user management, device management, and energy consumption monitoring.

## Prerequisites

- [Docker](https://www.docker.com/get-started) installed on your machine.
- [Docker Compose](https://docs.docker.com/compose/install/) (usually included with Docker Desktop).

## Getting Started

### 1. Start the Application

To start all services, run the following command in the root directory of the project:

```bash
docker-compose up -d --build
```

This command will:
- Build the images for `frontend`, `auth-service`, `user-service`, `device-service`, and `monitoring-service`.
- Start the databases (`user_db`, `device_db`, `credential_db`, `monitoring_db`).
- Start `rabbitmq` for message brokering.
- Start `traefik` as the reverse proxy.

### 2. Access the Application

Once the containers are running, you can access the application at:

- **Frontend**: [http://localhost](http://localhost)
- **Traefik Dashboard**: [http://localhost:8080](http://localhost:8080) (for monitoring routes and services)

## Architecture Overview

The system consists of the following microservices:

- **Frontend**: A static web application served via Nginx (or similar) and routed through Traefik.
- **Auth Service**: Handles user authentication and authorization.
- **User Service**: Manages user data.
- **Device Service**: Manages smart devices and their mapping to users.
- **Monitoring Service**: Collects and visualizes energy consumption data.
- **Simulator**: A Python script (if applicable) to simulate device energy consumption.

## Useful Tips

### Stopping the Application

To stop all running services:

```bash
docker-compose down
```

To stop and remove volumes (WARNING: this will delete database data):

```bash
docker-compose down -v
```

### Viewing Logs

To view logs for a specific service (e.g., `monitoring-service`):

```bash
docker logs -f monitoring-service
```

### Database Access

The databases are exposed on the following ports for local debugging:
- **User DB**: 5432 (mapped to host port 1000)
- **Device DB**: 5432 (mapped to host port 1001)
- **Credential DB**: 5432 (mapped to host port 1002)
- **Monitoring DB**: 5432 (mapped to host port 1003)

### RabbitMQ Management

Access the RabbitMQ Management Interface at:
- URL: [http://localhost:15672](http://localhost:15672)
- Default Credentials: `kalo` / `kalo` (as defined in `docker-compose.yml`)

## Troubleshooting

- **Port Conflicts**: Ensure ports `80`, `8080`, `5672`, `15672`, and the DB ports (`1000`-`1003`) are not in use by other applications.
- **Service Startup**: If a service fails to start immediately, it might be waiting for its database or RabbitMQ. The services are configured to restart on failure or have retry logic, so give them a moment.
