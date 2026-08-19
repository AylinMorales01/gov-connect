import { useNavigate } from 'react-router-dom';
import { Box, Typography, Button } from '@mui/material';
import BlockOutlinedIcon from '@mui/icons-material/BlockOutlined';
import ArrowBackOutlinedIcon from '@mui/icons-material/ArrowBackOutlined';
import { useAuth } from '../hooks/useAuth';

/**
 * Página mostrada cuando un usuario autenticado intenta acceder
 * a una ruta para la que no tiene el rol requerido (HTTP 403).
 * <p>
 * No es una barrera de seguridad — el backend es la fuente de verdad.
 * Esta página solo mejora la UX evitando pantallas rotas.
 * </p>
 */
export default function ForbiddenPage() {
    const navigate = useNavigate();
    const { role } = useAuth();

    return (
        <Box
            sx={{
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                justifyContent: 'center',
                minHeight: '60vh',
                textAlign: 'center',
                gap: 2,
            }}
        >
            <BlockOutlinedIcon sx={{ fontSize: 80, color: 'error.main', mb: 1 }} />

            <Typography variant="h4" sx={{ fontWeight: 'bold' }}>
                Acceso denegado
            </Typography>

            <Typography variant="body1" color="text.secondary" sx={{ maxWidth: 480 }}>
                No tienes los permisos necesarios para acceder a esta sección.
                {role && (
                    <>
                        <br />
                        Tu rol actual es <strong>{role}</strong>.
                    </>
                )}
            </Typography>

            <Button
                variant="contained"
                size="large"
                startIcon={<ArrowBackOutlinedIcon />}
                onClick={() => navigate(role === 'ADMIN' ? '/' : '/analytics', { replace: true })}
                sx={{ mt: 2, textTransform: 'none' }}
            >
                Volver a una sección autorizada
            </Button>
        </Box>
    );
}
