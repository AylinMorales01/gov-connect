import { Navigate, Outlet } from 'react-router-dom';
import { Box, CircularProgress } from '@mui/material';
import { useAuth } from '../../hooks/useAuth';
import ForbiddenPage from '../../pages/ForbiddenPage';

interface ProtectedRouteProps {
    /**
     * Roles permitidos para acceder a esta ruta.
     * Si no se especifica, cualquier usuario autenticado puede acceder.
     * Ejemplo: {@code allowedRoles={['ADMIN']}}
     */
    allowedRoles?: string[];
}

/**
 * Componente de ruta protegida.
 * <ul>
 *   <li>Si el usuario no está autenticado → redirige a /login.</li>
 *   <li>Si está autenticado pero su rol no está en {@code allowedRoles} → muestra {@link ForbiddenPage}.</li>
 *   <li>Si está autenticado y tiene el rol correcto → renderiza {@code <Outlet />}.</li>
 * </ul>
 * <p>
 * <b>El backend es la fuente de verdad para autorización.</b>
 * Este componente es solo una capa de UX para evitar navegación innecesaria.
 * </p>
 */
export default function ProtectedRoute({ allowedRoles }: ProtectedRouteProps) {
    const { isAuthenticated, initializing, hasRole } = useAuth();

    // Mientras se restaura la sesión (vía /auth/me), no redirigir para
    // evitar un flash de /login en usuarios ya autenticados.
    if (initializing) {
        return (
            <Box
                sx={{
                    display: 'flex',
                    justifyContent: 'center',
                    alignItems: 'center',
                    minHeight: '100vh',
                }}
            >
                <CircularProgress />
            </Box>
        );
    }

    if (!isAuthenticated) {
        return <Navigate to="/login" replace />;
    }

    if (allowedRoles && allowedRoles.length > 0) {
        const hasAllowedRole = allowedRoles.some((role) => hasRole(role));
        if (!hasAllowedRole) {
            return <ForbiddenPage />;
        }
    }

    return <Outlet />;
}
