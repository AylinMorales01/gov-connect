import { createContext } from 'react';

// ── Tipos ──────────────────────────────────────────────

export interface AuthState {
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
    login: (username: string, password: string) => Promise<string>;
    logout: () => Promise<void>;
}

// ── Context ────────────────────────────────────────────

export const AuthContext = createContext<AuthContextType | undefined>(undefined);
