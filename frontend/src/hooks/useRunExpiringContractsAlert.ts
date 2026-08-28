import { useMutation, useQueryClient } from '@tanstack/react-query';
import { runExpiringContractsAlert } from '../api/automationApi';

/**
 * Dispara manualmente la alerta de contratos por vencer.
 *
 * Cada ejecución queda registrada por el backend en {@code automation_logs},
 * por lo que al terminar se invalida la consulta del historial para que la
 * nueva fila aparezca sin que el usuario tenga que refrescar.
 */
export function useRunExpiringContractsAlert() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: runExpiringContractsAlert,
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['automation-logs'] });
        },
    });
}
