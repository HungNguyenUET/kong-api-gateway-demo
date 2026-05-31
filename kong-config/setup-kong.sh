#!/bin/bash
# Kong Admin API base URL
KONG_ADMIN="http://localhost:8001"

echo "========================================"
echo " Kong Gateway Configuration Script"
echo "========================================"

# -----------------------------------------------
# 1. Register product-service
# -----------------------------------------------
echo ""
echo "[1/4] Creating Kong Service: product-service"
curl -s -X POST "$KONG_ADMIN/services" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "product-service",
    "url": "http://product-service:8081/products"
  }' | python3 -m json.tool

# -----------------------------------------------
# 2. Create Route for product-service
#    Strips /api/v1 prefix before forwarding
# -----------------------------------------------
echo ""
echo "[2/4] Creating Route for product-service: /api/v1/products"
curl -s -X POST "$KONG_ADMIN/services/product-service/routes" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "product-route",
    "paths": ["/api/v1/products"],
    "strip_path": true,
    "methods": ["GET"]
  }' | python3 -m json.tool

# -----------------------------------------------
# 3. Register order-service
# -----------------------------------------------
echo ""
echo "[3/4] Creating Kong Service: order-service"
curl -s -X POST "$KONG_ADMIN/services" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "order-service",
    "url": "http://order-service:8082/orders"
  }' | python3 -m json.tool

# -----------------------------------------------
# 4. Create Route for order-service
# -----------------------------------------------
echo ""
echo "[4/4] Creating Route for order-service: /api/v1/orders"
curl -s -X POST "$KONG_ADMIN/services/order-service/routes" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "order-route",
    "paths": ["/api/v1/orders"],
    "strip_path": true,
    "methods": ["GET", "POST"]
  }' | python3 -m json.tool

echo ""
echo "========================================"
echo " Done! Verify with:"
echo "  curl http://localhost:8001/services"
echo "  curl http://localhost:8001/routes"
echo "========================================"
