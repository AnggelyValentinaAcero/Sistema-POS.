from datetime import datetime, timedelta, timezone

import jwt
from flask import Blueprint, jsonify, request

from app.config import JWT_SECRET_KEY
from autentificacion.decorators import requiere_token

authentication_bp = Blueprint("authentication", __name__)


@authentication_bp.post("/api/auth/login")
def iniciar_sesion():
    data = request.get_json() or {}
    #aca se hara consulta a la base de datos para verificar las credenciales del usuario
    username = data.get("username")
    password = data.get("password")

    if username == "admin" and password == "password":
        datos_token = {
            "usuario": username,
            #aca se puede agregar el nivel de acceso del usuario, roles, permisos, etc.
            "exp": datetime.now(timezone.utc) + timedelta(minutes=30),
        }

        token = jwt.encode(
            datos_token,
            JWT_SECRET_KEY,
            algorithm="HS256",
        )

        return jsonify({
            "mensaje": "Inicio de sesión exitoso",
            "token": token,
        }), 200

    return jsonify({
        "mensaje": "Credenciales inválidas",
    }), 401


@authentication_bp.get("/api/auth/me")
@requiere_token
def obtener_usuario(datos_usuario):
    return jsonify({"usuario": datos_usuario["usuario"]}), 200