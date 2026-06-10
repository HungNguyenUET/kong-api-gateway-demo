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
echo "[1/10] Creating Kong Service: product-service"
curl -s -X POST "$KONG_ADMIN/services" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "product-service",
    "url": "http://product-service:8081/products"
  }' | py -m json.tool

# -----------------------------------------------
# 2. Create Route for product-service
#    Strips /api/v1 prefix before forwarding
# -----------------------------------------------
echo ""
echo "[2/10] Creating Route for product-service: /api/v1/products"
curl -s -X POST "$KONG_ADMIN/services/product-service/routes" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "product-route",
    "paths": ["/api/v1/products"],
    "strip_path": true,
    "methods": ["GET"]
  }' | py -m json.tool

# -----------------------------------------------
# 3. Register order-service
# -----------------------------------------------
echo ""
echo "[3/10] Creating Kong Service: order-service"
curl -s -X POST "$KONG_ADMIN/services" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "order-service",
    "url": "http://order-service:8082/orders"
  }' | py -m json.tool

# -----------------------------------------------
# 4. Create Route for order-service
# -----------------------------------------------
echo ""
echo "[4/10] Creating Route for order-service: /api/v1/orders"
curl -s -X POST "$KONG_ADMIN/services/order-service/routes" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "order-route",
    "paths": ["/api/v1/orders"],
    "strip_path": true,
    "methods": ["GET", "POST"]
  }' | py -m json.tool

# -----------------------------------------------
# 5. Register auth-service
#    URL ends with /auth so that strip_path correctly routes:
#    /api/v1/auth/login -> strip /api/v1/auth -> /login
#    -> http://auth-service:8083/auth/login (matches controller)
# -----------------------------------------------
echo ""
echo "[5/10] Creating Kong Service: auth-service"
curl -s -X POST "$KONG_ADMIN/services" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "auth-service",
    "url": "http://auth-service:8083/auth"
  }' | py -m json.tool

# -----------------------------------------------
# 6. Create Route for auth-service (PUBLIC - no JWT plugin)
# -----------------------------------------------
echo ""
echo "[6/10] Creating Route for auth-service: /api/v1/auth"
curl -s -X POST "$KONG_ADMIN/services/auth-service/routes" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "auth-route",
    "paths": ["/api/v1/auth"],
    "strip_path": true,
    "methods": ["GET", "POST"]
  }' | py -m json.tool

# -----------------------------------------------
# 7. Create Kong Consumer for JWT validation
# -----------------------------------------------
echo ""
echo "[7/10] Creating Kong Consumer: keycloak-consumer"
curl -s -X POST "$KONG_ADMIN/consumers" \
  -H "Content-Type: application/json" \
  -d '{
    "username": "keycloak-consumer"
  }' | py -m json.tool

# -----------------------------------------------
# 8. Fetch Keycloak RS256 public key and register as JWT credential
#    Uses Python to handle PEM newlines in JSON payload
#    The 'key' field must exactly match the 'iss' claim in Keycloak tokens
# -----------------------------------------------
echo ""
echo "[8/10] Fetching Keycloak public key and registering JWT credential..."
echo "  Waiting for Keycloak realm endpoint to be ready..."

until curl -sf "http://localhost:8080/realms/demo" > /dev/null; do
  echo "  Keycloak not ready yet, retrying in 5s..."
  sleep 5
done

py - <<'PYEOF'
import json, urllib.request, textwrap, sys

try:
    with urllib.request.urlopen("http://localhost:8080/realms/demo") as r:
        realm_info = json.loads(r.read())
except Exception as e:
    print(f"ERROR: Could not fetch Keycloak realm info: {e}", file=sys.stderr)
    sys.exit(1)

issuer = realm_info["issuer"]
pem = (
    "-----BEGIN PUBLIC KEY-----\n"
    + "\n".join(textwrap.wrap(realm_info["public_key"], 64))
    + "\n-----END PUBLIC KEY-----"
)

print(f"  Keycloak issuer (will be used as Kong JWT credential key): {issuer}")

payload = json.dumps({
    "key": issuer,
    "algorithm": "RS256",
    "rsa_public_key": pem
}).encode("utf-8")

req = urllib.request.Request(
    "http://localhost:8001/consumers/keycloak-consumer/jwt",
    data=payload,
    headers={"Content-Type": "application/json"},
    method="POST"
)

try:
    with urllib.request.urlopen(req) as r:
        print(json.dumps(json.loads(r.read()), indent=2))
except urllib.error.HTTPError as e:
    body = e.read().decode("utf-8")
    print(f"ERROR: Kong API returned {e.code}: {body}", file=sys.stderr)
    sys.exit(1)
PYEOF

# -----------------------------------------------
# 9. Enable JWT plugin on product-route
#    key_claim_name: "iss" -> Kong looks up consumer by iss claim value
#    claims_to_verify: ["exp"] -> validates token not expired
# -----------------------------------------------
echo ""
echo "[9/10] Enabling JWT plugin on product-route"
curl -s -X POST "$KONG_ADMIN/routes/product-route/plugins" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "jwt",
    "config": {
      "key_claim_name": "iss",
      "claims_to_verify": ["exp"]
    }
  }' | py -m json.tool

# -----------------------------------------------
# 10. Enable JWT plugin on order-route
# -----------------------------------------------
echo ""
echo "[10/10] Enabling JWT plugin on order-route"
curl -s -X POST "$KONG_ADMIN/routes/order-route/plugins" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "jwt",
    "config": {
      "key_claim_name": "iss",
      "claims_to_verify": ["exp"]
    }
  }' | py -m json.tool

echo ""
echo "========================================"
echo " Done! JWT authentication configured."
echo ""
echo " Public routes (no auth required):"
echo "   POST http://localhost:8000/api/v1/auth/login"
echo "   POST http://localhost:8000/api/v1/auth/register"
echo ""
echo " Protected routes (Bearer JWT required):"
echo "   GET  http://localhost:8000/api/v1/products"
echo "   GET  http://localhost:8000/api/v1/orders"
echo "   POST http://localhost:8000/api/v1/orders"
echo ""
echo " Verify with:"
echo "   curl http://localhost:8001/consumers/keycloak-consumer/jwt"
echo "   curl http://localhost:8001/routes/product-route/plugins"
echo "========================================"
