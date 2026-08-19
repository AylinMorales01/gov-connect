import { api } from './axios';

/**
 * Respuesta del endpoint POST /api/v1/auth/login y /auth/refresh.
 * Coincide con AuthResponse.java del backend: ya no incluye el token,
 * que viaja en una cookie HttpOnly.
 */
export interface AuthResponse {
    role: string;
    expiresIn: number;
}

/**
 * Respuesta del endpoint GET /api/v1/auth/me.
 * Permite restaurar el estado de sesión tras recargar la página.
 */
export interface MeResponse {
    username: string;
    role: string;
    expiresIn: number;
}

/**
 * Envía credenciales al backend. El access token se recibe como cookie
 * HttpOnly, por lo que no se devuelve en el cuerpo.
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
 */
export const refreshTokenRequest = async (): Promise<AuthResponse> => {
    const response = await api.post<{ data: AuthResponse }>('/auth/refresh');
    return response.data.data;
};

/**
 * Cierra la sesión en el servidor (invalida el refresh token y limpia cookies).
 */
export const logoutRequest = async (): Promise<void> => {
    await api.post('/auth/logout');
};

/**
 * Obtiene el usuario autenticado actual desde la cookie HttpOnly.
 */
export const getMe = async (): Promise<MeResponse> => {
    const response = await api.get<{ data: MeResponse }>('/auth/me');
    return response.data.data;
};
