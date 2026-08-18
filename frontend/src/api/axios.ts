import axios from 'axios';

export const api = axios.create({
    baseURL: import.meta.env.VITE_API_URL,
    timeout: 10000,
    headers: {
        'Content-Type': 'application/json',
    },
});

// ── Request interceptor: adjuntar access token ──
api.interceptors.request.use((config) => {
    const token = localStorage.getItem('govconnect_token');
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

// ── Refresh token queue ──────────────────────────────────
// Evita múltiples llamadas simultáneas a /auth/refresh
// cuando varias requests expiran al mismo tiempo.

let isRefreshing = false;
let failedQueue: Array<{
    resolve: (token: string) => void;
    reject: (error: unknown) => void;
}> = [];

const processQueue = (error: unknown, token: string | null) => {
    failedQueue.forEach((prom) => {
        if (error) {
            prom.reject(error);
        } else if (token) {
            prom.resolve(token);
        }
    });
    failedQueue = [];
};

// Callback que AuthContext registra para actualizarse cuando
// el interceptor renueva el token automáticamente.
let onTokenRefreshed: ((token: string, role: string) => void) | null = null;

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

        // No reintentar el propio endpoint de refresh (evitar loop infinito)
        if (originalRequest.url === '/auth/refresh') {
            return Promise.reject(error);
        }

        // No reintentar el endpoint de login
        if (originalRequest.url === '/auth/login') {
            return Promise.reject(error);
        }

        // Evitar reintentos múltiples sobre la misma request original
        if (originalRequest._retry) {
            return Promise.reject(error);
        }

        // Si ya se está refrescando, encolar esta request
        if (isRefreshing) {
            return new Promise<string>((resolve, reject) => {
                failedQueue.push({ resolve, reject });
            }).then((newToken) => {
                originalRequest.headers.Authorization = `Bearer ${newToken}`;
                return api(originalRequest);
            });
        }

        originalRequest._retry = true;
        isRefreshing = true;

        try {
            // POST /auth/refresh — la cookie HttpOnly se envía automáticamente
            const response = await api.post<{ data: { token: string; role: string; expiresIn: number } }>(
                '/auth/refresh'
            );
            const { token, role } = response.data.data;

            // Actualizar localStorage
            localStorage.setItem('govconnect_token', token);
            localStorage.setItem('govconnect_role', role);

            // Notificar a AuthContext para que actualice su estado
            if (onTokenRefreshed) {
                onTokenRefreshed(token, role);
            }

            // Reintentar todas las requests encoladas
            processQueue(null, token);

            // Reintentar la request original
            originalRequest.headers.Authorization = `Bearer ${token}`;
            return api(originalRequest);
        } catch (refreshError) {
            // El refresh falló: token expirado, revocado o usuario inactivo
            processQueue(refreshError, null);

            // Notificar a AuthContext para limpiar el estado de sesión.
            // Al quedar desautenticado, ProtectedRoute redirige a /login
            // vía React Router (sin recarga completa de la página).
            if (onSessionExpired) {
                onSessionExpired();
            } else {
                // Fallback si el callback aún no fue registrado (p. ej. antes
                // de montar el provider): limpiar y recargar.
                localStorage.removeItem('govconnect_token');
                localStorage.removeItem('govconnect_username');
                localStorage.removeItem('govconnect_role');
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
