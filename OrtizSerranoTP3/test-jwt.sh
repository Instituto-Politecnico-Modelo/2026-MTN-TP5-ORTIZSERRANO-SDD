#!/bin/bash

echo "🔐 Prueba de Autenticación JWT - Sistema Decanato"
echo "=================================================="
echo ""

BASE_URL="http://localhost:8080/api/auth"

# Colores
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}1. Registrando nuevo usuario...${NC}"
SIGNUP_RESPONSE=$(curl -s -X POST $BASE_URL/signup \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Test",
    "apellido": "Usuario",
    "email": "test@example.com",
    "password": "password123"
  }')

echo "$SIGNUP_RESPONSE"
echo ""

echo -e "${BLUE}2. Haciendo login...${NC}"
LOGIN_RESPONSE=$(curl -s -X POST $BASE_URL/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123"
  }')

echo "$LOGIN_RESPONSE"
echo ""

# Extraer token
TOKEN=$(echo $LOGIN_RESPONSE | grep -o '"token":"[^"]*' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
  echo -e "${RED}❌ Error: No se pudo obtener el token${NC}"
  exit 1
fi

echo -e "${GREEN}✅ Token JWT obtenido exitosamente${NC}"
echo "Token: ${TOKEN:0:50}..."
echo ""

echo -e "${BLUE}3. Obteniendo información del usuario autenticado...${NC}"
ME_RESPONSE=$(curl -s -X GET $BASE_URL/me \
  -H "Authorization: Bearer $TOKEN")

echo "$ME_RESPONSE"
echo ""

echo -e "${GREEN}✅ Prueba completada exitosamente!${NC}"
echo ""
echo "Puedes usar este token para autenticar tus peticiones:"
echo "Authorization: Bearer $TOKEN"
