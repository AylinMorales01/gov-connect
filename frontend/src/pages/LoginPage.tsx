import { useState, useEffect, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import {
    Box,
    Card,
    CardContent,
    Typography,
    TextField,
    Button,
    Alert,
    CircularProgress,
} from '@mui/material';
import LockOutlinedIcon from '@mui/icons-material/LockOutlined';
import axios from 'axios';
import { useAuth } from '../hooks/useAuth';

export default function LoginPage() {
    const { login, isAuthenticated } = useAuth();
    const navigate = useNavigate();

    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);

    // Si ya está autenticado, redirigir al dashboard
    useEffect(() => {
        if (isAuthenticated) {
            navigate('/', { replace: true });
        }
    }, [isAuthenticated, navigate]);

    const handleSubmit = async (e: FormEvent) => {
        e.preventDefault();

        if (!username.trim() || !password.trim()) {
            setError('Ingrese usuario y contraseña.');
            return;
        }

        setError('');
        setLoading(true);

        try {
            await login(username.trim(), password);
            navigate('/', { replace: true });
        } catch (err: unknown) {
            if (axios.isAxiosError(err)) {
                if (err.response?.status === 401) {
                    setError('Credenciales inválidas. Verifique usuario y contraseña.');
                } else if (err.response?.status === 429) {
                    setError('Demasiados intentos. Intente nuevamente en un minuto.');
                } else if (!err.response) {
                    setError('No se pudo conectar con el servidor. Verifica que el backend esté corriendo.');
                } else {
                    setError(`Error del servidor (${err.response.status}). Intente nuevamente más tarde.`);
                }
            } else {
                setError('Error inesperado. Intente nuevamente.');
            }
        } finally {
            setLoading(false);
        }
    };

    return (
        <Box
            sx={{
                display: 'flex',
                justifyContent: 'center',
                alignItems: 'center',
                minHeight: '100vh',
                bgcolor: 'background.default',
                p: 2,
            }}
        >
            <Card sx={{ maxWidth: 420, width: '100%', boxShadow: 3, borderRadius: 2 }}>
                <CardContent sx={{ p: { xs: 3, sm: 4 } }}>
                    {/* Encabezado */}
                    <Box sx={{ textAlign: 'center', mb: 3 }}>
                        <LockOutlinedIcon
                            sx={{ fontSize: 40, color: 'primary.main', mb: 1 }}
                        />
                        <Typography variant="h5" sx={{ fontWeight: 'bold' }}>
                            Gov Connect
                        </Typography>
                        <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
                            Inicie sesión para continuar
                        </Typography>
                    </Box>

                    {/* Error */}
                    {error && (
                        <Alert severity="error" sx={{ mb: 2 }}>
                            {error}
                        </Alert>
                    )}

                    {/* Formulario */}
                    <Box component="form" onSubmit={handleSubmit} noValidate>
                        <TextField
                            label="Usuario"
                            fullWidth
                            autoFocus
                            margin="normal"
                            value={username}
                            onChange={(e) => setUsername(e.target.value)}
                            disabled={loading}
                            autoComplete="username"
                        />

                        <TextField
                            label="Contraseña"
                            type="password"
                            fullWidth
                            margin="normal"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            disabled={loading}
                            autoComplete="current-password"
                        />

                        <Button
                            type="submit"
                            variant="contained"
                            fullWidth
                            size="large"
                            disabled={loading}
                            sx={{ mt: 3, textTransform: 'none', fontWeight: 'bold' }}
                        >
                            {loading ? (
                                <CircularProgress size={24} color="inherit" />
                            ) : (
                                'Iniciar sesión'
                            )}
                        </Button>
                    </Box>
                </CardContent>
            </Card>
        </Box>
    );
}
