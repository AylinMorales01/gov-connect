import { Component, type ErrorInfo, type ReactNode } from 'react';
import { Alert, Box, Button, Typography } from '@mui/material';

interface ErrorBoundaryProps {
    children: ReactNode;
}

interface ErrorBoundaryState {
    hasError: boolean;
    error: Error | null;
}

export default class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
    constructor(props: ErrorBoundaryProps) {
        super(props);
        this.state = { hasError: false, error: null };
    }

    static getDerivedStateFromError(error: Error): ErrorBoundaryState {
        return { hasError: true, error };
    }

    componentDidCatch(error: Error, info: ErrorInfo): void {
        console.error('ErrorBoundary capturó:', error, info.componentStack);
    }

    render(): ReactNode {
        if (this.state.hasError) {
            return (
                <Box
                    sx={{
                        display: 'flex',
                        flexDirection: 'column',
                        alignItems: 'center',
                        justifyContent: 'center',
                        minHeight: 400,
                        p: 4,
                    }}
                >
                    <Alert severity="error" sx={{ mb: 2, maxWidth: 500 }}>
                        <Typography variant="h6" sx={{ mb: 1 }}>
                            Algo salió mal
                        </Typography>
                        <Typography variant="body2" color="text.secondary">
                            {this.state.error?.message ?? 'Error inesperado al renderizar la página.'}
                        </Typography>
                    </Alert>
                    <Button
                        variant="outlined"
                        onClick={() => {
                            this.setState({ hasError: false, error: null });
                            window.location.reload();
                        }}
                    >
                        Reintentar
                    </Button>
                </Box>
            );
        }

        return this.props.children;
    }
}
