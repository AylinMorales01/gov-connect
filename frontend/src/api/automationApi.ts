import { api } from './axios';
import type { ExpiringContractsAlertResult } from '../types/dashboard';

/**
 * El envío por SMTP se hace de forma síncrona dentro de la petición: el
 * handshake con el servidor de correo más un envío por destinatario supera con
 * facilidad el timeout global de 10 s de la instancia axios. Sin este override
 * la petición se abortaba en el cliente aunque el backend terminara enviando
 * los correos, dejando a la UI reportando un error inexistente.
 */
const ALERT_CONFIG = {
    timeout: 60_000,
};

/**
 * Dispara manualmente la alerta de contratos por vencer (requiere rol ADMIN).
 *
 * No lanza excepción cuando la alerta no envía correo: el backend degrada de
 * forma controlada (sin destinatarios, sin SMTP o sin contratos por vencer) y
 * devuelve el motivo en {@link ExpiringContractsAlertResult.message}.
 */
export const runExpiringContractsAlert = async (): Promise<ExpiringContractsAlertResult> => {
    const response = await api.post<{ data: ExpiringContractsAlertResult }>(
        '/automation/expiring-contracts/alert',
        null,
        ALERT_CONFIG,
    );
    return response.data.data;
};
