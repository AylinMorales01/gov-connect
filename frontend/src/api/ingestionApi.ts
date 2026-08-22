import { api } from './axios';
import type { EtlTask, IngestionTask } from '../types/ingestion';

/** Construye el cuerpo multipart para subir un archivo CSV. */
function buildFormData(file: File): FormData {
    const formData = new FormData();
    formData.append('file', file);
    return formData;
}

/**
 * Encabezado multipart explícito para las subidas de archivo. La instancia
 * axios global fija `Content-Type: application/json`; sin este override el
 * `transformRequest` serializa el FormData como JSON y el backend responde
 * `MultipartException: Current request is not a multipart request`.
 *
 * El timeout se amplía sobre el global de 10 s: aunque la importación ya es
 * asíncrona y responde de inmediato, subir un export de SECOP II de decenas de
 * MB tarda más que eso y axios abortaba la conexión a mitad del envío.
 */
const UPLOAD_CONFIG = {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 300_000,
};

export const uploadContracts = async (file: File): Promise<IngestionTask> => {
    const response = await api.post<{ data: IngestionTask }>(
        '/ingestion/contracts',
        buildFormData(file),
        UPLOAD_CONFIG,
    );
    return response.data.data;
};

export const uploadBudgets = async (file: File): Promise<IngestionTask> => {
    const response = await api.post<{ data: IngestionTask }>(
        '/ingestion/budgets',
        buildFormData(file),
        UPLOAD_CONFIG,
    );
    return response.data.data;
};

export const uploadCollections = async (file: File): Promise<IngestionTask> => {
    const response = await api.post<{ data: IngestionTask }>(
        '/ingestion/collections',
        buildFormData(file),
        UPLOAD_CONFIG,
    );
    return response.data.data;
};

/** Consulta el avance de una importación encolada. */
export const getIngestionStatus = async (taskId: string): Promise<IngestionTask> => {
    const response = await api.get<{ data: IngestionTask }>(`/ingestion/status/${taskId}`);
    return response.data.data;
};

export const getEtlStatus = async (taskId: string): Promise<EtlTask> => {
    const response = await api.get<{ data: EtlTask }>(`/analytics/etl/status/${taskId}`);
    return response.data.data;
};
