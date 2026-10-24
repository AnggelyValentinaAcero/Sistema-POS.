from flask import Flask, jsonify
from flask_cors import CORS
from autentificacion.routes import authentication_bp

app = Flask(__name__)
CORS(app)
app.register_blueprint(authentication_bp)

@app.get("/")
def inicio():
    return jsonify({"mensaje": "API de inventarios funcionando"}), 200

@app.get("/api/health")
def estado():
    return jsonify({
        "status": "ok",
        "mensaje": "La API esta disponible"
    }), 200

if __name__ == "__main__":
    app.run(debug=True)