import { api } from './axios';

/**
 * Respuesta del endpoint POST /api/v1/auth/login.
 * Coincide con AuthResponse.java del backend.
 */
export interface AuthResponse {
    token: string;
    tokenType: string;
    expiresIn: number;
    role: string;
}

/**
 * Envía credenciales al backend y retorna el JWT con metadatos.
 * El refresh token se recibe automáticamente como cookie HttpOnly.
 *
 * @throws AxiosError con status 401 si las credenciales son inválidas.
 */
export const loginRequest = async (
    username: string,
    password: string
): Promise<AuthResponse> => {
    const response = await api.post<{ data: AuthResponse }>(
        '/auth/login',
        { username, password }
    );
    return response.data.data;
};

/**
 * Renueva el access token usando el refresh token de la cookie HttpOnly.
 * El navegador envía la cookie automáticamente en peticiones same-site.
 *
 * @returns nuevos access token, expiresIn y role.
 * @throws AxiosError con status 401 si el refresh token es inválido/expirado.
 */
export const refreshTokenRequest = async (): Promise<AuthResponse> => {
    const response = await api.post<{ data: AuthResponse }>('/auth/refresh');
    return response.data.data;
};

/**
 * Cierra la sesión en el servidor (invalida el refresh token).
 * El navegador envía la cookie automáticamente.
 */
export const logoutRequest = async (): Promise<void> => {
    await api.post('/auth/logout');
};
