import React from 'react';
import { Card, CardContent, Typography, Box, Avatar } from '@mui/material';

const colorMap = {
    primary: '#1976D2',
    secondary: '#009688',
    success: '#2E7D32',
    warning: '#ED6C02',
    error: '#D32F2F',
};

interface DashboardCardProps {
    title: string;
    value: string | number;
    icon: React.ReactElement; // Usamos ReactElement para poder pasarle estilos o escalarlo
    color?: keyof typeof colorMap;
    subtitle?: string;
}

export default function DashboardCard({
                                          title,
                                          value,
                                          icon,
                                          color = 'primary',
                                          subtitle,
                                      }: DashboardCardProps) {
    const selectedColor = colorMap[color];

    return (
        <Card
            sx={{
                height: '100%',
                minHeight: 135, // Ajuste 3: Tarjetas más altas (entre 130px - 140px)
                boxShadow: 2,
                borderRadius: 2,
                display: 'flex',
                flexDirection: 'column',
                justifyContent: 'center', // Centra el contenido verticalmente
                // Ajuste 5: Hover moderno con elevación sutil
                transition: 'transform 0.2s ease-in-out, box-shadow 0.2s ease-in-out',
                '&:hover': {
                    transform: 'translateY(-3px)',
                    boxShadow: 4,
                },
            }}
        >
            <CardContent sx={{ p: 2.5 }}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <Box>
                        <Typography variant="subtitle2" color="text.secondary" gutterBottom>
                            {title}
                        </Typography>
                        <Typography variant="h5" sx={{ fontWeight: 'bold', mb: subtitle ? 0.5 : 0 }}>
                            {value}
                        </Typography>
                        {subtitle && (
                            <Typography variant="caption" color="text.secondary">
                                {subtitle}
                            </Typography>
                        )}
                    </Box>

                    <Avatar
                        sx={{
                            bgcolor: `${selectedColor}15`,
                            color: selectedColor,
                            width: 60,
                            height: 60,
                            // Ajuste 4: Hacemos que el ícono dentro del Avatar sea un poco más grande
                            '& > svg': {
                                fontSize: '1.85rem',
                            },
                        }}
                    >
                        {icon}
                    </Avatar>
                </Box>
            </CardContent>
        </Card>
    );
}