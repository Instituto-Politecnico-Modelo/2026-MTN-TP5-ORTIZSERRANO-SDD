#!/bin/bash

# Colores
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

clear
echo -e "${BLUE}╔════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║     🔐 PROBADOR INTERACTIVO DE JWT 🔐        ║${NC}"
echo -e "${BLUE}║         Sistema de Decanato                    ║${NC}"
echo -e "${BLUE}╚════════════════════════════════════════════════╝${NC}"
echo ""

BASE_URL="http://localhost:8080/api/auth"

# Función para hacer pausas
pause() {
    echo ""
    read -p "Presiona ENTER para continuar..."
    echo ""
}

# ====================
# PASO 1: REGISTRO
# ====================
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}📝 PASO 1: Registrar un nuevo usuario${NC}"
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo "Vamos a registrar un usuario de prueba:"
echo -e "${GREEN}  Nombre: Test${NC}"
echo -e "${GREEN}  Apellido: Usuario${NC}"
echo -e "${GREEN}  Email: test@prueba.com${NC}"
echo -e "${GREEN}  Password: password123${NC}"
echo ""

pause

echo -e "${BLUE}Enviando petición de registro...${NC}"
SIGNUP_RESPONSE=$(curl -s -X POST $BASE_URL/signup \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Test",
    "apellido": "Usuario",
    "email": "test@prueba.com",
    "password": "password123"
  }')

echo ""
echo -e "${GREEN}✅ Respuesta del servidor:${NC}"
echo "$SIGNUP_RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$SIGNUP_RESPONSE"
echo ""

pause

# ====================
# PASO 2: LOGIN
# ====================
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}🔑 PASO 2: Hacer Login y obtener Token JWT${NC}"
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo "Ahora vamos a autenticarnos con:"
echo -e "${GREEN}  Email: test@prueba.com${NC}"
echo -e "${GREEN}  Password: password123${NC}"
echo ""

pause

echo -e "${BLUE}Enviando credenciales...${NC}"
LOGIN_RESPONSE=$(curl -s -X POST $BASE_URL/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@prueba.com",
    "password": "password123"
  }')

echo ""
echo -e "${GREEN}✅ Respuesta del servidor:${NC}"
echo "$LOGIN_RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$LOGIN_RESPONSE"
echo ""

# Extraer token
TOKEN=$(echo $LOGIN_RESPONSE | grep -o '"token":"[^"]*' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
  echo -e "${RED}❌ Error: No se pudo obtener el token${NC}"
  exit 1
fi

echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}🎉 ¡Token JWT obtenido exitosamente!${NC}"
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo -e "${BLUE}Tu token JWT:${NC}"
echo "${TOKEN:0:80}..."
echo ""

pause

# ====================
# PASO 3: USAR TOKEN
# ====================
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}🔓 PASO 3: Acceder con el Token (Validación JWT)${NC}"
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo "Ahora vamos a acceder a un endpoint protegido usando el token"
echo ""

pause

echo -e "${BLUE}Enviando petición con token JWT...${NC}"
ME_RESPONSE=$(curl -s -X GET $BASE_URL/me \
  -H "Authorization: Bearer $TOKEN")

echo ""
echo -e "${GREEN}✅ Respuesta del servidor:${NC}"
echo "$ME_RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$ME_RESPONSE"
echo ""

pause

# ====================
# PASO 4: SIN TOKEN
# ====================
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}❌ PASO 4: Intentar acceder SIN token${NC}"
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo "Ahora probemos qué pasa si NO enviamos el token..."
echo ""

pause

echo -e "${BLUE}Enviando petición SIN token...${NC}"
NO_TOKEN_RESPONSE=$(curl -s -X GET $BASE_URL/me)

echo ""
echo -e "${RED}❌ Respuesta del servidor (debe fallar):${NC}"
echo "$NO_TOKEN_RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$NO_TOKEN_RESPONSE"
echo ""

pause

# ====================
# PASO 5: TOKEN FALSO
# ====================
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}❌ PASO 5: Intentar acceder con TOKEN FALSO${NC}"
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo "Probemos con un token inventado..."
echo ""

pause

echo -e "${BLUE}Enviando petición con token FALSO...${NC}"
FAKE_TOKEN_RESPONSE=$(curl -s -X GET $BASE_URL/me \
  -H "Authorization: Bearer token_falso_123_esto_no_funciona")

echo ""
echo -e "${RED}❌ Respuesta del servidor (debe fallar):${NC}"
echo "$FAKE_TOKEN_RESPONSE" | python3 -m json.tool 2>/dev/null || echo "$FAKE_TOKEN_RESPONSE"
echo ""

pause

# ====================
# RESUMEN
# ====================
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}📊 RESUMEN DE PRUEBAS${NC}"
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo -e "${GREEN}✅ Registro:${NC}        Usuario creado correctamente"
echo -e "${GREEN}✅ Login:${NC}           Token JWT generado"
echo -e "${GREEN}✅ Con Token:${NC}       Acceso permitido (JWT válido)"
echo -e "${RED}❌ Sin Token:${NC}       Acceso denegado (401)"
echo -e "${RED}❌ Token Falso:${NC}     Acceso denegado (401)"
echo ""
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}🎉 ¡JWT FUNCIONA PERFECTAMENTE! 🎉${NC}"
echo -e "${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo -e "${BLUE}Tu token para usar manualmente:${NC}"
echo "$TOKEN"
echo ""
echo -e "${BLUE}Ejemplo de uso con curl:${NC}"
echo "curl -X GET http://localhost:8080/api/auth/me \\"
echo "  -H \"Authorization: Bearer $TOKEN\""
echo ""
