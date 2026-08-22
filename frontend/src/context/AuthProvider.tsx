import {
    useState,
    useCallback,
    useEffect,
    type ReactNode,
} from 'react';
import { loginRequest, logoutRequest, getMe } from '../api/authApi';
import { setOnTokenRefreshed, setOnSessionExpired } from '../api/axios';
import { AuthContext, type AuthState } from './authContext';

// ── Provider ───────────────────────────────────────────

export function AuthProvider({ children }: { children: ReactNode }) {
    const [auth, setAuth] = useState<AuthState>({
        username: null,
        role: null,
        tokenExpiresAt: null,
    });
    const [initializing, setInitializing] = useState(true);
    const [now, setNow] = useState(0);

    const isAuthenticated = auth.username !== null;
    const isAdmin = auth.role === 'ADMIN';

    /** Verifica si el usuario autenticado posee un rol específico. */
    const hasRole = useCallback(
        (role: string): boolean => {
            return auth.role === role;
        },
        [auth.role]
    );

    // Reloj para el tiempo restante de sesión. Se mantiene en estado para no
    // llamar a Date.now() (función impura) durante el render; se refresca cada
    // 30 s mientras hay sesión para que la advertencia de expiración avance sola.
    useEffect(() => {
        if (auth.tokenExpiresAt == null) {
            return;
        }
        const id = setInterval(() => setNow(Date.now()), 30_000);
        return () => clearInterval(id);
    }, [auth.tokenExpiresAt]);

    // Tiempo restante de sesión (para advertencia de expiración)
    const sessionMinutesLeft =
        auth.tokenExpiresAt != null
            ? Math.max(0, Math.round((auth.tokenExpiresAt - now) / 60000))
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

    const login = useCallback(async (username: string, password: string): Promise<string> => {
        const { role, expiresIn } = await loginRequest(username, password);

        setAuth({
            username,
            role,
            tokenExpiresAt: Date.now() + expiresIn * 1000,
        });

        return role;
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
