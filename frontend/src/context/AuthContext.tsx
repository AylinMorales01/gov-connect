import {
    createContext,
    useState,
    useCallback,
    useEffect,
    type ReactNode,
} from 'react';
import { loginRequest, logoutRequest } from '../api/authApi';
import { setOnTokenRefreshed } from '../api/axios';

// ── Tipos ──────────────────────────────────────────────

interface AuthState {
    token: string | null;
    username: string | null;
    role: string | null;
    /** Timestamp (ms) en que expira el access token, o null si no es JWT decodificable. */
    tokenExpiresAt: number | null;
}

export interface AuthContextType extends AuthState {
    isAuthenticated: boolean;
    /** Verdadero si el usuario tiene rol ADMIN. */
    isAdmin: boolean;
    /** Verifica si el usuario tiene un rol específico. */
    hasRole: (role: string) => boolean;
    /** Minutos restantes antes de que expire la sesión (null si no hay token). */
    sessionMinutesLeft: number | null;
    login: (username: string, password: string) => Promise<void>;
    logout: () => Promise<void>;
}

// ── Context ────────────────────────────────────────────

export const AuthContext = createContext<AuthContextType | undefined>(undefined);

// ── Storage keys ───────────────────────────────────────

const STORAGE_KEY_TOKEN = 'govconnect_token';
const STORAGE_KEY_USERNAME = 'govconnect_username';
const STORAGE_KEY_ROLE = 'govconnect_role';

// ── JWT decode (sin librería externa) ──────────────────

interface JwtPayload {
    sub: string;
    role: string;
    exp: number;
    iat: number;
}

function decodeJwt(token: string): JwtPayload | null {
    try {
        const payload = token.split('.')[1];
        const decoded = JSON.parse(atob(payload));
        return decoded as JwtPayload;
    } catch {
        return null;
    }
}

function getExpiresAt(token: string | null): number | null {
    if (!token) return null;
    const decoded = decodeJwt(token);
    return decoded?.exp ? decoded.exp * 1000 : null;
}

// ── Provider ───────────────────────────────────────────

export function AuthProvider({ children }: { children: ReactNode }) {
    const [auth, setAuth] = useState<AuthState>(() => {
        const token = localStorage.getItem(STORAGE_KEY_TOKEN);
        return {
            token,
            username: token ? localStorage.getItem(STORAGE_KEY_USERNAME) : null,
            role: token ? localStorage.getItem(STORAGE_KEY_ROLE) : null,
            tokenExpiresAt: getExpiresAt(token),
        };
    });

    const isAuthenticated = !!auth.token;
    const isAdmin = auth.role === 'ADMIN';

    /** Verifica si el usuario autenticado posee un rol específico. */
    const hasRole = useCallback(
        (role: string): boolean => {
            return auth.role === role;
        },
        [auth.role]
    );

    // Tiempo restante de sesión (para advertencia de expiración)
    const sessionMinutesLeft =
        auth.tokenExpiresAt != null
            ? Math.max(0, Math.round((auth.tokenExpiresAt - Date.now()) / 60000))
            : null;

    // ── Registrar callback para que el interceptor de axios ──
    // ── actualice el estado después de un refresh silencioso   ──
    useEffect(() => {
        setOnTokenRefreshed((token: string, role: string) => {
            const decoded = decodeJwt(token);
            setAuth({
                token,
                username: decoded?.sub || null,
                role,
                tokenExpiresAt: decoded?.exp ? decoded.exp * 1000 : null,
            });
        });
        return () => setOnTokenRefreshed(null);
    }, []);

    // ── Login ──────────────────────────────────────────

    const login = useCallback(async (username: string, password: string) => {
        const response = await loginRequest(username, password);
        const { token, role } = response;
        const decoded = decodeJwt(token);

        localStorage.setItem(STORAGE_KEY_TOKEN, token);
        localStorage.setItem(STORAGE_KEY_USERNAME, username);
        localStorage.setItem(STORAGE_KEY_ROLE, role);

        setAuth({
            token,
            username,
            role,
            tokenExpiresAt: decoded?.exp ? decoded.exp * 1000 : null,
        });
    }, []);

    // ── Logout ──────────────────────────────────────────

    const logout = useCallback(async () => {
        try {
            // Invalidar refresh token en el servidor
            await logoutRequest();
        } catch {
            // Si el servidor no responde, igual limpiamos localmente
        }

        localStorage.removeItem(STORAGE_KEY_TOKEN);
        localStorage.removeItem(STORAGE_KEY_USERNAME);
        localStorage.removeItem(STORAGE_KEY_ROLE);

        setAuth({
            token: null,
            username: null,
            role: null,
            tokenExpiresAt: null,
        });
    }, []);

    return (
        <AuthContext.Provider
            value={{ ...auth, isAuthenticated, isAdmin, hasRole, sessionMinutesLeft, login, logout }}
        >
            {children}
        </AuthContext.Provider>
    );
}
