import {
    createContext,
    useState,
    useCallback,
    useEffect,
    type ReactNode,
} from 'react';
import { loginRequest, logoutRequest, getMe } from '../api/authApi';
import { setOnTokenRefreshed, setOnSessionExpired } from '../api/axios';

// ── Tipos ──────────────────────────────────────────────

interface AuthState {
    username: string | null;
    role: string | null;
    /** Timestamp (ms) en que expira el access token, o null si se desconoce. */
    tokenExpiresAt: number | null;
}

export interface AuthContextType extends AuthState {
    isAuthenticated: boolean;
    /** Verdadero mientras se restaura la sesión al iniciar la app. */
    initializing: boolean;
    /** Verdadero si el usuario tiene rol ADMIN. */
    isAdmin: boolean;
    /** Verifica si el usuario tiene un rol específico. */
    hasRole: (role: string) => boolean;
    /** Minutos restantes antes de que expire la sesión (null si no hay sesión). */
    sessionMinutesLeft: number | null;
    login: (username: string, password: string) => Promise<void>;
    logout: () => Promise<void>;
}

// ── Context ────────────────────────────────────────────

export const AuthContext = createContext<AuthContextType | undefined>(undefined);

// ── Provider ───────────────────────────────────────────

export function AuthProvider({ children }: { children: ReactNode }) {
    const [auth, setAuth] = useState<AuthState>({
        username: null,
        role: null,
        tokenExpiresAt: null,
    });
    const [initializing, setInitializing] = useState(true);

    const isAuthenticated = auth.username !== null;
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

    // ── Registrar callbacks para el interceptor de axios ──
    useEffect(() => {
        setOnTokenRefreshed((role: string, expiresIn: number) => {
            setAuth((prev) => ({
                ...prev,
                role,
                tokenExpiresAt: Date.now() + expiresIn * 1000,
            }));
        });

        setOnSessionExpired(() => {
            setAuth({ username: null, role: null, tokenExpiresAt: null });
        });

        return () => {
            setOnTokenRefreshed(null);
            setOnSessionExpired(null);
        };
    }, []);

    // ── Restaurar sesión al iniciar (vía /auth/me con la cookie) ──
    useEffect(() => {
        let cancelled = false;

        (async () => {
            try {
                const me = await getMe();
                if (!cancelled) {
                    setAuth({
                        username: me.username,
                        role: me.role,
                        tokenExpiresAt: Date.now() + me.expiresIn * 1000,
                    });
                }
            } catch {
                // Sin sesión válida (o backend caído): quedamos desautenticados.
                if (!cancelled) {
                    setAuth({ username: null, role: null, tokenExpiresAt: null });
                }
            } finally {
                if (!cancelled) {
                    setInitializing(false);
                }
            }
        })();

        return () => {
            cancelled = true;
        };
    }, []);

    // ── Login ──────────────────────────────────────────

    const login = useCallback(async (username: string, password: string) => {
        const { role, expiresIn } = await loginRequest(username, password);

        setAuth({
            username,
            role,
            tokenExpiresAt: Date.now() + expiresIn * 1000,
        });
    }, []);

    // ── Logout ──────────────────────────────────────────

    const logout = useCallback(async () => {
        try {
            // Invalida el refresh token en el servidor y limpia las cookies
            await logoutRequest();
        } catch {
            // Si el servidor no responde, igual limpiamos localmente
        }

        setAuth({ username: null, role: null, tokenExpiresAt: null });
    }, []);

    return (
        <AuthContext.Provider
            value={{
                ...auth,
                isAuthenticated,
                initializing,
                isAdmin,
                hasRole,
                sessionMinutesLeft,
                login,
                logout,
            }}
        >
            {children}
        </AuthContext.Provider>
    );
}
