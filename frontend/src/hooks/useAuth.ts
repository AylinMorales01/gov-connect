import { useContext } from 'react';
import { AuthContext, type AuthContextType } from '../context/authContext';

/**
 * Hook de conveniencia para acceder al contexto de autenticación.
 * Debe usarse dentro de {@link AuthProvider}.
 *
 * El método {@link AuthContextType.logout} es asíncrono porque invalida
 * el refresh token en el servidor antes de limpiar el estado local.
 */
export function useAuth(): AuthContextType {
    const context = useContext(AuthContext);
    if (!context) {
        throw new Error('useAuth debe usarse dentro de un AuthProvider');
    }
    return context;
}
