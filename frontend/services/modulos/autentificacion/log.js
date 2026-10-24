const API_URL = "http://127.0.0.1:5000";

const tarjeta = document.getElementById("auth-card");
const loginView = document.getElementById("login-view");
const registroView = document.getElementById("registro-view");
const registroInvitacion = document.getElementById("registro-invitacion");
const loginInvitacion = document.getElementById("login-invitacion");
const loginForm = document.getElementById("login-form");
const loginButton = document.getElementById("login-button");
const loginMessage = document.getElementById("login-message");
const registroForm = document.getElementById("registrar-form");
const registroMessage = document.getElementById("registro-message");

function mostrarMensaje(elemento, mensaje, estado) {
    elemento.textContent = mensaje;
    elemento.dataset.state = estado;
}

document.getElementById("mostrar-registro").addEventListener("click", () => {
    tarjeta.classList.add("modo-registro");
    loginView.hidden = true;
    registroView.hidden = false;
    registroInvitacion.hidden = true;
    loginInvitacion.hidden = false;
    loginMessage.textContent = "";
});

document.getElementById("mostrar-login").addEventListener("click", () => {
    tarjeta.classList.remove("modo-registro");
    registroView.hidden = true;
    loginView.hidden = false;
    loginInvitacion.hidden = true;
    registroInvitacion.hidden = false;
    registroMessage.textContent = "";
});

loginForm.addEventListener("submit", async (evento) => {
    evento.preventDefault();
    loginButton.disabled = true;
    loginButton.textContent = "Iniciando sesión...";
    mostrarMensaje(loginMessage, "", "");

    try {
        const respuesta = await fetch(`${API_URL}/api/auth/login`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify({
                username: loginForm.elements.username.value.trim(),
                password: loginForm.elements.password.value,
            }),
        });

        let resultado;
        try {
            resultado = await respuesta.json();
        } catch {
            throw new Error("El backend devolvió una respuesta no válida.");
        }

        if (!respuesta.ok) {
            mostrarMensaje(
                loginMessage,
                resultado.mensaje || `Error al iniciar sesión (${respuesta.status}).`,
                "error",
            );
            return;
        }

        if (!resultado.token) {
            throw new Error("El backend no devolvió un token de autenticación.");
        }

        try {
            sessionStorage.setItem("authToken", resultado.token);
        } catch {
            throw new Error("No se pudo guardar la sesión en este navegador.");
        }

        window.location.assign("../inventario/home.html");
    } catch (error) {
        console.error("Error al iniciar sesión:", error);
        mostrarMensaje(
            loginMessage,
            error instanceof TypeError
                ? "No se pudo conectar con el backend. Comprueba que esté ejecutándose en http://127.0.0.1:5000."
                : error.message,
            "error",
        );
    } finally {
        loginButton.disabled = false;
        loginButton.textContent = "Iniciar sesión";
    }
});

registroForm.addEventListener("submit", (evento) => {
    evento.preventDefault();
    mostrarMensaje(
        registroMessage,
        "El registro todavía no está habilitado en el backend.",
        "error",
    );
});