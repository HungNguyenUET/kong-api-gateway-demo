# Kong API Gateway Setup with Docker Compose

## Quick Start

### 1. Start Kong Services

```powershell
docker-compose up -d
```

This will start:
- **PostgreSQL** (Kong database)
- **Kong** (API Gateway)
- **Konga** (Admin Dashboard - optional)

### 2. Verify Services are Running

```powershell
docker-compose ps
```

### 3. Access Kong Admin UI

Open browser: `http://localhost:1337`

- **Kong Admin API**: `http://localhost:8001`
- **Kong Proxy**: `http://localhost:8000`

---

## Common Docker Compose Commands

```powershell
# Start services
docker-compose up -d

# Stop services
docker-compose down

# View logs
docker-compose logs -f kong

# Restart Kong
docker-compose restart kong

# Remove all data (clean slate)
docker-compose down -v
```

---

## Setup Konga (First Time)

When you first access Konga at `http://localhost:1337`:

1. Create admin account
2. Add new connection to Kong Admin API:
   - **Name**: Kong Local
   - **Kong Admin URL**: `http://kong:8001`
   - Click **Create Connection**

---

## Configure Your First Service & Route

### Option 1: Using Konga UI
1. Go to Services → Add Service
2. Name: `example-api`
3. URL: `https://httpbin.org`
4. Create Route:
   - Paths: `/api`
   - Methods: GET, POST

### Option 2: Using API Calls

```powershell
# Create Service
curl -X POST http://localhost:8001/services `
  -H "Content-Type: application/json" `
  -d '{
    "name": "example-api",
    "url": "https://httpbin.org"
  }'

# Create Route
curl -X POST http://localhost:8001/services/example-api/routes `
  -H "Content-Type: application/json" `
  -d '{
    "paths": ["/api"],
    "methods": ["GET", "POST"]
  }'

# Test Route
curl http://localhost:8000/api/get
```

---

## Add Authentication Plugin

```powershell
# Add Key Auth to Service
curl -X POST http://localhost:8001/services/example-api/plugins `
  -H "Content-Type: application/json" `
  -d '{
    "name": "key-auth"
  }'

# Create Consumer (User)
curl -X POST http://localhost:8001/consumers `
  -H "Content-Type: application/json" `
  -d '{
    "username": "user1"
  }'

# Create API Key for Consumer
curl -X POST http://localhost:8001/consumers/user1/key-auth `
  -H "Content-Type: application/json" `
  -d '{
    "key": "my-secret-key"
  }'

# Test with API Key
curl http://localhost:8000/api/get `
  -H "apikey: my-secret-key"
```

---

## Add Rate Limiting Plugin

```powershell
curl -X POST http://localhost:8001/services/example-api/plugins `
  -H "Content-Type: application/json" `
  -d '{
    "name": "rate-limiting",
    "config": {
      "minute": 10,
      "hour": 100
    }
  }'
```

---

## Kong Ports Overview

| Service | Port | Purpose |
|---------|------|---------|
| Kong Proxy | 8000 | HTTP requests from clients |
| Kong Proxy SSL | 8443 | HTTPS requests from clients |
| Kong Admin | 8001 | Admin API management |
| Kong Admin SSL | 8444 | Admin API over HTTPS |
| PostgreSQL | 5432 | Database |
| Konga UI | 1337 | Admin Dashboard |

---

## Check Kong Status

```powershell
# Check Kong health
curl http://localhost:8001/status

# List all services
curl http://localhost:8001/services

# List all routes
curl http://localhost:8001/routes

# List all plugins
curl http://localhost:8001/plugins
```

---

## Troubleshooting

### Kong fails to start
```powershell
# Check logs
docker-compose logs kong

# Ensure database is ready
docker-compose logs kong-database
```

### Reset Everything
```powershell
docker-compose down -v
docker-compose up -d
```

### Database connection issue
```powershell
# Restart database
docker-compose restart kong-database
docker-compose restart kong
```

---

## Next Steps

1. ✅ Learn Services & Routes
2. ✅ Add Authentication (Key-Auth, OAuth, etc.)
3. ✅ Configure Rate Limiting
4. ✅ Add request/response transformations
5. ✅ Set up logging and monitoring

**Kong Plugin Marketplace**: https://docs.konghq.com/hub/
