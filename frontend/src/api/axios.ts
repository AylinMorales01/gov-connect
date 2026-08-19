import axios from 'axios';

export const api = axios.create({
    baseURL: import.meta.env.VITE_API_URL,
    timeout: 10000,
    // Envía automáticamente las cookies HttpOnly (access_token / refresh_token).
    withCredentials: true,
    headers: {
        'Content-Type': 'application/json',
    },
});

// ── Refresh token queue ──────────────────────────────────
// Evita múltiples llamadas simultáneas a /auth/refresh
// cuando varias requests expiran al mismo tiempo.

let isRefreshing = false;
let failedQueue: Array<{
    resolve: () => void;
    reject: (error: unknown) => void;
}> = [];

const processQueue = (error: unknown) => {
    failedQueue.forEach((prom) => {
        if (error) {
            prom.reject(error);
        } else {
            prom.resolve();
        }
    });
    failedQueue = [];
};

// Callback que AuthContext registra para actualizar su estado
// cuando el interceptor renueva el token automáticamente.
let onTokenRefreshed: ((role: string, expiresIn: number) => void) | null = null;

export function setOnTokenRefreshed(cb: typeof onTokenRefreshed) {
    onTokenRefreshed = cb;
}

// Callback que AuthContext registra para limpiar el estado de sesión
// cuando el refresh falla (token expirado o revocado). Al limpiar el
// estado, el ProtectedRoute redirige a /login vía React Router, sin
// recargar la página.
let onSessionExpired: (() => void) | null = null;

export function setOnSessionExpired(cb: typeof onSessionExpired) {
    onSessionExpired = cb;
}

// ── Response interceptor: manejar 401 con refresh ──
api.interceptors.response.use(
    (response) => response,
    async (error) => {
        const originalRequest = error.config;

        // Solo interceptamos 401
        if (error.response?.status !== 401) {
            return Promise.reject(error);
        }

        // No reintentar los endpoints de auth (evitar loops infinitos)
        if (
            originalRequest.url === '/auth/refresh' ||
            originalRequest.url === '/auth/login' ||
            originalRequest.url === '/auth/me'
        ) {
            return Promise.reject(error);
        }

        // Evitar reintentos múltiples sobre la misma request original
        if (originalRequest._retry) {
            return Promise.reject(error);
        }

        // Si ya se está refrescando, encolar esta request
        if (isRefreshing) {
            return new Promise<void>((resolve, reject) => {
                failedQueue.push({ resolve, reject });
            }).then(() => api(originalRequest));
        }

        originalRequest._retry = true;
        isRefreshing = true;

        try {
            // POST /auth/refresh — la cookie HttpOnly se envía automáticamente
            const response = await api.post<{ data: { role: string; expiresIn: number } }>(
                '/auth/refresh'
            );
            const { role, expiresIn } = response.data.data;

            // Notificar a AuthContext para que actualice su estado
            if (onTokenRefreshed) {
                onTokenRefreshed(role, expiresIn);
            }

            // Reintentar todas las requests encoladas
            processQueue(null);

            // Reintentar la request original (la cookie ya se renovó)
            return api(originalRequest);
        } catch (refreshError) {
            // El refresh falló: token expirado, revocado o usuario inactivo
            processQueue(refreshError);

            // Notificar a AuthContext para limpiar el estado de sesión.
            if (onSessionExpired) {
                onSessionExpired();
            } else {
                // Fallback si el callback aún no fue registrado (p. ej. antes
                // de montar el provider): redirigir recargando.
                if (window.location.pathname !== '/login') {
                    window.location.href = '/login';
                }
            }

            return Promise.reject(refreshError);
        } finally {
            isRefreshing = false;
        }
    }
);
