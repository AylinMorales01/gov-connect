import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import { useState } from 'react';
import {
    AppBar,
    Box,
    CssBaseline,
    Drawer,
    IconButton,
    Toolbar,
    Typography,
    List,
    ListItem,
    ListItemButton,
    ListItemIcon,
    ListItemText,
    Tooltip,
    Alert,
    Snackbar,
    Button,
    Chip,
} from '@mui/material';

import DashboardIcon from '@mui/icons-material/Dashboard';
import AnalyticsIcon from '@mui/icons-material/Analytics';
import DescriptionIcon from '@mui/icons-material/Description';
import AutorenewIcon from '@mui/icons-material/Autorenew';
import LogoutIcon from '@mui/icons-material/Logout';
import TimerOutlinedIcon from '@mui/icons-material/TimerOutlined';
import AdminPanelSettingsOutlinedIcon from '@mui/icons-material/AdminPanelSettingsOutlined';
import PersonOutlinedIcon from '@mui/icons-material/PersonOutlined';

import { useAuth } from '../hooks/useAuth';

const drawerWidth = 240;

interface NavItem {
    label: string;
    path: string;
    icon: React.ReactElement;
    /** Si se especifica, solo los usuarios con este rol ven el ítem. */
    requiredRole?: string;
}

const allNavItems: NavItem[] = [
    { label: 'Dashboard', path: '/', icon: <DashboardIcon />, requiredRole: 'ADMIN' },
    { label: 'Analytics', path: '/analytics', icon: <AnalyticsIcon /> },
    { label: 'Contratos', path: '/contracts', icon: <DescriptionIcon /> },
    { label: 'Automatización', path: '/automation', icon: <AutorenewIcon /> },
];

/** Umbral de minutos para mostrar la advertencia de expiración. */
const SESSION_WARNING_MINUTES = 2;

export default function MainLayout() {
    const navigate = useNavigate();
    const location = useLocation();
    const { username, role, isAdmin, sessionMinutesLeft, logout } = useAuth();

    const [loggingOut, setLoggingOut] = useState(false);
    const [showSessionWarning, setShowSessionWarning] = useState(false);

    // ── Filtrar navegación según rol ──
    const visibleNavItems = allNavItems.filter(
        (item) => !item.requiredRole || role === item.requiredRole
    );

    const handleLogout = async () => {
        setLoggingOut(true);
        try {
            await logout();
        } finally {
            setLoggingOut(false);
        }
        navigate('/login', { replace: true });
    };

    // ── Advertencia de expiración de sesión ──
    const handleSessionWarningClose = () => setShowSessionWarning(false);

    const shouldWarn =
        sessionMinutesLeft !== null &&
        sessionMinutesLeft <= SESSION_WARNING_MINUTES &&
        sessionMinutesLeft > 0;

    if (shouldWarn && !showSessionWarning) {
        queueMicrotask(() => setShowSessionWarning(true));
    }

    return (
        <Box sx={{ display: 'flex' }}>
            <CssBaseline />

            {/* ── AppBar ── */}
            <AppBar position="fixed" sx={{ zIndex: (theme) => theme.zIndex.drawer + 1 }}>
                <Toolbar>
                    <Typography variant="h6" noWrap component="div" sx={{ flexGrow: 1 }}>
                        Gov Connect
                    </Typography>

                    {/* Badge de rol */}
                    {role && (
                        <Chip
                            icon={
                                isAdmin ? (
                                    <AdminPanelSettingsOutlinedIcon />
                                ) : (
                                    <PersonOutlinedIcon />
                                )
                            }
                            label={isAdmin ? 'ADMIN' : 'USER'}
                            size="small"
                            color={isAdmin ? 'warning' : 'default'}
                            variant="outlined"
                            sx={{
                                mr: 1,
                                fontWeight: 'bold',
                                '& .MuiChip-icon': { color: 'inherit' },
                            }}
                        />
                    )}

                    <Typography variant="body2" noWrap sx={{ mr: 1 }}>
                        {username}
                    </Typography>

                    <Tooltip title="Cerrar sesión">
                        <span>
                            <IconButton
                                color="inherit"
                                onClick={handleLogout}
                                size="small"
                                disabled={loggingOut}
                            >
                                <LogoutIcon />
                            </IconButton>
                        </span>
                    </Tooltip>
                </Toolbar>
            </AppBar>

            {/* ── Drawer ── */}
            <Drawer
                variant="permanent"
                sx={{
                    width: drawerWidth,
                    flexShrink: 0,
                    [`& .MuiDrawer-paper`]: {
                        width: drawerWidth,
                        boxSizing: 'border-box',
                    },
                }}
            >
                <Toolbar />
                <Box sx={{ overflow: 'auto' }}>
                    <List>
                        {visibleNavItems.map((item) => {
                            const isActive = location.pathname === item.path;
                            return (
                                <ListItem key={item.path} disablePadding>
                                    <ListItemButton
                                        selected={isActive}
                                        onClick={() => navigate(item.path)}
                                    >
                                        <ListItemIcon>{item.icon}</ListItemIcon>
                                        <ListItemText primary={item.label} />
                                    </ListItemButton>
                                </ListItem>
                            );
                        })}
                    </List>
                </Box>
            </Drawer>

            {/* ── Contenido principal ── */}
            <Box component="main" sx={{ flexGrow: 1, p: 3 }}>
                <Toolbar />
                <Outlet />
            </Box>

            {/* ── Snackbar de advertencia de expiración ── */}
            <Snackbar
                open={showSessionWarning}
                anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
                sx={{ mb: 2 }}
            >
                <Alert
                    severity="warning"
                    variant="filled"
                    icon={<TimerOutlinedIcon />}
                    onClose={handleSessionWarningClose}
                    action={
                        <Button
                            color="inherit"
                            size="small"
                            onClick={handleSessionWarningClose}
                        >
                            Entendido
                        </Button>
                    }
                    sx={{ borderRadius: 2, boxShadow: 3 }}
                >
                    Tu sesión expirará en {sessionMinutesLeft} minuto
                    {sessionMinutesLeft !== 1 ? 's' : ''}. Guarda tu trabajo.
                </Alert>
            </Snackbar>
        </Box>
    );
}
