#!/bin/bash

# Script para probar operaciones CRUD de usuarios

echo "==================================="
echo "PRUEBA CRUD DE USUARIOS CON JWT"
echo "==================================="
echo ""

BASE_URL="http://localhost:8080/api/auth"

# Colores
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}1. REGISTRAR USUARIO${NC}"
echo "POST ${BASE_URL}/signup"
SIGNUP_RESPONSE=$(curl -s -X POST ${BASE_URL}/signup \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Carlos",
    "apellido": "Gómez",
    "email": "carlos.gomez@test.com",
    "password": "password123"
  }')
echo "$SIGNUP_RESPONSE" | jq .
echo ""

echo -e "${BLUE}2. LOGIN - OBTENER TOKEN JWT${NC}"
echo "POST ${BASE_URL}/login"
LOGIN_RESPONSE=$(curl -s -X POST ${BASE_URL}/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "carlos.gomez@test.com",
    "password": "password123"
  }')
echo "$LOGIN_RESPONSE" | jq .

# Extraer el token
TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.accessToken')
echo -e "\n${GREEN}Token JWT guardado: ${TOKEN:0:50}...${NC}"
echo ""

echo -e "${BLUE}3. OBTENER INFO DEL USUARIO AUTENTICADO${NC}"
echo "GET ${BASE_URL}/me"
curl -s -X GET ${BASE_URL}/me \
  -H "Authorization: Bearer $TOKEN" | jq .
echo ""

echo -e "${BLUE}4. ACTUALIZAR INFORMACIÓN DEL USUARIO${NC}"
echo "PUT ${BASE_URL}/update"
curl -s -X PUT ${BASE_URL}/update \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "nombre": "Carlos Alberto",
    "apellido": "Gómez Pérez"
  }' | jq .
echo ""

echo -e "${BLUE}5. VERIFICAR ACTUALIZACIÓN${NC}"
echo "GET ${BASE_URL}/me"
curl -s -X GET ${BASE_URL}/me \
  -H "Authorization: Bearer $TOKEN" | jq .
echo ""

echo -e "${BLUE}6. BAJA LÓGICA - Desactivar usuario${NC}"
echo "DELETE ${BASE_URL}/delete"
curl -s -X DELETE ${BASE_URL}/delete \
  -H "Authorization: Bearer $TOKEN" | jq .
echo ""

echo -e "${BLUE}7. INTENTAR LOGIN CON USUARIO INACTIVO${NC}"
echo "POST ${BASE_URL}/login"
LOGIN_INACTIVE=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X POST ${BASE_URL}/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "carlos.gomez@test.com",
    "password": "password123"
  }')
echo "$LOGIN_INACTIVE"
echo ""

echo -e "${GREEN}==================================="
echo "PRUEBA FINALIZADA"
echo "===================================${NC}"
echo ""
echo "NOTA: Para probar la baja física, primero debes reactivar el usuario"
echo "o crear uno nuevo, ya que un usuario inactivo no puede hacer login."
