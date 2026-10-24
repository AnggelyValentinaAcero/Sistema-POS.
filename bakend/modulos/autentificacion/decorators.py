from functools import wraps

import jwt
from flask import jsonify, request

from app.config import JWT_SECRET_KEY


def requiere_token(funcion):
    @wraps(funcion)
    def decorador(*args, **kwargs):
        encabezado = request.headers.get("Authorization")

        if not encabezado:
            return jsonify({
                "mensaje": "Falta el encabezado Authorization"
            }), 401

        partes = encabezado.split()

        if len(partes) != 2 or partes[0] != "Bearer":
            return jsonify({
                "mensaje": "Formato de token inválido"
            }), 401

        token = partes[1]

        try:
            datos_usuario = jwt.decode(
                token,
                JWT_SECRET_KEY,
                algorithms=["HS256"]
            )
        except jwt.ExpiredSignatureError:
            return jsonify({
                "mensaje": "El token ha expirado"
            }), 401
        except jwt.InvalidTokenError:
            return jsonify({
                "mensaje": "El token no es válido"
            }), 401

        return funcion(datos_usuario, *args, **kwargs)

    return decorador